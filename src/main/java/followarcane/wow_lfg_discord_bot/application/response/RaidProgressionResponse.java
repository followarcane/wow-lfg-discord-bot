package followarcane.wow_lfg_discord_bot.application.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RaidProgressionResponse {
    private String raidName;
    private String summary;
}
