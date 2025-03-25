package followarcane.wow_lfg_discord_bot.domain.repository;

import followarcane.wow_lfg_discord_bot.domain.model.ServerFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerFeatureRepository extends JpaRepository<ServerFeature, Long> {
    List<ServerFeature> findByServer_ServerId(String serverId);
} 