package net.vince;

import java.io.IOException;
import java.util.Properties;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.vince.beat.Beat;

public class Main {

  public static void main(String[] args) throws IOException {

    var properties = new Properties();
    var loader     = Thread.currentThread().getContextClassLoader();
    var stream     = loader.getResourceAsStream("beat.properties");
    properties.load(stream);

    // We don't need any intents for this bot. Slash commands work without any intents!
    JDA jda = JDABuilder.createLight(properties.getProperty("token"), GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS)
                        .addEventListeners(new Beat())
                        .enableCache(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES)
                        .setMemberCachePolicy(MemberCachePolicy.ALL)
                        .setActivity(Activity.of(ActivityType.LISTENING, "/play"))
                        .build();

    // Sets the global command list to the provided commands (removing all others)
    jda.updateCommands().addCommands(
        Commands.slash("stop", "Remove all tracks")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                .setGuildOnly(true),
        Commands.slash("skip", "Skip current track")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                .setGuildOnly(true),
        Commands.slash("play", "Search and play a track")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                .setGuildOnly(true) // Ban command only works inside a guild
                .addOption(OptionType.STRING, "track", "Track to search for and play", true),
        Commands.slash("clear", "Delete all bot messages")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS))
                .setGuildOnly(true) // Ban command only works inside a guild
    ).queue();
  }

}