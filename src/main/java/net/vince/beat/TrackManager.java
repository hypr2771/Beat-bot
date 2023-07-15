package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.vince.beat.mixin.AudioPlayerSendHandler;

public class TrackManager extends AudioEventAdapter {

  private final AudioPlayer       player;
  private final Guild             guild;
  private final Deque<AudioTrack> playlist;

  private Optional<Message> currentMessage;

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

    if (!player.startTrack(track, true)) {
      playlist.offer(track);
    }

    replyHook.deleteOriginal()
             .flatMap(ignored -> currentMessage.<RestAction<Message>>map(message -> message.editMessageComponents(ActionRow.of(Button.secondary("skip", Emoji.fromUnicode("⏭")),
                                                                                                                               Button.danger("stop", Emoji.fromUnicode("⏹")))))
                                               .orElseGet(() -> eventChannel.sendMessageComponents(ActionRow.of(Button.secondary("skip", Emoji.fromUnicode("⏭")),
                                                                                                                Button.danger("stop", Emoji.fromUnicode("⏹"))))))
             .queue(message -> this.currentMessage = Optional.of(message));
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    if (!playlist.isEmpty()) {
      player.playTrack(playlist.pop());
    } else {
      currentMessage.map(Message::delete)
                    .ifPresent(RestAction::queue);
      currentMessage = Optional.empty();
      guild.getAudioManager().closeAudioConnection();
    }
  }

  public void skip() {
    player.stopTrack();
  }

  public void stop() {
    playlist.clear();
    player.stopTrack();
  }
}
