package followarcane.wow_lfg_discord_bot.domain.repository;

import followarcane.wow_lfg_discord_bot.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByDiscordId(String discordId);
    User findUserByDiscordId(String discordId);
}
