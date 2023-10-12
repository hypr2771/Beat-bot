package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.time.Duration;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
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

public class TrackManager extends AudioEventAdapter {

  private final AudioPlayer       player;
  private final Guild             guild;
  private final Deque<AudioTrack> playlist;

  private Optional<Message> currentMessage;

  private boolean loop     = false;
  private boolean stopping = false;

  public TrackManager(AudioPlayerManager audioManager, Guild guild) {

    var newPlayer = audioManager.createPlayer();
    newPlayer.addListener(this);

    guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(newPlayer));

    this.player         = newPlayer;
    this.guild          = guild;
    this.playlist       = new ConcurrentLinkedDeque<>();
    this.currentMessage = Optional.empty();
  }

  public void queue(AudioTrack track, AudioChannelUnion channel, InteractionHook replyHook, MessageChannelUnion eventChannel) {

    guild.getAudioManager().openAudioConnection(channel);

    playlist.offer(track);

    if (player.getPlayingTrack() == null) {
      player.playTrack(playlist.element());
    }

    replyHook.deleteOriginal()
             .flatMap(ignored -> currentMessage.map(message -> message.editMessageComponents(getItemComponents())
                                                                      .flatMap(edited -> edited.editMessageEmbeds(getMessageEmbed())))
                                               .orElseGet(() -> eventChannel.sendMessageComponents(getItemComponents())
                                                                            .flatMap(message -> message.editMessageEmbeds(getMessageEmbed()))))
             .queue(message -> this.currentMessage = Optional.of(message));
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

    if (this.loop && !this.stopping) {
      playlist.offerFirst(track.makeClone());
    }

    playlist.remove(track);

    if (!playlist.isEmpty()) {
      player.playTrack(playlist.element());

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
    return ActionRow.of(Button.secondary("skip", Emoji.fromUnicode("⏭")),
                        Button.danger("stop", Emoji.fromUnicode("⏹")),
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
                            this.playlist.stream()
                                         .map(playlistTrack -> "· %s - %s - %s".formatted(playlistTrack.getInfo().title,
                                                                                          playlistTrack.getInfo().author,
                                                                                          Duration.ofMillis(playlistTrack.getInfo().length).toString()))
                                         .collect(Collectors.joining("\n")),
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

  public void skip() {
    playlist.remove();
    player.stopTrack();
  }

  public void stop() {
    playlist.clear();

    this.stopping = true;

    player.stopTrack();

    this.stopping = false;
    this.loop = false;
  }

  public void toggleLoop() {
    this.loop = !this.loop;

    currentMessage.map(message -> message.editMessageComponents(getItemComponents())
                                         .flatMap(edited -> edited.editMessageEmbeds(getMessageEmbed())))
                  .ifPresent(messageRestAction -> messageRestAction.queue(message -> this.currentMessage = Optional.of(message)));
  }
}
