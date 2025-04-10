package followarcane.wow_lfg_discord_bot.application.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RaiderIODataResponse {
    private String classType;
    private String activeSpecName;
    private String activeSpecRole;
    private String race;
    private String gender;
    private String faction;
    private String achievementPoints;
    private String thumbnailUrl;
}
