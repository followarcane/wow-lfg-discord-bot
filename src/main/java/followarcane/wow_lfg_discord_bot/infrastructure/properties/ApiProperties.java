package followarcane.wow_lfg_discord_bot.infrastructure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@Data
@ConfigurationProperties(prefix = "wow-api")
public class ApiProperties {
    private String username;
    private String password;
}
