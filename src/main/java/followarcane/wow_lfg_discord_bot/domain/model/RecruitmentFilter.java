package followarcane.wow_lfg_discord_bot.domain.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "recruitment_filters")
public class RecruitmentFilter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "server_id")
    private DiscordServer server;

    @Column(nullable = false)
    private String classFilter = "ANY";

    @Column(nullable = false)
    private String roleFilter = "ANY";

    private Integer minIlevel;

    @Column(nullable = false)
    private String raidProgress = "ANY";
} 