package followarcane.wow_lfg_discord_bot.application.response;

import followarcane.wow_lfg_discord_bot.domain.FeatureType;
import lombok.Data;

@Data
public class ServerFeatureResponse {
    private FeatureType featureType;
    private String displayName;
    private boolean enabled;

    public ServerFeatureResponse(FeatureType featureType, boolean enabled) {
        this.featureType = featureType;
        this.displayName = featureType.getDisplayName();
        this.enabled = enabled;
    }
} 