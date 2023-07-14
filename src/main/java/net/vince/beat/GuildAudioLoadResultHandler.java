package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class GuildAudioLoadResultHandler implements AudioLoadResultHandler {

  private final SlashCommandInteractionEvent event;
  private final Guild                        guild;
  private final TrackManager                 trackManager;

  public GuildAudioLoadResultHandler(SlashCommandInteractionEvent event, Guild guild, TrackManager trackManager) {
    this.event        = event;
    this.guild        = guild;
    this.trackManager = trackManager;
  }

  @Override
  public void trackLoaded(AudioTrack track) {
    event.reply("Adding to queue " + track.getInfo().title + " for " + event.getMember().getEffectiveName()).queue();

    trackManager.queue(track, event.getMember().getVoiceState().getChannel());
  }

  @Override
  public void playlistLoaded(AudioPlaylist playlist) {
    AudioTrack firstTrack = playlist.getSelectedTrack();

    if (firstTrack == null) {
      firstTrack = playlist.getTracks().get(0);
    }

    event.reply("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

    trackManager.queue(playlist.getSelectedTrack(), event.getMember().getVoiceState().getChannel());
  }

  @Override
  public void noMatches() {
    event.reply("Nothing found by " + event.getOption("track").getAsString()).queue();
  }

  @Override
  public void loadFailed(FriendlyException exception) {
    event.reply("Could not play: " + exception.getMessage()).queue();
  }
}

