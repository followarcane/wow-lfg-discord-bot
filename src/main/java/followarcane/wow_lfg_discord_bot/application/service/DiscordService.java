package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.request.*;
import followarcane.wow_lfg_discord_bot.application.response.RecruitmentFilterResponse;
import followarcane.wow_lfg_discord_bot.application.response.ServerFeatureResponse;
import followarcane.wow_lfg_discord_bot.domain.model.*;
import followarcane.wow_lfg_discord_bot.domain.repository.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class DiscordService {

    private final DiscordServerRepository serverRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final DiscordChannelRepository discordChannelRepository;
    private final RequestConverter requestConverter;
    private final RecruitmentFilterService filterService;
    private final ServerFeatureRepository serverFeatureRepository;

    public void addServer(DiscordServerRequest discordServerRequest) {
        DiscordServer discordServer = requestConverter.convertToDiscordServer(discordServerRequest);
        if (serverRepository.existsByServerId(discordServer.getServerId())) {
            throw new RuntimeException("Server already exists");
        }
        serverRepository.save(discordServer);
    }

    public void addChannel(DiscordChannelRequest discordChannelRequest) {
        DiscordServer discordServer = serverRepository.findServerByServerId(discordChannelRequest.getServerId());
        if (discordServer == null) {
            throw new RuntimeException("Server not found");
        }

        if (discordChannelRepository.existsByServer(discordServer)) {
            throw new RuntimeException("A Channel already exists for this server");
        }

        DiscordChannel discordChannel = requestConverter.convertToDiscordChannel(discordChannelRequest);
        discordChannel.setServer(discordServer);
        discordChannelRepository.save(discordChannel);
    }

    public void addUser(UserRequest userRequest) {
        User user = requestConverter.convertToUser(userRequest);

        if (userRepository.existsByDiscordId(user.getDiscordId())) {
            throw new RuntimeException("User already exists");
        }
        userRepository.save(user);
    }

    public void addUserSettings(UserSettingsRequest userSettingsRequest, String userId) {
        userSettingsRequest.setUserDiscordId(userId);

        // Önce aynı sunucu ID'si için herhangi bir ayar var mı kontrol et
        List<UserSettings> existingSettings = userSettingsRepository.findByServer_ServerId(userSettingsRequest.getServerId());

        // Kullanıcı ve sunucu kombinasyonuna göre ayarlar
        UserSettings userSettings = getPureSettingsByServerIdAndUserId(userSettingsRequest.getServerId(), userSettingsRequest.getUserDiscordId());

        // Eğer bu kullanıcıya ait ayar yoksa ama sunucu için başka ayarlar varsa
        if (userSettings == null && !existingSettings.isEmpty()) {
            // Sunucu için ilk ayarı kullan ve sadece kullanıcı ID'sini değiştir
            UserSettings firstSettings = existingSettings.get(0);

            // Mevcut ayarları sil (ilk ayar hariç)
            if (existingSettings.size() > 1) {
                for (int i = 1; i < existingSettings.size(); i++) {
                    userSettingsRepository.delete(existingSettings.get(i));
                }
            }

            // Yeni kullanıcıyı kurulum sahibi yap
            User newUser = userRepository.findUserByDiscordId(userId);
            firstSettings.setUser(newUser);

            // Yeni kullanıcı ayarları ile güncelle
            updateExistingUserSettings(firstSettings, userSettingsRequest);
        } else if (userSettings != null) {
            // Kullanıcının mevcut ayarlarını güncelle
            updateExistingUserSettings(userSettings, userSettingsRequest);

            // Fazladan olan diğer kayıtları sil
            if (existingSettings.size() > 1) {
                for (UserSettings settings : existingSettings) {
                    if (!settings.getId().equals(userSettings.getId())) {
                        userSettingsRepository.delete(settings);
                    }
                }
            }
        } else {
            // Yeni ayar oluştur
            createNewUserSettings(userSettingsRequest);
        }

        // 2. Recruitment Filter güncelleme
        RecruitmentFilterRequest filterRequest = new RecruitmentFilterRequest();
        filterRequest.setClassFilter(userSettingsRequest.getClassFilter());
        filterRequest.setRoleFilter(userSettingsRequest.getRoleFilter());
        filterRequest.setMinIlevel(userSettingsRequest.getMinIlevel());
        filterRequest.setRaidProgress(userSettingsRequest.getRaidProgress());

        filterService.updateFilters(userSettingsRequest.getServerId(), filterRequest);
    }

    private void updateExistingUserSettings(UserSettings userSettings, UserSettingsRequest request) {
        userSettings.setRealm(request.getRealm());
        userSettings.setRegion(request.getRegion());
        userSettings.setLanguage(request.getLanguages());
        userSettings.setPlayerInfo(request.isInformationAboutPlayer());
        userSettings.setRanks(request.isWarcraftlogsRanks());
        userSettings.setFaction(request.isFaction());
        userSettings.setProgress(request.isRecentRaidProgression());

        // Channel güncelleme eklendi
        DiscordChannel channel = userSettings.getChannel();
        if (!channel.getChannelId().equals(request.getChannelId())) {
            channel.setChannelId(request.getChannelId());
            discordChannelRepository.save(channel);
        }

        // Son değişiklik bilgilerini güncelle
        User modifyingUser = userRepository.findUserByDiscordId(request.getUserDiscordId());
        userSettings.setLastModifiedBy(modifyingUser);
        userSettings.setLastModifiedAt(LocalDateTime.now());

        userSettingsRepository.save(userSettings);
    }

    private UserSettings createNewUserSettings(UserSettingsRequest request) {
        DiscordServer discordServer = serverRepository.findServerByServerId(request.getServerId());
        if (discordServer == null) {
            throw new RuntimeException("Server not found");
        }

        DiscordChannel discordChannel = discordChannelRepository.findDiscordChannelByServer_ServerId(request.getServerId());
        if (discordChannel == null) {
            discordChannel = new DiscordChannel();
            discordChannel.setChannelId(request.getChannelId());
            discordChannel.setServer(discordServer);
            discordChannelRepository.save(discordChannel);
        }

        User user = userRepository.findUserByDiscordId(request.getUserDiscordId());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        UserSettings userSettings = requestConverter.convertToUserSettings(request);
        userSettings.setServer(discordServer);
        userSettings.setChannel(discordChannel);
        userSettings.setUser(user);

        // Son değişiklik bilgilerini ekle
        userSettings.setLastModifiedBy(user);
        userSettings.setLastModifiedAt(LocalDateTime.now());

        return userSettingsRepository.save(userSettings);
    }

    public List<UserSettings> getAllUserSettings() {
        return userSettingsRepository.findAll();
    }

    @Transactional
    public void updateLastSentCharacters(DiscordChannel channel, String characterName) {
        DiscordChannel persistentChannel = discordChannelRepository.findById(channel.getId()).orElseThrow(() -> new RuntimeException("Channel not found"));

        LinkedList<String> lastSentCharacters = new LinkedList<>(persistentChannel.getLastSentCharacters());
        if (lastSentCharacters.contains(characterName)) {
            lastSentCharacters.remove(characterName);
        } else if (lastSentCharacters.size() >= 3) {
            lastSentCharacters.removeFirst();
        }
        lastSentCharacters.addLast(characterName);

        persistentChannel.setLastSentCharacters(new ArrayList<>(lastSentCharacters));
        discordChannelRepository.save(persistentChannel);
    }

    public User getUserByDiscordId(String discordId) {
        return userRepository.findUserByDiscordId(discordId);
    }

    public List<DiscordServer> getServersByUserDiscordId(String userDiscordId) {
        User user = getUserByDiscordId(userDiscordId);
        return serverRepository.findByUserAndActiveTrue(user);
    }

    public User findUserByDiscordId(String discordId) {
        return userRepository.findUserByDiscordId(discordId);
    }

    public UserSettingsRequest getSettingsByServerIdAndUserId(String serverId, String userId) {
        UserSettings userSettings = userSettingsRepository.findByServer_ServerIdAndUser_DiscordId(serverId, userId);
        if (userSettings == null) {
            return null;
        }

        UserSettingsRequest dto = requestConverter.convertToUserSettingsDTO(userSettings);


        RecruitmentFilterResponse filters = filterService.getFilters(serverId);
        dto.setClassFilter(filters.getClassFilter());
        dto.setRoleFilter(filters.getRoleFilter());
        dto.setMinIlevel(filters.getMinIlevel());
        dto.setRaidProgress(filters.getRaidProgress());

        return dto;
    }

    public void deActiveGuild(String id) {
        DiscordServer discordServer = serverRepository.findServerByServerIdAndActiveTrue(id);
        discordServer.setActive(false);
        serverRepository.save(discordServer);
    }

    public UserSettings getPureSettingsByServerIdAndUserId(String serverId, String userId) {
        return userSettingsRepository.findByServer_ServerIdAndUser_DiscordId(serverId, userId);
    }

    public DiscordServer getServerByServerId(String serverId) {
        return serverRepository.findServerByServerIdAndActiveTrue(serverId);
    }

    public List<ServerFeatureResponse> getServerFeatures(String serverId) {
        List<ServerFeature> features = serverFeatureRepository.findByServer_ServerId(serverId);
        
        log.info("Found {} features for server {}", features.size(), serverId);
        
        return features.stream()
            .map(feature -> new ServerFeatureResponse(
                feature.getFeatureType(),
                feature.isEnabled()
            ))
            .toList();
    }
}
