package followarcane.wow_lfg_discord_bot.domain.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "discord_servers")
public class DiscordServer {
    @Id
    private String serverId;
    private String serverName;
    private String ownerId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
