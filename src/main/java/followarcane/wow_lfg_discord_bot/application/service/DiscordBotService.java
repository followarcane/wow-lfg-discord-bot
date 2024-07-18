package followarcane.wow_lfg_discord_bot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.model.Message;
import followarcane.wow_lfg_discord_bot.domain.model.User;
import followarcane.wow_lfg_discord_bot.domain.repository.DiscordServerRepository;
import followarcane.wow_lfg_discord_bot.domain.repository.MessageRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class DiscordBotService extends ListenerAdapter {
    private final MessageRepository messageRepository;
    private final DiscordServerRepository discordServerRepository;

    private JDA jda;
    @Getter
    private boolean botAlreadyLoggedIn = false;

    @Value("${discord.bot.token}")
    private String token;

    @Value("${discord.client.id}")
    private String clientId;

    @Value("${discord.client.secret}")
    private String clientSecret;

    @Value("${discord.redirect.uriCallback}")
    private String redirectUriCallback;

    @Value("${discord.redirect.uriDashboard}")
    private String redirectUriDashboard;

    private final DiscordService discordService;

    public DiscordBotService(MessageRepository messageRepository, DiscordServerRepository discordServerRepository, DiscordService discordService) {
        this.messageRepository = messageRepository;
        this.discordServerRepository = discordServerRepository;
        this.discordService = discordService;
    }

    @PostConstruct
    public void startBot() throws LoginException {
        try {
            if (token == null || token.isEmpty()) {
                throw new LoginException("Discord bot token is not provided!");
            }
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .setActivity(Activity.playing("WoW LFG"))
                    .build();
            botAlreadyLoggedIn = true;
        } catch (LoginException e) {
            throw new LoginException();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        // Send a welcome message
        if (event.getGuild().getDefaultChannel() != null) {
            //event.getGuild().getDefaultChannel().sen("Thanks for adding WoW LFG Bot!").queue();
        }
    }

    public void addGuildToRepository(String serverId, String serverName, String ownerId, User user) {
        if (!discordServerRepository.existsByServerId(serverId)) {
            DiscordServer discordServer = new DiscordServer();
            discordServer.setServerId(serverId);
            discordServer.setServerName(serverName);
            discordServer.setOwnerId(ownerId);
            discordServer.setUser(user);

            discordServerRepository.save(discordServer);
            log.info("New server joined: {}", serverName);
        } else {
            log.info("Server {} already exists in the repository", serverName);
        }
    }


    public void sendEmbedMessageToChannel(String channelId, EmbedBuilder embed) {
        Objects.requireNonNull(jda.getTextChannelById(channelId)).sendMessageEmbeds(embed.build()).queue();

        Message message = new Message();
        message.setMessageGuildId("guildIdHere");
        message.setMessageChannelId(channelId);
        message.setMessageContent(embed.build().getTitle());
        message.setTimestamp(System.currentTimeMillis());

        messageRepository.save(message);
    }

    public String exchangeCodeForToken(String code, boolean isCallback) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://discord.com/api/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&grant_type=authorization_code" +
                    "&code=" + code +
                    "&redirect_uri=" + (isCallback ? redirectUriCallback : redirectUriDashboard);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("Failed to exchange code for token: {}", response.getBody());
                return null;
            }
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            return null;
        }
    }

    public Map<String, Object> getUserDetails(String tokenResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(tokenResponse);
            String accessToken = jsonNode.get("access_token").asText();

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            // Get User Data
            ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                    "https://discord.com/api/users/@me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode userInfo = objectMapper.readTree(userInfoResponse.getBody());

            UserRequest userRequest = new UserRequest();
            userRequest.setDiscordId(userInfo.get("id").asText());
            userRequest.setUsername(userInfo.get("username").asText());
            userRequest.setGlobalName(userInfo.get("global_name").asText());
            userRequest.setDiscriminator(userInfo.get("discriminator").asText());
            userRequest.setAvatar(userInfo.get("avatar").asText(null));
            userRequest.setBanner(userInfo.get("banner").asText(null));
            userRequest.setBannerColor(userInfo.get("banner_color").asText(null));
            userRequest.setLocale(userInfo.get("locale").asText());
            userRequest.setDiscordToken(accessToken);

            Map<String, Object> response = new HashMap<>();
            response.put("user", userRequest);

            return response;
        } catch (Exception e) {
            log.error("Error processing token response", e);
            return null;
        }
    }

    public void handleServerInvite(String tokenResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(tokenResponse);
            String accessToken = jsonNode.get("access_token").asText();

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<String> guildInfoResponse = restTemplate.exchange(
                    "https://discord.com/api/users/@me/guilds", HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode guildInfo = objectMapper.readTree(guildInfoResponse.getBody()).get(0);

            String serverId = guildInfo.get("id").asText();
            String serverName = guildInfo.get("name").asText();
            String ownerId = guildInfo.get("owner_id").asText();

            User user = discordService.getUserByDiscordId(guildInfo.get("owner_id").asText());

            addGuildToRepository(serverId, serverName, ownerId, user);
        } catch (Exception e) {
            log.error("Error processing server invite", e);
        }
    }

}
