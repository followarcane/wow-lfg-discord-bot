package followarcane.wow_lfg_discord_bot.domain.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String discordId;
    private String username;
    private String globalName;
    private String discriminator;
    private String avatar;
    private String banner;
    private String bannerColor;
    private String locale;
}
