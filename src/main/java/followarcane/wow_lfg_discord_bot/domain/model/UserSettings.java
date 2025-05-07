package followarcane.wow_lfg_discord_bot.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"server_id"}, name = "uk_user_settings_server_id")
})
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

    // Son değişikliği yapan kullanıcı
    @ManyToOne
    @JoinColumn(name = "last_modified_by")
    private User lastModifiedBy;

    // Son değişiklik zamanı
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
}

