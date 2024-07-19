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
    private String systemChannelId;
    private String prefix;
    private String icon;
    private String banner;
    private String description;
    boolean active = true;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
