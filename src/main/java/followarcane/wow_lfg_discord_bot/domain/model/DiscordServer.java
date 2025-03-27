package followarcane.wow_lfg_discord_bot.domain.model;

import followarcane.wow_lfg_discord_bot.domain.FeatureType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(name = "discord_servers")
public class DiscordServer {
    @Id
    @Column(name = "server_id")
    private String serverId;
    private String serverName;
    private String ownerId;
    private String systemChannelId;
    private String prefix;
    private String icon;
    private String banner;
    private String description;
    boolean active = true;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<ServerFeature> features = new HashSet<>();

    public boolean isFeatureEnabled(FeatureType featureType) {
        return features.stream()
            .filter(f -> f.getFeatureType() == featureType)
            .findFirst()
            .map(ServerFeature::isEnabled)
            .orElse(false);
    }

    public void setFeatureEnabled(FeatureType featureType, boolean enabled) {
        features.stream()
            .filter(f -> f.getFeatureType() == featureType)
            .findFirst()
            .ifPresentOrElse(
                feature -> feature.setEnabled(enabled),
                () -> {
                    ServerFeature newFeature = new ServerFeature();
                    newFeature.setServer(this);
                    newFeature.setFeatureType(featureType);
                    newFeature.setEnabled(enabled);
                    features.add(newFeature);
                }
            );
    }

    @Override
    public String toString() {
        return "DiscordServer{" +
            "serverId='" + serverId + '\'' +
            ", serverName='" + serverName + '\'' +
            ", active=" + active +
            '}';
    }
}
