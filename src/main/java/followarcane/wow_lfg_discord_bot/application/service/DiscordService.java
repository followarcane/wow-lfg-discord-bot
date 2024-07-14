package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.RequestConverter;
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
        DiscordServer discordServer = serverRepository.findServerByServerId(userSettingsRequest.getServerId());
        if (discordServer == null) {
            throw new RuntimeException("Server not found");
        }
        DiscordChannel discordChannel = discordChannelRepository.findDiscordChannelByChannelId(userSettingsRequest.getChannelId());
        if (discordChannel == null) {
            throw new RuntimeException("Channel not found");
        }
        User user = userRepository.findUserByDiscordId(userSettingsRequest.getUserDiscordId());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        UserSettings userSettings = requestConverter.convertToUserSettings(userSettingsRequest);
        userSettings.setServer(discordServer);
        userSettings.setChannel(discordChannel);
        userSettings.setUser(user);

        userSettingsRepository.save(userSettings);
    }

    public List<UserSettings> getAllUserSettings() {
        return userSettingsRepository.findAll();
    }
}
