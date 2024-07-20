package followarcane.wow_lfg_discord_bot.application.request;

import lombok.Data;

@Data
public class DiscordServerRequest {
    private String serverId;
    private String serverName;
    private String ownerId;
    private String icon;
}
