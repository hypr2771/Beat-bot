package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildAudioLoadResultHandler implements AudioLoadResultHandler {

  private static final Logger log = LoggerFactory.getLogger(GuildAudioLoadResultHandler.class);

  private final SlashCommandInteractionEvent event;
  private final Guild                        guild;
  private final TrackManager                 trackManager;
  private final InteractionHook              replyHook;

  public GuildAudioLoadResultHandler(SlashCommandInteractionEvent event, Guild guild, TrackManager trackManager, InteractionHook replyHook) {
    this.event        = event;
    this.guild        = guild;
    this.trackManager = trackManager;
    this.replyHook    = replyHook;
  }

  @Override
  public void trackLoaded(AudioTrack track) {
    trackManager.queue(track,
                       event.getMember().getVoiceState().getChannel(),
                       replyHook,
                       event.getChannel());
  }

  @Override
  public void playlistLoaded(AudioPlaylist playlist) {

    replyHook.editOriginal("✅ Adding to queue " + playlist.getName()).queue();

    trackManager.queue(playlist.getTracks().stream().findFirst().get(),
                       event.getMember().getVoiceState().getChannel(),
                       replyHook,
                       event.getChannel());

    for (AudioTrack track : playlist.getTracks().subList(1, playlist.getTracks().size())) {
      trackManager.queue(track,
                         event.getMember().getVoiceState().getChannel(),
                         replyHook,
                         event.getChannel(),
                         false);
    }

    trackManager.jumpTo(playlist.getTracks().indexOf(playlist.getSelectedTrack()) + 1);
  }

  @Override
  public void noMatches() {
    log.info("⚠️ Nothing found for {}", event.getOption("track").getAsString());
    replyHook.editOriginal("⚠️ Nothing found for " + event.getOption("track").getAsString()).queue();
  }

  @Override
  public void loadFailed(FriendlyException exception) {
    log.info("⛔️ Could not play: {}", exception.getMessage(), exception);
    replyHook.editOriginal("⛔️ Could not play: " + exception.getMessage()).queue();
  }
}

