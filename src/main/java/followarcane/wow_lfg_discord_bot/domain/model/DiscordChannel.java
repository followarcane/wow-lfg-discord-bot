package followarcane.wow_lfg_discord_bot.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "last_sent_characters", joinColumns = @JoinColumn(name = "channel_id"))
    @Column(name = "character_name")
    private List<String> lastSentCharacters = new ArrayList<>();
}
