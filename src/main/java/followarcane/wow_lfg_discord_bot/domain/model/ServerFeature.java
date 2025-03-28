package followarcane.wow_lfg_discord_bot.domain.model;

import followarcane.wow_lfg_discord_bot.domain.FeatureType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@Table(name = "server_features")
public class ServerFeature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "server_id", referencedColumnName = "server_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DiscordServer server;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeatureType featureType;

    private boolean enabled = false;

    public ServerFeature() {
    }

    public ServerFeature(DiscordServer server, FeatureType featureType) {
        this.server = server;
        this.featureType = featureType;
        this.enabled = featureType == FeatureType.LFG; // LFG için default true
    }
    
    // toString metodunu override edelim
    @Override
    public String toString() {
        return "ServerFeature{" +
            "id=" + id +
            ", featureType=" + featureType +
            ", enabled=" + enabled +
            '}';
    }
} 