package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.domain.model.Message;
import followarcane.wow_lfg_discord_bot.domain.repository.MessageRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;

@Service
public class DiscordBotService {
    private final MessageRepository messageRepository;

    private JDA jda;

    @Value("${discord.bot.token}")
    private String token;

    public DiscordBotService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

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

    public void sendEmbedMessageToChannel(EmbedBuilder embed) {
        jda.getGuilds().forEach(guild -> guild.getTextChannels().forEach(textChannel -> {
            if (textChannel != null) {
                textChannel.sendMessageEmbeds(embed.build()).queue();

                Message message = new Message();
                message.setMessageGuildId(guild.getId());
                message.setMessageChannelId(textChannel.getId());
                message.setMessageContent(embed.build().getTitle());
                message.setTimestamp(System.currentTimeMillis());

                messageRepository.save(message);
            }
        }));
    }
}
