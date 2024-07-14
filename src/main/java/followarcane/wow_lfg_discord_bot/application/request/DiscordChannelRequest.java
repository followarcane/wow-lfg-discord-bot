package followarcane.wow_lfg_discord_bot.application.request;

import lombok.Data;

@Data
public class DiscordChannelRequest {
    private String channelId;
    private String channelName;
    private String serverId;
}
