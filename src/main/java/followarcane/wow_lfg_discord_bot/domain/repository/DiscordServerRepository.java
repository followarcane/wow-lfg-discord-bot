package followarcane.wow_lfg_discord_bot.domain.repository;

import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscordServerRepository extends JpaRepository<DiscordServer, Long> {
    boolean existsByServerId(String serverId);
    DiscordServer findServerByServerId(String serverId);
    List<DiscordServer> findByUserAndActiveTrue(User user);

}