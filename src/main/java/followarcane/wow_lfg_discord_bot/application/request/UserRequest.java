package followarcane.wow_lfg_discord_bot.application.request;

import lombok.Data;

@Data
public class UserRequest {
    private String discordId;
    private String username;
}
