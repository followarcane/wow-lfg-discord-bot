package followarcane.wow_lfg_discord_bot.application.request;

import lombok.Data;

@Data
public class UserRequest {
    private String discordId;
    private String username;
    private String globalName;
    private String discriminator;
    private String avatar;
    private String banner;
    private String bannerColor;
    private String locale;
    private String accessToken;
    private String refreshToken;
}
