package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.dto.TextChannelDTO;
import followarcane.wow_lfg_discord_bot.application.request.DiscordChannelRequest;
import followarcane.wow_lfg_discord_bot.application.request.DiscordServerRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserSettingsRequest;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordChannel;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.model.User;
import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
        userSettings.setLanguage(userSettingsRequest.getLanguages());
        userSettings.setRanks(userSettingsRequest.isWarcraftlogsRanks());
        userSettings.setPlayerInfo(userSettingsRequest.isInformationAboutPlayer());
        userSettings.setProgress(userSettingsRequest.isRecentRaidProgression());
        userSettings.setFaction(userSettingsRequest.isFaction());
        return userSettings;
    }

    public UserRequest convertToUserRequest(User user) {
        UserRequest userRequest = new UserRequest();
        userRequest.setDiscordId(user.getDiscordId());
        userRequest.setUsername(user.getUsername());
        userRequest.setGlobalName(user.getGlobalName());
        userRequest.setDiscriminator(user.getDiscriminator());
        userRequest.setAvatar(user.getAvatar());
        userRequest.setBanner(user.getBanner());
        userRequest.setBannerColor(user.getBannerColor());
        userRequest.setLocale(user.getLocale());
        return userRequest;
    }

    public UserSettingsRequest convertToUserSettingsDTO(UserSettings userSettings) {
        UserSettingsRequest userSettingsRequest = new UserSettingsRequest();
        userSettingsRequest.setRealm(userSettings.getRealm());
        userSettingsRequest.setRegion(userSettings.getRegion());
        userSettingsRequest.setLanguages(userSettings.getLanguage());
        userSettingsRequest.setChannelId(userSettings.getChannel().getChannelId());
        userSettingsRequest.setWarcraftlogsRanks(userSettings.isRanks());
        userSettingsRequest.setInformationAboutPlayer(userSettings.isPlayerInfo());
        userSettingsRequest.setRecentRaidProgression(userSettings.isProgress());
        userSettingsRequest.setFaction(userSettings.isFaction());
        return userSettingsRequest;
    }

    public DiscordServer convertToDiscordServerRequest(DiscordServer discordServer) {
        DiscordServerRequest discordServerRequest = new DiscordServerRequest();
        discordServerRequest.setServerId(discordServer.getServerId());
        discordServerRequest.setServerName(discordServer.getServerName());
        discordServerRequest.setOwnerId(discordServer.getOwnerId());
        discordServerRequest.setIcon(discordServer.getIcon());
        return discordServer;
    }

    public List<TextChannelDTO> convertToDTO(List<TextChannel> channels) {
        return channels.stream()
                .map(channel -> new TextChannelDTO(channel.getId(), channel.getName()))
                .collect(Collectors.toList());
    }
}
