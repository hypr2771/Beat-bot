package net.vince;

import java.io.IOException;
import java.util.Properties;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.vince.beat.Beat;
import net.vince.beat.Log;
import net.vince.beat.Menace;

public class Main {

  public static void main(String[] args) throws IOException {

    var properties = new Properties();
    var loader     = Thread.currentThread().getContextClassLoader();
    var stream     = loader.getResourceAsStream("beat.properties");
    properties.load(stream);

    // We don't need any intents for this bot. Slash commands work without any intents!
    JDA jda = JDABuilder.createLight(properties.getProperty("token"), GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS)
                        .addEventListeners(new Beat())
                        .addEventListeners(new Menace())
                        .addEventListeners(new Log())
                        .enableCache(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES)
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .setActivity(Activity.of(ActivityType.LISTENING, "/play"))
                        .build();

    var commands = Beat.getCommands();
    commands.addAll(Menace.getCommands());

    // Sets the global command list to the provided commands (removing all others)
    jda.updateCommands().addCommands(commands).queue();
  }

}