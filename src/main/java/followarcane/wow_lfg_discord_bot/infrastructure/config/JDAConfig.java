package followarcane.wow_lfg_discord_bot.infrastructure.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JDAConfig {

    @Value("${discord.bot.token}")
    private String token;

    @Bean
    public JDA jda() {
        return JDABuilder.createDefault(token).build();
    }
}