package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.request.DiscordChannelRequest;
import followarcane.wow_lfg_discord_bot.application.request.DiscordServerRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserSettingsRequest;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordChannel;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.model.User;
import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import followarcane.wow_lfg_discord_bot.domain.repository.DiscordChannelRepository;
import followarcane.wow_lfg_discord_bot.domain.repository.DiscordServerRepository;
import followarcane.wow_lfg_discord_bot.domain.repository.UserRepository;
import followarcane.wow_lfg_discord_bot.domain.repository.UserSettingsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
@AllArgsConstructor
public class DiscordService {

    private final DiscordServerRepository serverRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final DiscordChannelRepository discordChannelRepository;
    private final RequestConverter requestConverter;

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

    public void addUserSettings(UserSettingsRequest userSettingsRequest) {
        UserSettings userSettings = getSettingsByServerIdAndUserId(userSettingsRequest.getServerId(), Long.valueOf(userSettingsRequest.getUserDiscordId()));
        if (userSettings != null) {
            //Update existing settings
        } else {
            DiscordServer discordServer = serverRepository.findServerByServerId(userSettingsRequest.getServerId());
            if (discordServer == null) {
                throw new RuntimeException("Server not found");
            }
            DiscordChannel discordChannel = discordChannelRepository.findDiscordChannelByChannelId(userSettingsRequest.getChannelId());
            if (discordChannel == null) {
                discordChannel = new DiscordChannel();
                discordChannel.setChannelId(userSettingsRequest.getChannelId());
                discordChannel.setServer(discordServer);
                discordChannelRepository.save(discordChannel);
            }
            User user = userRepository.findUserByDiscordId(userSettingsRequest.getUserDiscordId());
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            userSettings = requestConverter.convertToUserSettings(userSettingsRequest);
            userSettings.setServer(discordServer);
            userSettings.setChannel(discordChannel);
            userSettings.setUser(user);

            userSettingsRepository.save(userSettings);
        }
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
        } else if (lastSentCharacters.size() >= 5) {
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
        return serverRepository.findServersByUser(user);
    }

    public User findUserByDiscordId(String discordId) {
        return userRepository.findUserByDiscordId(discordId);
    }

    public UserSettings getSettingsByServerIdAndUserId(String serverId, Long userId) {
        return userSettingsRepository.findByServer_ServerIdAndUser_Id(serverId, userId);
    }
}
