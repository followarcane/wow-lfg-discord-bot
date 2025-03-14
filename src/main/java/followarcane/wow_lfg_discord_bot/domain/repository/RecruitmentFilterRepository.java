package followarcane.wow_lfg_discord_bot.domain.repository;

import followarcane.wow_lfg_discord_bot.domain.model.RecruitmentFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecruitmentFilterRepository extends JpaRepository<RecruitmentFilter, Long> {
    Optional<RecruitmentFilter> findByServer_ServerId(String serverId);
} 