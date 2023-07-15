package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
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
          event.deferReply(true)
               .flatMap(InteractionHook::deleteOriginal)
               .queue();
        }
        case "stop" -> {
          trackManager.stop();
          event.deferReply(true)
               .flatMap(InteractionHook::deleteOriginal)
               .queue();
        }
        case "play" -> {
          var track = event.getOption("track").getAsString();
          event.deferReply(true)
               .queue(hook -> audioManager.loadItem(track,
                                                    new GuildAudioLoadResultHandler(event, guild, trackManager, hook)));
        }
        case "clear" -> {
          event.deferReply(true)
               .flatMap(hook -> event.getMessageChannel().getHistory().retrievePast(100))
               .map(messages -> messages.stream().filter(message -> message.getAuthor().getId().equals("1129054171383996509")).toList())
               .map(messages -> event.getGuildChannel().purgeMessages(messages))
               .queue();
        }
        default -> event.reply("Unknown command " + commandId).queue();
      }
    }

  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {

    var guild = event.getGuild();

    if (!guildTrackManagers.containsKey(guild.getIdLong())) {
      guildTrackManagers.put(guild.getIdLong(), new TrackManager(audioManager, guild));
    }

    var trackManager = guildTrackManagers.get(guild.getIdLong());

    switch (event.getButton().getId()) {
      case "skip" -> {
        trackManager.skip();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      }
      case "stop" -> {
        trackManager.stop();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      }
      default -> event.reply("Unknown command " + event.getButton().getId()).queue();
    }

  }

  @Override
  public void onGenericEvent(GenericEvent event) {
    System.out.println(event);
  }

}
