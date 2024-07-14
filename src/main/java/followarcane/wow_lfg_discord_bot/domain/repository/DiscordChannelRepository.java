package followarcane.wow_lfg_discord_bot.domain.repository;

import followarcane.wow_lfg_discord_bot.domain.model.DiscordChannel;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscordChannelRepository extends JpaRepository<DiscordChannel, Long> {
    boolean existsByServer(DiscordServer discordServer);
    DiscordChannel findDiscordChannelByChannelId(String channelId);
}