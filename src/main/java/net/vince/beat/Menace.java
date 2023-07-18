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

public class Menace extends ListenerAdapter {

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
                 var ipPort   = server;

                 if (!isIpPort) {

                   var noInfo = false;

                   ipPort = switch (server) {
                     case "1" -> "51.255.222.114:27020";
                     case "2" -> "51.255.222.114:27017";
                     case "3" -> "91.134.123.42:27027";
                     default -> {
                       noInfo = true;
                       yield "";
                     }
                   };

                   if (noInfo) {
                     return hook.editOriginal("There is only 3 known servers. Either type IP:PORT of server or 1, 2, 3 for Menace's servers.");
                   }
                 }

                 var ip   = ipPort.replaceAll("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})", "$1");
                 var port = Integer.valueOf(ipPort.replaceAll("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})", "$2"));

                 try (var socket = new DatagramSocket()) {
                   socket.setSoTimeout(3 * 1_000);

                   // TSource Engine Query
                   byte data[] = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x54, (byte) 0x53, (byte) 0x6f, (byte) 0x75, (byte) 0x72, (byte) 0x63, (byte) 0x65, (byte) 0x20, (byte) 0x45, (byte) 0x6e, (byte) 0x67, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20, (byte) 0x51, (byte) 0x75, (byte) 0x65, (byte) 0x72, (byte) 0x79, (byte) 0x00};

                   System.out.println("send:     " + new String(data));

                   DatagramPacket dp = new DatagramPacket(data, 0, data.length, InetAddress.getByName(ip), port);
                   socket.send(dp);
                   byte[]         rec = new byte[1024];
                   DatagramPacket dp2 = new DatagramPacket(rec, 1024);
                   socket.receive(dp2);

                   System.out.println("received: " + new String(rec));

                   var info             = new String(rec).split(String.valueOf((char) 0x00));
                   var name             = info[0].substring(6);
                   var map              = info[1];
                   var state            = info[3];
                   var connectedPlayers = info[5].length() == 0 ? (byte) 0 : (byte) info[5].charAt(0);
                   var maxPlayers       = info[5].length() == 0 ? (byte) info[6].charAt(0) : (byte) info[5].charAt(1);

                   return hook.sendMessageEmbeds(new MessageEmbed(null,
                                                                  name,
                                                                  """
                                                                      · Map: %s
                                                                      · State: %s
                                                                      · Players: %s/%s
                                                                      """.formatted(map, state, connectedPlayers, maxPlayers),
                                                                  EmbedType.RICH,
                                                                  null,
                                                                  0x00FFFF,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null,
                                                                  null));


                 } catch (IOException e) {
                   return hook.editOriginal("Server %s did not respond...".formatted(ipPort));
                 } catch (ArrayIndexOutOfBoundsException e) {
                   return hook.editOriginal("Server uses a different version of HLDS API...".formatted(ipPort));
                 }

               }).queue();
        }
        default -> event.reply("Unknown command " + commandId).queue();
      }
    }

  }

}
