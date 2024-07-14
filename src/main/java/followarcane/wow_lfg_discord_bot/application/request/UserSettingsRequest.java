package followarcane.wow_lfg_discord_bot.application.request;

import lombok.Data;

@Data
public class UserSettingsRequest {
    private String language;
    private String realm;
    private String region;
    private String serverId;
    private String channelId;
    private String userDiscordId;
}
