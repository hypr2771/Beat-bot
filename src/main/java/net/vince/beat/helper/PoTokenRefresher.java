package net.vince.beat.helper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import dev.lavalink.youtube.clients.Web;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;

public class PoTokenRefresher {

  public static void refresh() {
    try {
      var token = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                    .readerFor(Token.class)
                                    .<Token>readValue(
                                        String.valueOf(HttpClient.newHttpClient()
                                                                 .send(
                                                                     HttpRequest.newBuilder()
                                                                                .GET()
                                                                                .uri(URI.create("http://localhost:8980/token"))
                                                                                .build(),
                                                                     HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                                                                 ).body()));

      Web.setPoTokenAndVisitorData(token.potoken(), token.visitorData());
    } catch (IOException e) {
      System.err.println(e);
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      System.err.println(e);
      throw new RuntimeException(e);
    }

  }

  private record Token(String potoken, String visitorData) {

  }

}
