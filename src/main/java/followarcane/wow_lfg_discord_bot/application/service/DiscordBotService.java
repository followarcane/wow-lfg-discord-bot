package followarcane.wow_lfg_discord_bot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.response.StatisticsResponse;
import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import followarcane.wow_lfg_discord_bot.domain.FeatureType;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.model.Message;
import followarcane.wow_lfg_discord_bot.domain.model.ServerFeature;
import followarcane.wow_lfg_discord_bot.domain.model.User;
import followarcane.wow_lfg_discord_bot.domain.repository.DiscordServerRepository;
import followarcane.wow_lfg_discord_bot.domain.repository.MessageRepository;
import followarcane.wow_lfg_discord_bot.security.util.TokenValidationUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class DiscordBotService extends ListenerAdapter {
    private final MessageRepository messageRepository;
    private final DiscordServerRepository discordServerRepository;

    private JDA jda;

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

    private final ClassColorCodeHelper classColorCodeHelper;

    public DiscordBotService(MessageRepository messageRepository, DiscordServerRepository discordServerRepository, DiscordService discordService, RequestConverter requestConverter, RestTemplate restTemplate, RecruitmentFilterService filterService, ClassColorCodeHelper classColorCodeHelper) {
        this.messageRepository = messageRepository;
        this.discordServerRepository = discordServerRepository;
        this.discordService = discordService;
        this.requestConverter = requestConverter;
        this.restTemplate = restTemplate;
        this.classColorCodeHelper = classColorCodeHelper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startBot() {
        try {
            log.info("[DISCORD_START] Starting Discord bot...");
            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Discord bot token is not provided!");
            }
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .setActivity(Activity.customStatus("https://azerite.app"))
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .build();

            registerSlashCommands();
            
            log.info("[DISCORD_SUCCESS] Discord bot started successfully.");
        } catch (Exception e) {
            log.error("[DISCORD_ERROR] Error starting Discord bot: {}", e.getMessage(), e);
        }
    }

    private void registerSlashCommands() {
        // Önce tüm komutları temizle
        jda.updateCommands().queue(success -> {
            log.info("All commands cleared successfully");

            // Sonra yeni komutları ekle
            jda.updateCommands().addCommands(
                Commands.slash("help", "Shows help information about the bot"),
                Commands.slash("setup", "Set up the LFG feature"),
                Commands.slash("example", "Shows an example LFG message"),
                Commands.slash("discord", "Get an invite link to our official Discord"),
                    Commands.slash("weekly-runs", "Shows a player's weekly Mythic+ runs from Raider.io")
                        .addOption(OptionType.STRING, "name", "Character name", true)
                        .addOption(OptionType.STRING, "realm", "Realm name (use dash for spaces, e.g. 'twisting-nether')", true)
                        .addOption(OptionType.STRING, "region", "Region (eu/us/kr/tw)", true)
            ).queue(
                    updated -> log.info("Commands updated successfully"),
                    error -> log.error("Error updating commands: {}", error.getMessage())
            );
        }, error -> log.error("Error clearing commands: {}", error.getMessage()));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        switch (commandName) {
            case "help":
                handleHelpCommand(event);
                break;
            case "setup":
                handleSetupCommand(event);
                break;
            case "example":
                handleExampleCommand(event);
                break;
            case "discord":
                handleDiscordCommand(event);
                break;
            case "weekly-runs":
                handleRaiderIOCommand(event);
                break;
            default:
                event.reply("Unknown command!").setEphemeral(true).queue();
                break;
        }
    }

    private void handleHelpCommand(SlashCommandInteractionEvent event) {
        String helpMessage = "To set up Azerite and start searching for players, please visit https://azerite.app and invite the bot to your channel again.\n" +
                "Then, go to the 'Looking For Player?' configuration in the features section to adjust your settings and join us!\n" +
                "Feel free to ask any question in our official discord.\n\n" +
                "You can get an invite link with the /discord command.";

        event.reply(helpMessage).queue();
    }

    private void handleSetupCommand(SlashCommandInteractionEvent event) {
        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You need administrator permission to use this command!").setEphemeral(true).queue();
            return;
        }

        event.reply("Setting up LFG feature... Please visit https://azerite.app to complete the configuration.").queue();
    }

    private void handleExampleCommand(SlashCommandInteractionEvent event) {
        event.replyEmbeds(sendExampleEmbedMessage().build()).queue();
    }

    private void handleDiscordCommand(SlashCommandInteractionEvent event) {
        event.reply("Join our official Discord server: https://discord.gg/FVR9e3X6xx").queue();
    }

    private void handleRaiderIOCommand(SlashCommandInteractionEvent event) {
        // Defer reply to give us time to fetch data
        event.deferReply().queue();

        String name = event.getOption("name").getAsString();
        String realm = event.getOption("realm").getAsString().replace(" ", "-");
        String region = event.getOption("region").getAsString();

        try {
            String url = String.format("https://raider.io/api/v1/characters/profile?region=%s&realm=%s&name=%s&fields=mythic_plus_scores_by_season%%3Acurrent%%2Cmythic_plus_weekly_highest_level_runs",
                    region, realm, name);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());

                // Create embed message
                EmbedBuilder embed = new EmbedBuilder();

                // Set character info in title
                String characterName = rootNode.get("name").asText();
                String characterClass = rootNode.get("class").asText();
                String characterSpec = rootNode.get("active_spec_name").asText();
                String characterRole = rootNode.get("active_spec_role").asText();
                String characterRealm = rootNode.get("realm").asText();
                String profileUrl = rootNode.get("profile_url").asText();
                String thumbnailUrl = rootNode.get("thumbnail_url").asText();
                String faction = rootNode.get("faction").asText();

                embed.setTitle(characterName + " | " + characterRealm + " | " + characterSpec + " " + characterClass, profileUrl);
                embed.setThumbnail(thumbnailUrl);

                // Set color based on class using existing helper
                embed.setColor(Color.decode(classColorCodeHelper.getClassColorCode(characterClass)));

                // Add current season scores
                JsonNode scoresNode = rootNode.path("mythic_plus_scores_by_season").path(0).path("scores");
                if (!scoresNode.isMissingNode()) {
                    StringBuilder scoreInfo = new StringBuilder();

                    // Add non-zero scores
                    if (scoresNode.has("all") && scoresNode.get("all").asDouble() > 0) {
                        scoreInfo.append("**Overall:** ").append(scoresNode.get("all").asDouble()).append("\n");
                    }
                    if (scoresNode.has("dps") && scoresNode.get("dps").asDouble() > 0) {
                        scoreInfo.append("**DPS:** ").append(scoresNode.get("dps").asDouble()).append("\n");
                    }
                    if (scoresNode.has("healer") && scoresNode.get("healer").asDouble() > 0) {
                        scoreInfo.append("**Healer:** ").append(scoresNode.get("healer").asDouble()).append("\n");
                    }
                    if (scoresNode.has("tank") && scoresNode.get("tank").asDouble() > 0) {
                        scoreInfo.append("**Tank:** ").append(scoresNode.get("tank").asDouble()).append("\n");
                    }

                    if (scoreInfo.length() > 0) {
                        embed.addField("Current Season Scores", scoreInfo.toString(), false);
                    }
                }
                
                // Add weekly runs
                JsonNode runsNode = rootNode.get("mythic_plus_weekly_highest_level_runs");
                if (runsNode.isArray() && runsNode.size() > 0) {
                    StringBuilder runsInfo = new StringBuilder();

                    for (JsonNode run : runsNode) {
                        String dungeon = run.get("dungeon").asText();
                        int level = run.get("mythic_level").asInt();
                        int upgrades = run.get("num_keystone_upgrades").asInt();
                        String completedAt = run.get("completed_at").asText().substring(0, 10);
                        double score = run.get("score").asDouble();

                        String upgradeStars = "";
                        for (int i = 0; i < upgrades; i++) {
                            upgradeStars += "⭐";
                        }

                        runsInfo.append("**").append(dungeon).append("** +").append(level)
                                .append(" (").append(upgradeStars).append(")")
                                .append(" - Score: ").append(score)
                                .append(" - ").append(completedAt)
                                .append("\n\n");
                    }

                    embed.addField("Weekly Mythic+ Runs", runsInfo.toString(), false);
                } else {
                    embed.addField("Weekly Mythic+ Runs", "No runs found for this week", false);
                }

                // Add footer - same as LFG message
                embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");
                
                // Send the embed
                event.getHook().sendMessageEmbeds(embed.build()).queue();

            } else {
                event.getHook().sendMessage("Could not find character: " + name + " on " + realm + "-" + region.toUpperCase() + 
                        ". Please check the spelling and try again.").queue();
            }
        } catch (Exception e) {
            log.error("Error fetching Raider.io data: {}", e.getMessage(), e);
            event.getHook().sendMessage("Error fetching data from Raider.io. Please try again later.").queue();
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

            // Yeni server için default feature'ları ekle
            ServerFeature lfgFeature = new ServerFeature(discordServer, FeatureType.LFG);
            discordServer.getFeatures().add(lfgFeature);

            log.info("New server joined: {}", serverName);
        } else {
            log.info("Server {} already exists in the repository. Changing status to Active", serverName);
            discordServer.setIcon(icon);
            discordServer.setActive(true);
        }
        return discordServerRepository.save(discordServer);
    }

    public List<TextChannel> getGuildChannelList(String guildId) {
        return Objects.requireNonNull(jda.getGuildById(guildId)).getTextChannels();
    }

    public void sendEmbedMessageToChannel(String channelId, EmbedBuilder embed, Map<String, String> playerInfo) {
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                log.error("[DISCORD_ERROR] Channel not found: {}", channelId);
                return;
            }

            if (!channel.canTalk()) {
                log.error("[DISCORD_ERROR] Bot doesn't have permission...");
                return;
            }

            channel.sendMessageEmbeds(embed.build()).queue(
                success -> log.info("[DISCORD_SUCCESS] Message sent. ChannelID: {}, GuildID: {}", 
                    channelId, 
                    channel.getGuild().getId()),
                error -> log.error("[DISCORD_ERROR] Failed to send message. ChannelID: {}, Error: {}", 
                    channelId, 
                    error.getMessage())
            );

            Message message = new Message();
            message.setMessageGuildId(channel.getGuild().getId());
            message.setMessageChannelId(channelId);
            message.setMessageContent(embed.build().getTitle());
            message.setTimestamp(System.currentTimeMillis());

            messageRepository.save(message);

        } catch (Exception e) {
            log.error("[DISCORD_ERROR] Error sending message", e);
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
        int maxRetries = 3;
        int currentTry = 0;
        long retryDelay = 500; // 500ms başlangıç delay

        while (currentTry < maxRetries) {
            try {
                Thread.sleep(retryDelay);  // Her denemeden önce bekle
                
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
                        responses.put("enabledFeatures", matchedGuild.get().get("enabledFeatures") == null ? 
                            new ArrayList<>() : matchedGuild.get().get("enabledFeatures"));
                        return responses;
                    }
                    return null;
                }
            } catch (Exception e) {
                if (e.getMessage().contains("429 Too Many Requests")) {
                    currentTry++;
                    retryDelay *= 2;  // Exponential backoff
                    log.warn("Rate limited, retry {} of {}, waiting {}ms", currentTry, maxRetries, retryDelay);
                    continue;
                }
                log.error("Error getting guild details: {}", e.getMessage());
                return null;
            }
        }
        
        log.error("Failed to get guild details after {} retries", maxRetries);
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


    private static @NotNull EmbedBuilder sendExampleEmbedMessage() {
        String raiderIOLink = ("https://raider.io/characters/eu/twisting-nether/Shadlynn");
        String wowProgressLink = ("https://www.wowprogress.com/character/eu/twisting-nether/Shadlynn");
        String warcraftLogsLink = ("https://www.warcraftlogs.com/character/eu/twisting-nether/shadlynn?zone=29#zone=26");
        String armoryLink = ("https://worldofwarcraft.blizzard.com/en-gb/character/eu/twisting-nether/shadlynn");

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Shadlynn | Twisting Nether | Priest | Dps | Shadow", raiderIOLink);
        embedBuilder.addField("Language", "Turkish, English", true);
        embedBuilder.addField("Item Level", "560", true);
        embedBuilder.addField("Faction", "Horde", true);

        embedBuilder.addField("Awakened Amirdrassil The Dreams Hope", "9/9 Mythic", false);
        embedBuilder.addField("Amirdrassil The Dreams Hope", "9/9 Mythic", false);


        embedBuilder.addField("Information About Player", "Azerite Bot is designed to simplify and enhance the guild recruitment process for World of Warcraft players by providing integrated data from popular WoW community sites.\n\nPlease review the customizable features and automatic sharing functionalities that set Azerite Bot apart in terms of utility and efficiency.", false);
        embedBuilder.addField("External Links",
                "[Armory]" + "(" + armoryLink + ")" +
                        " | [Raider IO]" + "(" + raiderIOLink + ")" +
                        " | [WowProgress]" + "(" + wowProgressLink + ")" +
                        " | [Warcraftlogs]" + "(" + warcraftLogsLink + ")"
                , false);
        //embedBuilder.setFooter("Donate -> https://www.patreon.com/Shadlynn/membership", "https://imgur.com/kk6VClj.png");
        embedBuilder.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");
        embedBuilder.setThumbnail("https://render.worldofwarcraft.com/eu/character/tw…atar.jpg?alt=/wow/static/images/2d/avatar/9-1.jpg");

        embedBuilder.setColor(Color.white);
        return embedBuilder;
    }
}
