package org.bot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bot.event.DefaultEventListener;

import java.util.EnumSet;

public class DiscordBot {

    public static void run() {
        String token = "YOUR-TOKEN-KEY";

        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_MODERATION
        );

        JDABuilder builder = JDABuilder.create(token, intents);

        builder.addEventListeners(new DefaultEventListener());

        try {
            builder.build();
            System.out.println("There is no catching!!!");
        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}
