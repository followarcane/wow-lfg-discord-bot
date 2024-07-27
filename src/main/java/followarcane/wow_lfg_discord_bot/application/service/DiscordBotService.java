package followarcane.wow_lfg_discord_bot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.response.StatisticsResponse;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.model.Message;
import followarcane.wow_lfg_discord_bot.domain.model.User;
import followarcane.wow_lfg_discord_bot.domain.repository.DiscordServerRepository;
import followarcane.wow_lfg_discord_bot.domain.repository.MessageRepository;
import followarcane.wow_lfg_discord_bot.security.util.TokenValidationUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.StreamSupport;

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

    @Value("${discord.redirect.uriInvite}")
    private String redirectUriInvite;

    private final DiscordService discordService;

    private final RequestConverter requestConverter;

    private final RestTemplate restTemplate;

    public DiscordBotService(MessageRepository messageRepository, DiscordServerRepository discordServerRepository, DiscordService discordService, RequestConverter requestConverter, RestTemplate restTemplate) {
        this.messageRepository = messageRepository;
        this.discordServerRepository = discordServerRepository;
        this.discordService = discordService;
        this.requestConverter = requestConverter;
        this.restTemplate = restTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startBot() {
        try {
            log.info("Starting Discord bot...");
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Discord bot token is not provided!");
            }
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .setActivity(Activity.customStatus("https://azerite.app"))
                    .build();
            botAlreadyLoggedIn = true;
            log.info("Discord bot started successfully.");
        } catch (Exception e) {
            log.error("Error starting Discord bot", e);
        }
    }


    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        // Send a welcome message
        if (event.getGuild().getDefaultChannel() != null) {
            //event.getGuild().getDefaultChannel().sen("Thanks for adding WoW LFG Bot!").queue();
        }
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        discordService.deActiveGuild(event.getGuild().getId());
    }

    public DiscordServer addGuildToRepository(String serverId, String serverName, String ownerId, User user, String systemChannelId, String prefix, String icon, String banner, String description) {
        DiscordServer discordServer = discordServerRepository.findServerByServerId(serverId);
        if (discordServer == null) {
            discordServer = new DiscordServer();
            discordServer.setServerId(serverId);
            discordServer.setServerName(serverName);
            discordServer.setOwnerId(ownerId);
            discordServer.setUser(user);
            discordServer.setSystemChannelId(systemChannelId);
            discordServer.setPrefix(prefix);
            discordServer.setIcon(icon);
            discordServer.setBanner(banner);
            discordServer.setDescription(description);

            log.info("New server joined: {}", serverName);
        } else {
            log.info("Server {} already exists in the repository. Changing status to Active", serverName);
            discordServer.setIcon(icon);
            discordServer.setActive(true);
        }
        return discordServerRepository.save(discordServer);
    }

    public List<TextChannel> getGuildChannelList(String guildId){
        List<TextChannel> channels = jda.getGuildById(guildId).getTextChannels();
        return channels;
    }

    public void sendEmbedMessageToChannel(String channelId, EmbedBuilder embed) {
        try {
            log.info("Sending message to channel: {}", channelId);
            Objects.requireNonNull(jda.getTextChannelById(channelId)).sendMessageEmbeds(embed.build()).queue();
            log.info("Message sent");
            Message message = new Message();
            message.setMessageGuildId("guildIdHere");
            message.setMessageChannelId(channelId);
            message.setMessageContent(embed.build().getTitle());
            message.setTimestamp(System.currentTimeMillis());

            messageRepository.save(message);
        } catch (RuntimeException e) {
            log.error("Error sending message to channel", e);
        }

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
                    "&redirect_uri=" + (isCallback ? redirectUriCallback : redirectUriInvite);

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

    public HashMap<String, Object> getUserDetails(String tokenResponse) {
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


            HashMap<String, Object> loginDetails = new HashMap<>();
            loginDetails.put("access_token", jsonNode.get("access_token").asText());
            loginDetails.put("refresh_token", jsonNode.get("refresh_token").asText());
            loginDetails.put("scope", jsonNode.get("scope").asText());
            loginDetails.put("token_type", jsonNode.get("token_type").asText());
            loginDetails.put("expires_in", 604800);

            UserRequest userRequest = new UserRequest();
            userRequest.setDiscordId(userInfo.get("id").asText());
            userRequest.setUsername(userInfo.get("username").asText());
            userRequest.setGlobalName(userInfo.get("global_name").asText());
            userRequest.setDiscriminator(userInfo.get("discriminator").asText());
            userRequest.setAvatar(userInfo.get("avatar").asText());
            userRequest.setBanner(userInfo.get("banner").asText());
            userRequest.setBannerColor(userInfo.get("banner_color").asText());
            userRequest.setLocale(userInfo.get("locale").asText());
            userRequest.setAccessToken(accessToken);
            userRequest.setRefreshToken(jsonNode.get("refresh_token").asText());

            User user = discordService.findUserByDiscordId(userRequest.getDiscordId());
            if (user != null) {
                log.info("User already exists in the repository");
                userRequest = requestConverter.convertToUserRequest(user);
                userRequest.setRefreshToken(jsonNode.get("refresh_token").asText());
                userRequest.setAccessToken(accessToken);
                return loginDetails;
            }
            discordService.addUser(userRequest);
            return loginDetails;
        } catch (Exception e) {
            log.error("Error processing token response", e);
            return null;
        }
    }

    public Map<String, Object> handleServerInvite(String tokenResponse) {
        Map<String, Object> responseMap = new HashMap<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(tokenResponse);
            if (jsonNode.get("guild") != null) {
                String discordId = jsonNode.get("guild").get("id").asText();
                String serverName = jsonNode.get("guild").get("name").asText();
                String ownerId = jsonNode.get("guild").get("owner_id").asText();
                String systemChannelId = jsonNode.get("guild").get("system_channel_id").asText();
                String prefix = "!";
                String icon = "https://cdn.discordapp.com/icons/" + discordId + "/guild_icon.png";
                String banner = jsonNode.get("guild").get("banner").asText();
                String description = jsonNode.get("guild").get("description").asText();

                User user = discordService.getUserByDiscordId(ownerId);
                DiscordServer server = addGuildToRepository(discordId, serverName, ownerId, user, systemChannelId, prefix, icon, banner, description);

                responseMap.put("user", user);
                responseMap.put("server", server);
                responseMap.put("tokenResponse", tokenResponse);
                return responseMap;
            }
        } catch (Exception e) {
            log.error("Error processing server invite", e);
        }
        return null;
    }

    public Map<String, Object> getGuildDetails(String token, String guildId) {
        try {
            Thread.sleep(500);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://discord.com/api/v9/users/@me/guilds",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode guildsArray = objectMapper.readTree(response.getBody());

                Optional<JsonNode> matchedGuild = StreamSupport.stream(guildsArray.spliterator(), false)
                        .filter(guildNode -> guildId.equals(guildNode.get("id").asText()))
                        .findFirst();

                DiscordServer discordServer = discordService.getServerByServerId(guildId);
                Map<String, Object> responses = new HashMap<>();

                if (matchedGuild.isPresent() && discordServer != null) {
                    responses.put("id", matchedGuild.get().get("id").asText());
                    responses.put("name", matchedGuild.get().get("name").asText());
                    responses.put("icon", matchedGuild.get().get("icon").asText());
                    responses.put("banner", matchedGuild.get().get("banner").asText());
                    responses.put("owner", matchedGuild.get().get("owner"));
                    responses.put("permissions", matchedGuild.get().get("permissions").asText());
                    responses.put("features", matchedGuild.get().get("features"));
                    responses.put("enabledFeatures", matchedGuild.get().get("enabledFeatures") == null ? new ArrayList<>() : matchedGuild.get().get("enabledFeatures"));
                    return responses;
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error getting guild details: " + e.getMessage());
        }
        return null;
    }

    public ResponseEntity<String> validateAndGetUserId(String token) {
        String userId = TokenValidationUtils.validateAccessToken(token.substring(7), restTemplate);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        return new ResponseEntity<>(userId, HttpStatus.OK);
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        StatisticsResponse statisticsResponse = new StatisticsResponse();
        statisticsResponse.setTotalServers(String.valueOf(discordServerRepository.count()));

        Long lastMessageId = messageRepository.findMaxId();
        statisticsResponse.setTotalMessages(lastMessageId != null ? String.valueOf(lastMessageId) : "0");

        return statisticsResponse;
    }
}
