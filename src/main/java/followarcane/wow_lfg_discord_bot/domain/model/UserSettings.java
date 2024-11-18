package followarcane.wow_lfg_discord_bot.domain.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_settings")
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String language;
    private String realm;
    private String region;
    private boolean faction;
    private boolean progress;
    private boolean ranks;
    private boolean playerInfo;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "server_id")
    private DiscordServer server;

    @ManyToOne
    @JoinColumn(name = "channel_id")
    private DiscordChannel channel;
}

