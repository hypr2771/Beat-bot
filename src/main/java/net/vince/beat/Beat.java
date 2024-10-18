package net.vince.beat;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
  import dev.lavalink.youtube.clients.AndroidMusic;
import dev.lavalink.youtube.clients.Music;
import dev.lavalink.youtube.clients.TvHtml5Embedded;
import dev.lavalink.youtube.clients.WebEmbedded;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command.Type;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.vince.beat.helper.KeyboardTranslation;

public class Beat extends ListenerAdapter {

  private static final List<CommandData> COMMANDS = new ArrayList<>(Stream.of(Commands.slash("previous", "Return to previous track")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true),
                                                                              Commands.slash("stop", "Remove all tracks")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true),
                                                                              Commands.slash("next", "Skip to next track")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true),
                                                                              Commands.slash("jump", "Jump to given track index in the playlist")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true)
                                                                                      .addOption(OptionType.STRING, "index", "Index of the track to play (number next to the name of the track)", true),
                                                                              Commands.slash("remove", "Remove tracks from playlist at provided indices")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true)
                                                                                      .addOption(OptionType.STRING, "tracks", "Indices of tracks to remove from playlist", true),
                                                                              Commands.slash("loop", "Loop current track")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true),
                                                                              Commands.slash("pause", "Pause or play back current track")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true),
                                                                              Commands.slash("play", "Search and play a track")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true)
                                                                                      .addOption(OptionType.STRING, "track", "Track to search for and play", true),
                                                                              Commands.slash("list", "List playlists available for further replay")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true),
                                                                              Commands.slash("save", "Save this playlist for further replay")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true)
                                                                                      .addOption(OptionType.STRING, "playlist", "Name of the playlist", true),
                                                                              Commands.slash("load", "Load a saved playlist")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true)
                                                                                      .addOption(OptionType.STRING, "playlist", "Name of the playlist", true),
                                                                              Commands.slash("clear", "Delete all bot messages")
                                                                                      .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                      .setGuildOnly(true))
                                                                          .flatMap(slashCommandData -> Stream.of(slashCommandData,
                                                                                                                 Commands.slash(KeyboardTranslation.toThai(slashCommandData.getName()),
                                                                                                                                slashCommandData.getDescription())
                                                                                                                         .setDefaultPermissions(slashCommandData.getDefaultPermissions())
                                                                                                                         .setGuildOnly(slashCommandData.isGuildOnly())
                                                                                                                         .addOptions(slashCommandData.getOptions())))
                                                                          .toList());

  private final AudioPlayerManager      audioManager;
  private final Map<Long, TrackManager> guildTrackManagers;

  public static Collection<CommandData> getCommands() {
    return new ArrayList<>(COMMANDS);
  }

  public Beat() {

    audioManager = new DefaultAudioPlayerManager();

    YoutubeAudioSourceManager ytSourceManager = new dev.lavalink.youtube.YoutubeAudioSourceManager(new Music(), new WebEmbedded(), new TvHtml5Embedded(), new AndroidMusic());
    audioManager.registerSourceManager(ytSourceManager);

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

      if (KeyboardTranslation.equals("previous", commandId)) {
        trackManager.previous();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      } else if (KeyboardTranslation.equals("stop", commandId)) {
        trackManager.stop();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      } else if (KeyboardTranslation.equals("next", commandId)) {
        trackManager.next();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      } else if (KeyboardTranslation.equals("jump", commandId)) {

        var index = event.getOption("index").getAsInt();

        trackManager.jumpTo(index);
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      } else if (KeyboardTranslation.equals("remove", commandId)) {

        var indices = Arrays.stream(event.getOption("tracks")
                                         .getAsString()
                                         .split(","))
                            .map(String::trim)
                            .<Optional<Integer>>map(index -> {
                              try {
                                return Optional.of(Integer.parseInt(index));
                              } catch (Exception e) {
                                return Optional.empty();
                              }
                            })
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            // Remove duplicates
                            .collect(Collectors.toSet())
                            .stream()
                            .toList();

        trackManager.remove(indices);
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      } else if (KeyboardTranslation.equals("loop", commandId)) {
        trackManager.toggleLoop();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      } else if (KeyboardTranslation.equals("pause", commandId)) {
        trackManager.togglePause();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      } else if (KeyboardTranslation.equals("list", commandId)) {

        try {

          var files = Files.find(Path.of("."),
                                 1,
                                 (path, basicFileAttributes) -> path.getFileName().toString().startsWith(guild.getId()) &&
                                                                path.getFileName().toString().endsWith(".playlist") &&
                                                                basicFileAttributes.isRegularFile());

          event.deferReply()
               .flatMap(interactionHook -> interactionHook.editOriginalEmbeds(new MessageEmbed(null,
                                                                                               "Available playlists",
                                                                                               "· " + files.map(path -> {
                                                                                                             try {
                                                                                                               return "%s (%s tracks)".formatted(path.getFileName()
                                                                                                                                                     .toString()
                                                                                                                                                     .replace(guild.getId() + ".", "")
                                                                                                                                                     .replace(".playlist", ""),
                                                                                                                                                 Files.readAllLines(path).size());
                                                                                                             } catch (IOException e) {
                                                                                                               return "%s (unknown number of tracks)".formatted(path.getFileName()
                                                                                                                                                                    .toString()
                                                                                                                                                                    .replace(guild.getId() + ".", "")
                                                                                                                                                                    .replace(".playlist", ""));
                                                                                                             }
                                                                                                           })
                                                                                                           .collect(Collectors.joining("\n· ")),
                                                                                               EmbedType.RICH,
                                                                                               null,
                                                                                               0x5dd200,
                                                                                               null,
                                                                                               null,
                                                                                               null,
                                                                                               null,
                                                                                               null,
                                                                                               null,
                                                                                               null)))
               .queue();

        } catch (IOException e) {
          event.deferReply(true)
               .flatMap(interactionHook -> interactionHook.editOriginal("Playlists could not be fetched."))
               .queue();
          throw new RuntimeException(e);
        }

      } else if (KeyboardTranslation.equals("save", commandId)) {

        if (trackManager.getPlaylist().isEmpty()) {
          event.deferReply(true)
               .flatMap(interactionHook -> interactionHook.editOriginal("No tracks are playing. Please start a track before saving a playlist."))
               .queue();

          return;
        }

        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();

        try {
          Files.writeString(Path.of("./%s.%s.playlist".formatted(guild.getId(), event.getOption("playlist").getAsString())),
                            trackManager.getPlaylist()
                                        .stream()
                                        .map(audioTrack -> audioTrack.getInfo().uri)
                                        .collect(Collectors.joining("\n")));
        } catch (IOException e) {
          event.deferReply(true)
               .flatMap(interactionHook -> interactionHook.editOriginal("Playlist %s could not be saved.".formatted(event.getOption("playlist").getAsString())))
               .queue();
          throw new RuntimeException(e);
        }

      } else if (KeyboardTranslation.equals("load", commandId)) {

        List<String> trackUrls;
        try {
          trackUrls = Files.readAllLines(Path.of("./%s.%s.playlist".formatted(guild.getId(), event.getOption("playlist").getAsString())));
        } catch (IOException e) {
          event.deferReply(true)
               .flatMap(interactionHook -> interactionHook.editOriginal("Playlist %s does not exists.".formatted(event.getOption("playlist").getAsString())))
               .queue();
          throw new RuntimeException(e);
        }

        if (trackUrls.isEmpty()) {
          event.deferReply(true)
               .flatMap(interactionHook -> interactionHook.editOriginal("Playlist %s is empty.".formatted(event.getOption("playlist").getAsString())))
               .queue();
          return;
        }

        event.deferReply(true)
             .queue(interactionHook ->
                    {

                      for (String trackUrl : trackUrls) {
                        var loader = audioManager.loadItem(trackUrl,
                                                           new GuildAudioLoadResultHandler(event, guild, trackManager, interactionHook));

                        while (!loader.isCancelled() && !loader.isDone()) {
                        }

                      }

                    });

      } else if (KeyboardTranslation.equals("play", commandId)) {
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
      } else if (KeyboardTranslation.equals("clear", commandId)) {
        event.deferReply(true)
             .flatMap(hook -> event.getMessageChannel().getHistory().retrievePast(100))
             .map(messages -> messages.stream().filter(message -> message.getAuthor().getId().equals("1129054171383996509")).toList())
             .map(messages -> event.getGuildChannel().purgeMessages(messages))
             .queue();
      } else {
        event.reply("Unknown command " + commandId).queue();
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
      case "previous" -> {
        trackManager.previous();
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
      case "next" -> {
        trackManager.next();
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
      case "pause" -> {
        trackManager.togglePause();
        event.deferReply(true)
             .flatMap(InteractionHook::deleteOriginal)
             .queue();
      }
      default -> event.reply("Unknown command " + event.getButton().getId()).queue();
    }

  }

}
