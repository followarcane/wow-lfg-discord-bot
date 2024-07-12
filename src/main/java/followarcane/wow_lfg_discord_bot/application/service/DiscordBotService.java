package followarcane.wow_lfg_discord_bot.application.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;

@Service
public class DiscordBotService {

    private JDA jda;

    @Value("${discord.bot.token}")
    private String token;

    @PostConstruct
    public void startBot() throws LoginException {
        try {
            if (token == null || token.isEmpty()) {
                throw new LoginException("Discord bot token is not provided!");
            }
            jda = JDABuilder.createDefault(token).build();
        } catch (LoginException e) {
            throw new LoginException();
        }
    }

    public void sendMessageToChannel(String channelId, String message) {
        var channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }
}
