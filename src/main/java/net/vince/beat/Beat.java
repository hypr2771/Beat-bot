package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command.Type;

public class Beat extends ListenerAdapter {

  private final AudioPlayerManager      audioManager;
  private final Map<Long, TrackManager> guildTrackManagers;

  public Beat() {

    audioManager = new DefaultAudioPlayerManager();

    AudioSourceManagers.registerRemoteSources(audioManager);
    AudioSourceManagers.registerLocalSource(audioManager);

    guildTrackManagers = new HashMap<>();
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

    if (event.getCommandType() == Type.SLASH) {

      String commandId = event.getFullCommandName();

      var guild = event.getGuild();

      if (!guildTrackManagers.containsKey(guild.getIdLong())) {
        guildTrackManagers.put(guild.getIdLong(), new TrackManager(audioManager, guild));
      }

      var trackManager = guildTrackManagers.get(guild.getIdLong());

      switch (commandId) {
        case "skip" -> {
          trackManager.skip();
          event.reply("Skipped").queue();
        }
        case "stop" -> {
          trackManager.stop();
          event.reply("Bye").queue();
        }
        case "play" -> {
          var track = event.getOption("track").getAsString();
          event.reply("Searching " + track + "...")
               .queue(hook -> audioManager.loadItem(track,
                                                    new GuildAudioLoadResultHandler(event, guild, trackManager, hook)));
        }
        default -> event.reply("Unknown command " + commandId).queue();
      }
    }

  }

  @Override
  public void onGenericEvent(GenericEvent event) {
    System.out.println(event);
  }

}
