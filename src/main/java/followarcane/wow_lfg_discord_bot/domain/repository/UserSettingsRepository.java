package followarcane.wow_lfg_discord_bot.domain.repository;

import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    UserSettings findByServer_ServerIdAndUser_DiscordId(String serverId, String userId);

    // Bir sunucu ID'sine göre tüm kullanıcı ayarlarını bulur
    List<UserSettings> findByServer_ServerId(String serverId);
}
