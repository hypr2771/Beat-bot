package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.vince.beat.mixin.AudioPlayerSendHandler;

public class TrackManager extends AudioEventAdapter {

  private final AudioPlayer       player;
  private final Guild             guild;
  private final Deque<AudioTrack> playlist;

  public TrackManager(AudioPlayerManager audioManager, Guild guild) {
    var player = audioManager.createPlayer();
    player.addListener(this);

    guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

    this.player   = player;
    this.guild    = guild;
    this.playlist = new ConcurrentLinkedDeque<>();
  }

  public void queue(AudioTrack track, final AudioChannelUnion channel) {

    guild.getAudioManager().openAudioConnection(channel);

    if (!player.startTrack(track, true)) {
      playlist.offer(track);
    }
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    if (!playlist.isEmpty()) {
      player.playTrack(playlist.pop());
    } else {
      guild.getAudioManager().closeAudioConnection();
    }
  }

  public void skip() {
    player.stopTrack();
  }

  public void stop() {
    playlist.clear();
    skip();
  }
}
