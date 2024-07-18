package followarcane.wow_lfg_discord_bot.application;

import followarcane.wow_lfg_discord_bot.application.request.DiscordChannelRequest;
import followarcane.wow_lfg_discord_bot.application.request.DiscordServerRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserSettingsRequest;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordChannel;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.model.User;
import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import org.springframework.stereotype.Service;

@Service
public class RequestConverter {

    public DiscordServer convertToDiscordServer(DiscordServerRequest discordServerRequest) {
        DiscordServer discordServer = new DiscordServer();
        discordServer.setServerId(discordServerRequest.getServerId());
        discordServer.setServerName(discordServerRequest.getServerName());
        discordServer.setOwnerId(discordServerRequest.getOwnerId());
        return discordServer;
    }

    public DiscordChannel convertToDiscordChannel(DiscordChannelRequest discordChannelRequest) {
        DiscordChannel discordChannel = new DiscordChannel();
        discordChannel.setChannelId(discordChannelRequest.getChannelId());
        discordChannel.setChannelName(discordChannelRequest.getChannelName());
        return discordChannel;
    }

    public User convertToUser(UserRequest userRequest) {
        User user = new User();
        user.setDiscordId(userRequest.getDiscordId());
        user.setUsername(userRequest.getUsername());
        user.setGlobalName(userRequest.getGlobalName());
        user.setDiscriminator(userRequest.getDiscriminator());
        user.setAvatar(userRequest.getAvatar());
        user.setBanner(userRequest.getBanner());
        user.setBannerColor(userRequest.getBannerColor());
        user.setLocale(userRequest.getLocale());
        return user;
    }

    public UserSettings convertToUserSettings(UserSettingsRequest userSettingsRequest) {
        UserSettings userSettings = new UserSettings();
        userSettings.setRealm(userSettingsRequest.getRealm());
        userSettings.setRegion(userSettingsRequest.getRegion());
        userSettings.setLanguage(userSettingsRequest.getLanguage());
        return userSettings;
    }
}
