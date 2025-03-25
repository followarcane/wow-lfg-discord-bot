package followarcane.wow_lfg_discord_bot.domain.model;

import followarcane.wow_lfg_discord_bot.domain.FeatureType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "server_features")
public class ServerFeature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "server_id", referencedColumnName = "server_id")
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
} 