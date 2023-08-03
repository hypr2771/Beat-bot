package net.vince.beat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

class MenaceTest {

  @org.junit.jupiter.api.Test
  void onSlashCommandInteraction() {

    try (var socket = new DatagramSocket()) {
      socket.setSoTimeout(3 * 1_000);

      // TSource Engine Query
      byte data[] = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x54, (byte) 0x53, (byte) 0x6f, (byte) 0x75, (byte) 0x72, (byte) 0x63, (byte) 0x65, (byte) 0x20, (byte) 0x45, (byte) 0x6e, (byte) 0x67, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20, (byte) 0x51, (byte) 0x75, (byte) 0x65, (byte) 0x72, (byte) 0x79, (byte) 0x00};

      System.out.println("send:     " + new String(data));

      DatagramPacket infoRequest = new DatagramPacket(data, 0, data.length, InetAddress.getByName("37.187.180.60"), 27015);
      socket.send(infoRequest);

      byte[]         challengePacketBytes = new byte[1024];
      DatagramPacket challengePacket      = new DatagramPacket(challengePacketBytes, 1024);
      socket.receive(challengePacket);

      System.out.println("received: " + new String(challengePacketBytes));
      var info = new String(challengePacketBytes).split(String.valueOf((char) 0x00));

      if (challengePacketBytes[4] == (byte) 0x41) {
        byte dataPostChallenge[] = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x54, (byte) 0x53, (byte) 0x6f, (byte) 0x75, (byte) 0x72, (byte) 0x63, (byte) 0x65, (byte) 0x20, (byte) 0x45, (byte) 0x6e, (byte) 0x67, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20, (byte) 0x51, (byte) 0x75, (byte) 0x65, (byte) 0x72, (byte) 0x79, (byte) 0x00, challengePacketBytes[5], challengePacketBytes[6], challengePacketBytes[7], challengePacketBytes[8]};

        System.out.println("send:     " + new String(data));

        DatagramPacket infoRequestPostChallenge = new DatagramPacket(dataPostChallenge, 0, dataPostChallenge.length, InetAddress.getByName("37.187.180.60"), 27015);
        socket.send(infoRequestPostChallenge);

        byte[]         postChallengePacketBytes = new byte[1024];
        DatagramPacket infoPacket = new DatagramPacket(postChallengePacketBytes, 1024);
        socket.receive(infoPacket);

        System.out.println("received: " + new String(postChallengePacketBytes));
        info = new String(postChallengePacketBytes).split(String.valueOf((char) 0x00));
      }

      var name             = info[0].substring(6);
      var map              = info[1];
      var state            = info[3];
      var connectedPlayers = info[5].isEmpty() ? (byte) 0 : (byte) info[5].charAt(0);
      var maxPlayers       = info[5].isEmpty() ? (byte) info[6].charAt(0) : (byte) info[5].charAt(1);
    } catch (SocketException | UnknownHostException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}