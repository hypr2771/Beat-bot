package net.vince.beat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command.Type;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class Menace extends ListenerAdapter {

  private static final List<String> IPS = List.of("51.75.246.41:27015",
                                                  "141.94.16.52:27015",
                                                  "185.47.128.104:27015",
                                                  "89.117.49.143:27015");

  private static final List<CommandData> COMMANDS = new ArrayList<>(List.of(Commands.slash("info", "Get info on CS server")
                                                                                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                                                                                    .setGuildOnly(true)
                                                                                    .addOption(OptionType.STRING, "server", "The server to query information from", true)));

  public static Collection<CommandData> getCommands() {
    return new ArrayList<>(COMMANDS);
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

    if (COMMANDS.stream().noneMatch(supportedCommand -> supportedCommand.getName().equals(event.getFullCommandName()))) {
      System.out.println("Not a Menace command");
      return;
    }

    if (event.getCommandType() == Type.SLASH) {

      String commandId = event.getFullCommandName();

      switch (commandId) {
        case "info" -> {
          var server = event.getOption("server").getAsString();

          event.deferReply()
               .flatMap(hook -> {

                 var isIpPort = server.matches("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");
                 var ipPorts  = List.<String>of();

                 if (!isIpPort) {

                   var noInfo = false;

                   ipPorts = switch (server) {
                     case "1" -> IPS.subList(0, 1);
                     case "2" -> IPS.subList(1, 2);
                     case "3" -> IPS.subList(2, 3);
                     case "all" -> IPS;
                     default -> {
                       noInfo = true;
                       yield List.of();
                     }
                   };

                   if (noInfo) {
                     return hook.editOriginal("There is only 3 known servers. Either type IP:PORT of server or 1, 2, 3 for Menace's servers.");
                   }
                 } else {
                   ipPorts = List.of(server);
                 }

                 var messages = ipPorts.parallelStream()
                                       .map(this::createMessage)
                                       .toList();

                 return hook.sendMessageEmbeds(messages);
               }).queue();
        }
        default -> event.reply("Unknown command " + commandId).queue();
      }
    }

  }

  @NotNull
  private MessageEmbed createMessage(final String ipPort) {
    var ip   = ipPort.replaceAll("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})", "$1");
    var port = Integer.parseInt(ipPort.replaceAll("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})", "$2"));

    try (var socket = new DatagramSocket()) {
      socket.setSoTimeout(3 * 1_000);

      // TSource Engine Query
      byte[] data = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x54, (byte) 0x53, (byte) 0x6f, (byte) 0x75, (byte) 0x72, (byte) 0x63, (byte) 0x65, (byte) 0x20, (byte) 0x45, (byte) 0x6e, (byte) 0x67, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20, (byte) 0x51, (byte) 0x75, (byte) 0x65, (byte) 0x72, (byte) 0x79, (byte) 0x00};

      System.out.println("send:     " + new String(data));

      DatagramPacket infoRequest = new DatagramPacket(data, 0, data.length, InetAddress.getByName(ip), port);
      socket.send(infoRequest);

      byte[]         challengePacketBytes = new byte[1024];
      DatagramPacket challengePacket      = new DatagramPacket(challengePacketBytes, 1024);
      socket.receive(challengePacket);

      System.out.println("received: " + new String(challengePacketBytes));
      var info = new String(challengePacketBytes).split(String.valueOf((char) 0x00));

      if (challengePacketBytes[4] == (byte) 0x41) {
        byte[] dataPostChallenge = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x54, (byte) 0x53, (byte) 0x6f, (byte) 0x75, (byte) 0x72, (byte) 0x63, (byte) 0x65, (byte) 0x20, (byte) 0x45, (byte) 0x6e, (byte) 0x67, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20, (byte) 0x51, (byte) 0x75, (byte) 0x65, (byte) 0x72, (byte) 0x79, (byte) 0x00, challengePacketBytes[5], challengePacketBytes[6], challengePacketBytes[7], challengePacketBytes[8]};

        System.out.println("send:     " + new String(data));

        DatagramPacket infoRequestPostChallenge = new DatagramPacket(dataPostChallenge, 0, dataPostChallenge.length, InetAddress.getByName(ip), port);
        socket.send(infoRequestPostChallenge);

        byte[]         postChallengePacketBytes = new byte[1024];
        DatagramPacket infoPacket               = new DatagramPacket(postChallengePacketBytes, 1024);
        socket.receive(infoPacket);

        System.out.println("received: " + new String(postChallengePacketBytes));
        info = new String(postChallengePacketBytes).split(String.valueOf((char) 0x00));
      }

      var name             = info[0].substring(6);
      var map              = info[1];
      var state            = info[3];
      var connectedPlayers = info[5].isEmpty() ? (byte) 0 : (byte) info[5].charAt(0);
      var maxPlayers       = info[5].isEmpty() ? (byte) info[6].charAt(0) : (byte) info[5].charAt(1);

      return new MessageEmbed(null,
                              name,
                              """
                                  · Map: %s
                                  · State: %s
                                  · Players: %s/%s
                                  """.formatted(map, state, connectedPlayers, maxPlayers),
                              EmbedType.RICH,
                              null,
                              connectedPlayers == maxPlayers ? 0x2986cc : 0x5dd200,
                              null,
                              null,
                              null,
                              null,
                              null,
                              null,
                              null);


    } catch (IOException e) {
      return new MessageEmbed(null,
                              "Failure for %s".formatted(ipPort),
                              "Server %s did not respond...".formatted(ipPort),
                              EmbedType.RICH,
                              null,
                              0xff4040,
                              null,
                              null,
                              null,
                              null,
                              null,
                              null,
                              null);
    } catch (ArrayIndexOutOfBoundsException e) {
      return new MessageEmbed(null,
                              "Failure for %s".formatted(ipPort),
                              "Server uses a different version of HLDS API...",
                              EmbedType.RICH,
                              null,
                              0xff7f00,
                              null,
                              null,
                              null,
                              null,
                              null,
                              null,
                              null);
    }
  }

}
