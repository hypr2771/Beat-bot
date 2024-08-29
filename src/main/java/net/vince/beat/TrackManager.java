package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.vince.beat.mixin.AudioPlayerSendHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Good test string: /play track:https://www.youtube.com/watch?v=FNMsx6SmtyA
 *
 * ex.:
 *
 * 0. Metronomy - Reservoir 1. เมืองแมน - สัตว์สังคม 2. Pyre Original Soundtrack - Full Album
 *
 * Upon start, current track is 0.
 *
 * Loop: Clicking on loop makes the current track stack anew in the list. This does mean the index should not change, while track at index should be cloned.
 */
public class TrackManager extends AudioEventAdapter {

  private final AudioPlayer player;
  private final Guild       guild;

  private List<AudioTrack> playlist;

  private Optional<Message> currentMessage;

  private int currentTrack = 0;

  private boolean loop     = false;
  private boolean pause    = false;
  private boolean stopping = false;

  public TrackManager(AudioPlayerManager audioManager, Guild guild) {

    var newPlayer = audioManager.createPlayer();
    newPlayer.addListener(this);

    guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(newPlayer));

    this.player = newPlayer;
    this.guild  = guild;

    initialState();
  }

  private void initialState() {
    this.currentMessage = Optional.empty();
    this.playlist       = new ArrayList<>();
    this.stopping       = false;
    this.pause          = false;
    this.loop           = false;
    this.currentTrack   = 0;
  }

  public void queue(AudioTrack track, AudioChannelUnion channel, InteractionHook replyHook, MessageChannelUnion eventChannel) {
    queue(track, channel, replyHook, eventChannel, true);
  }

  public void queue(AudioTrack track, AudioChannelUnion channel, InteractionHook replyHook, MessageChannelUnion eventChannel, boolean editMessage) {

    guild.getAudioManager().openAudioConnection(channel);

    playlist.add(track);

    if (player.getPlayingTrack() == null) {
      this.currentTrack = 0;
      player.playTrack(playlist.get(currentTrack));
    }

    if (editMessage) {
      replyHook.deleteOriginal()
               .flatMap(ignored -> currentMessage.map(message -> message.editMessageComponents(getItemComponents())
                                                                        .flatMap(edited -> edited.editMessageEmbeds(getMessageEmbed())))
                                                 .orElseGet(() -> eventChannel.sendMessageComponents(getItemComponents())
                                                                              .flatMap(message -> message.editMessageEmbeds(getMessageEmbed()))))
               .queue(message -> this.currentMessage = Optional.of(message));
    }
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

    if (this.pause) {
      togglePause();
    }

    if (this.loop &&
        !this.stopping &&
        currentTrack >= 0) {
      playlist.add(currentTrack, this.playlist.get(currentTrack).makeClone());
      playlist.remove(currentTrack + 1);
    } else if (this.currentTrack >= this.playlist.size() - 1) {
      playlist.clear();
    } else {
      this.currentTrack++;
    }

    if (!playlist.isEmpty()) {
      player.playTrack(playlist.get(currentTrack));

      currentMessage.map(message -> message.editMessageComponents(getItemComponents())
                                           .flatMap(edited -> edited.editMessageEmbeds(getMessageEmbed())))
                    .ifPresent(messageRestAction -> messageRestAction.queue(message -> this.currentMessage = Optional.of(message)));
    } else {
      currentMessage.map(Message::delete)
                    .ifPresent(RestAction::queue);
      currentMessage = Optional.empty();
      guild.getAudioManager().closeAudioConnection();
    }
  }

  @NotNull
  private ActionRow getItemComponents() {
    return ActionRow.of(Button.secondary("previous", Emoji.fromUnicode("⏮")),
                        this.pause ?
                        Button.success("pause",
                                       Emoji.fromUnicode("▶")) :
                        Button.secondary("pause",
                                         Emoji.fromUnicode("⏸")),
                        Button.danger("stop", Emoji.fromUnicode("⏹")),
                        Button.secondary("next", Emoji.fromUnicode("⏭")),
                        this.loop ?
                        Button.success("loop",
                                       Emoji.fromUnicode("\uD83D\uDD01")) :
                        Button.secondary("loop",
                                         Emoji.fromUnicode("\uD83D\uDD01")));
  }

  @NotNull
  private MessageEmbed getMessageEmbed() {
    return new MessageEmbed(null,
                            "Playlist",
                            getPlaylistString(),
                            EmbedType.RICH,
                            null,
                            0x5dd200,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);
  }

  private String getPlaylistString() {

    var string = new StringBuilder();

    for (int i = 0; i < this.playlist.size(); i++) {
      string.append("%s%s %s. %s - %s - %s%s%n".formatted(styleForIndex(i),
                                                          i == this.currentTrack ? "▸" : "·",
                                                          i + 1,
                                                          this.playlist.get(i).getInfo().title,
                                                          this.playlist.get(i).getInfo().author,
                                                          humanReadableFormat(Duration.ofMillis(this.playlist.get(i).getInfo().length)),
                                                          styleForIndex(i)));
    }

    return string.toString();
  }

  public static String humanReadableFormat(Duration duration) {
    return duration.toString()
                   .substring(2)
                   .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                   .toLowerCase();
  }


  private String styleForIndex(int index) {
    if (this.currentTrack < index) {
      return "*";
    }
    if (this.currentTrack == index) {
      return "**";
    }
    return "~~";
  }

  public void next() {
    player.stopTrack();
  }

  public void previous() {

    if (this.currentTrack == 0) {

      playlist.add(this.currentTrack, playlist.get(this.currentTrack).makeClone());
      playlist.remove(this.currentTrack + 1);

      this.currentTrack--;

    } else {

      playlist.add(this.currentTrack - 1, playlist.get(this.currentTrack - 1).makeClone());
      playlist.remove(this.currentTrack);
      playlist.add(this.currentTrack, playlist.get(this.currentTrack).makeClone());
      playlist.remove(this.currentTrack + 1);

      // Sets current track to -2 so that ending and playing next track does not just replay the same song
      this.currentTrack--;

      if (!this.loop) {
        this.currentTrack--;
      }
    }

    player.stopTrack();
  }

  public void stop() {
    this.playlist = playlist.subList(this.currentTrack, this.currentTrack + 1);

    this.stopping = true;

    player.stopTrack();

    initialState();
  }

  public void toggleLoop() {
    this.loop = !this.loop;

    currentMessage.map(message -> message.editMessageComponents(getItemComponents())
                                         .flatMap(edited -> edited.editMessageEmbeds(getMessageEmbed())))
                  .ifPresent(messageRestAction -> messageRestAction.queue(message -> this.currentMessage = Optional.of(message)));
  }

  public void togglePause() {
    this.pause = !this.pause;

    player.setPaused(this.pause);

    currentMessage.map(message -> message.editMessageComponents(getItemComponents())
                                         .flatMap(edited -> edited.editMessageEmbeds(getMessageEmbed())))
                  .ifPresent(messageRestAction -> messageRestAction.queue(message -> this.currentMessage = Optional.of(message)));
  }

  public void remove(List<Integer> indices) {

    var filteredIndices = indices.stream()
                                 .filter(index -> index > 0)
                                 .filter(index -> index <= playlist.size())
                                 .sorted(Comparator.reverseOrder())
                                 .toList();

    var shouldNext = false;

    for (var index : filteredIndices) {

      if (index < currentTrack + 1) {
        playlist.remove(index - 1);
        currentTrack--;
      } else if (index > currentTrack + 1) {
        playlist.remove(index - 1);
      } else if (index == currentTrack + 1) {
        playlist.remove(currentTrack);
        currentTrack--;
        shouldNext = true;
      }
    }

    if (shouldNext) {
      next();

      // Calling display anew is not required since calling `next()` method already does a display through `stopTrack` which triggers `onTrackEnd`
    } else {
      currentMessage.map(message -> message.editMessageComponents(getItemComponents())
                                           .flatMap(edited -> edited.editMessageEmbeds(getMessageEmbed())))
                    .ifPresent(messageRestAction -> messageRestAction.queue(message -> this.currentMessage = Optional.of(message)));
    }

  }

  List<AudioTrack> getPlaylist() {
    return playlist;
  }
}
