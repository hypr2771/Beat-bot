package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command.Type;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Beat extends ListenerAdapter {

  private static final List<CommandData> COMMANDS = new ArrayList<>(List.of(Commands.slash("stop", "Remove all tracks")
                                                                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                    .setGuildOnly(true),
                                                                            Commands.slash("skip", "Skip current track")
                                                                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                    .setGuildOnly(true),
                                                                            Commands.slash("loop", "Loop current track")
                                                                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                    .setGuildOnly(true),
                                                                            Commands.slash("play", "Search and play a track")
                                                                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                    .setGuildOnly(true)
                                                                                    .addOption(OptionType.STRING, "track", "Track to search for and play", true),
                                                                            Commands.slash("clear", "Delete all bot messages")
                                                                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                    .setGuildOnly(true)));

  private final AudioPlayerManager      audioManager;
  private final Map<Long, TrackManager> guildTrackManagers;

  public static Collection<CommandData> getCommands() {
    return new ArrayList<>(COMMANDS);
  }

  public Beat() {

    audioManager = new DefaultAudioPlayerManager();

    AudioSourceManagers.registerRemoteSources(audioManager);
    AudioSourceManagers.registerLocalSource(audioManager);

    guildTrackManagers = new HashMap<>();
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

    if (COMMANDS.stream().noneMatch(supportedCommand -> supportedCommand.getName().equals(event.getFullCommandName()))) {
      System.out.println("Not a Beat command");
      return;
    }

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
        case "loop" -> {
          trackManager.toggleLoop();
          event.deferReply(true)
               .flatMap(InteractionHook::deleteOriginal)
               .queue();
        }
        case "play" -> {
          var track = event.getOption("track").getAsString();

          event.deferReply(true)
               .queue(hook -> {

                 var isUri       = false;
                 var actualTrack = track;

                 try {
                   new URL(actualTrack);
                   isUri = true;
                 } catch (Exception ignored) {
                 }

                 if (!isUri) {
                   try {
                     actualTrack = HttpClient.newHttpClient().send(
                                                 HttpRequest.newBuilder(URI.create("https://www.youtube.com/youtubei/v1/search"))
                                                            .POST(BodyPublishers.ofString("""
                                                                                              {
                                                                                                "context": {
                                                                                                  "client": {
                                                                                                    "clientName": "WEB",
                                                                                                    "clientVersion": "2.20230714.00.00"
                                                                                                  },
                                                                                                  "user": {
                                                                                                    "lockedSafetyMode": false
                                                                                                  },
                                                                                                  "request": {
                                                                                                    "useSsl": true,
                                                                                                    "internalExperimentFlags": [],
                                                                                                    "consistencyTokenJars": []
                                                                                                  }
                                                                                                },
                                                                                                "query": "{QUERY}",
                                                                                              }
                                                                                              """.replace("{QUERY}", track)))
                                                            .build(),
                                                 BodyHandlers.ofLines())
                                             .body()
                                             .filter(line -> line.contains("videoId"))
                                             .map(videoIds -> videoIds.replaceAll(".+\"videoId\": \"(.+)\",", "$1"))
                                             .findFirst()
                                             .get();
                   } catch (IOException e) {
                     throw new RuntimeException(e);
                   } catch (InterruptedException e) {
                     throw new RuntimeException(e);
                   }

                   actualTrack = "https://www.youtube.com/watch?v=" + actualTrack;
                 }

                 audioManager.loadItem(actualTrack,
                                       new GuildAudioLoadResultHandler(event, guild, trackManager, hook));
               });
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
      case "loop" -> {
        trackManager.toggleLoop();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      }
      default -> event.reply("Unknown command " + event.getButton().getId()).queue();
    }

  }

}
