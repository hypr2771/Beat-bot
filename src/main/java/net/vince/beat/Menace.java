package net.vince.beat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command.Type;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

public class Menace extends ListenerAdapter {

  private static final List<String> IPS = List.of("217.156.22.183:27015",
                                                  "217.156.22.182:27015");

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

          var isNumber     = false;
          var serverNumber = 0;
          try {
            serverNumber = event.getOption("server").getAsInt();
            isNumber = true;
          } catch (Exception e) {
            // Do nothing, just checking whether we can parse server variable as a number
          }

          var isIpPort = server.matches("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");
          var ipPorts  = List.<String>of();

          if (!isIpPort) {

            var noInfo = false;

            if (isNumber) {
              ipPorts = IPS.subList(serverNumber - 1, serverNumber);
            } else if (server.equals("all")) {
              ipPorts = IPS;
            } else {
              noInfo  = true;
              ipPorts = List.of();
            }

            if (noInfo) {
              event.reply("⛔️ There is only %s known servers. Either type IP:PORT of server, \"all\" or the server index you want to get information about.".formatted(IPS.size())).queue();
            }
          } else {
            ipPorts = List.of(server);
          }

          event.deferReply().queue();

          ipPorts.parallelStream()
                 .map(this::createMessage)
                 .collect(messagesToRestAction(event))
                 .queue();
        }
        default -> event.reply("⁉️ Unknown command " + commandId).queue();
      }
    }

  }

  @NotNull
  private Collector<MessageEmbed, Deque<RestAction<?>>, RestAction<?>> messagesToRestAction(final SlashCommandInteractionEvent event) {
    return new Collector<>() {

      final InteractionHook    hook   = event.getInteraction().getHook();
      final List<MessageEmbed> embeds = new ArrayList<>();

      @Override
      public Supplier<Deque<RestAction<?>>> supplier() {
        return LinkedList::new;
      }

      @Override
      public BiConsumer<Deque<RestAction<?>>, MessageEmbed> accumulator() {
        return (restActions, messageEmbed) -> {
          embeds.add(messageEmbed);
          restActions.offer(restActions.isEmpty() ?
                            hook.editOriginalEmbeds(embeds) :
                            restActions.peekLast().and(hook.editOriginalEmbeds(embeds)));
        };
      }

      @Override
      public BinaryOperator<Deque<RestAction<?>>> combiner() {
        return (restActions, restActions2) -> restActions2;
      }

      @Override
      public Function<Deque<RestAction<?>>, RestAction<?>> finisher() {
        return Deque::peekLast;
      }

      @Override
      public Set<Characteristics> characteristics() {
        return Set.of();
      }
    };
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

      var isFull = connectedPlayers == maxPlayers;

      return new MessageEmbed("https://connectsteam.me/?%s:%s".formatted(ip, port),
                              name,
                              """
                                  · 🗾 Map: %s
                                  · ℹ️ State: %s
                                  · %s: %s/%s
                                  """.formatted(map,
                                                state,
                                                isFull ? "⚠️ Players:" : "✅ Players:", connectedPlayers, maxPlayers),
                              EmbedType.RICH,
                              null,
                              isFull ? 0x2986cc : 0x5dd200,
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
                              "⛔️ Server %s did not respond...".formatted(ipPort),
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
                              "⛔️ Server uses a different version of HLDS API...",
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
