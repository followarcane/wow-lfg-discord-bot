package followarcane.wow_lfg_discord_bot.domain.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "discord_channels")
public class DiscordChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String channelId;
    private String channelName;

    @ManyToOne
    @JoinColumn(name = "server_id")
    private DiscordServer server;
}

