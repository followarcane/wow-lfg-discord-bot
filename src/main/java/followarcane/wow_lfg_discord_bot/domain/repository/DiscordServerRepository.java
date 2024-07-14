package followarcane.wow_lfg_discord_bot.domain.repository;

import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscordServerRepository extends JpaRepository<DiscordServer, Long> {
    boolean existsByServerId(String serverId);
    DiscordServer findServerByServerId(String serverId);
}