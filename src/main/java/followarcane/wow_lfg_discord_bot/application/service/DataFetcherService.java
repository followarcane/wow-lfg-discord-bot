package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.response.BossRankResponse;
import followarcane.wow_lfg_discord_bot.application.response.CharacterInfoResponse;
import followarcane.wow_lfg_discord_bot.application.response.RaidProgressionResponse;
import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import followarcane.wow_lfg_discord_bot.infrastructure.properties.ApiProperties;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataFetcherService {

    private final DiscordBotService discordBotService;
    private final DiscordService discordService;
    private final RecruitmentFilterService filterService;

    private static ClassColorCodeHelper classColorCodeHelper;

    private final RestTemplate restTemplate = new RestTemplate();
    private Set<CharacterInfoResponse> previousData = new HashSet<>();

    @Autowired
    public DataFetcherService(DiscordBotService discordBotService, ApiProperties apiProperties, DiscordService discordService, ClassColorCodeHelper classColorCodeHelper, RecruitmentFilterService filterService) {
        this.discordBotService = discordBotService;
        this.discordService = discordService;
        DataFetcherService.classColorCodeHelper = classColorCodeHelper;
        this.filterService = filterService;

        // Check if username and password are not null or empty
        Assert.notNull(apiProperties.getUsername(), "Username must not be null");
        Assert.notNull(apiProperties.getPassword(), "Password must not be null");

        this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(apiProperties.getUsername(), apiProperties.getPassword()));
    }

    @Scheduled(fixedRate = 60000)
    public void fetchData() {
        log.info("Fetching data from WoW API...");

        try {
            String wowApi = "http://localhost:8080/api/v1/wdc/latest-lfg";
            ResponseEntity<List<CharacterInfoResponse>> response = restTemplate.exchange(
                    wowApi, HttpMethod.GET, null, new ParameterizedTypeReference<List<CharacterInfoResponse>>() {
                    });
            List<CharacterInfoResponse> data = response.getBody();

            if (data != null && !data.isEmpty()) {
                Set<CharacterInfoResponse> newData = new HashSet<>(data);
                newData.removeAll(previousData);

                if (!newData.isEmpty()) {
                    List<UserSettings> allSettings = discordService.getAllUserSettings().stream()
                            .filter(settings -> settings.getChannel().getServer().isActive()) // Check if server is active.
                            .toList();

                    // Her sunucu için sadece bir UserSettings kullanmak için Map oluştur
                    Map<String, UserSettings> serverIdToSettings = new HashMap<>();

                    // Her sunucu için ilk UserSettings'i al
                    for (UserSettings settings : allSettings) {
                        String serverId = settings.getChannel().getServer().getServerId();
                        if (!serverIdToSettings.containsKey(serverId)) {
                            serverIdToSettings.put(serverId, settings);
                        }
                    }

                    // Sadece her sunucu için tekil ayarları kullan
                    List<UserSettings> matchedLfgs = new ArrayList<>(serverIdToSettings.values());
                    
                    for (UserSettings settings : matchedLfgs) {
                        List<CharacterInfoResponse> filteredData = newData.stream()
                                .filter(character -> characterMatchesSettings(character, settings))
                                .toList();

                        for (CharacterInfoResponse character : filteredData) {
                            if (!settings.getChannel().getLastSentCharacters().contains(character.getName())) {
                                sendFilteredMessage(character, settings);
                            }
                        }
                    }
                    previousData = new HashSet<>(data); // Update the previous data
                } else {
                    log.info("No new data found. Skipping message send.");
                }
            } else {
                log.warn("No data fetched or data is empty.");
            }
        } catch (Exception e) {
            log.error("Error fetching data", e);
        }
    }

    private boolean characterMatchesSettings(CharacterInfoResponse character, UserSettings settings) {
        List<String> settingLanguages = Arrays.stream(settings.getLanguage().replaceAll("\\s+", "").toLowerCase().split(","))
                .toList();
        List<String> characterLanguages = Arrays.stream(character.getLanguages().replaceAll("\\s+", "").toLowerCase().split(","))
                .toList();

        boolean languageMatch = settingLanguages.stream().anyMatch(characterLanguages::contains);
        boolean realmMatch = character.getRealm().equalsIgnoreCase(settings.getRealm()) || settings.getRealm().equalsIgnoreCase("All Realms");
        boolean regionMatch = character.getRegion().equalsIgnoreCase(settings.getRegion());

        if (!languageMatch || !realmMatch || !regionMatch) {
            return false;
        }

        Map<String, String> playerInfo = new HashMap<>();
        playerInfo.put("class", character.getRaiderIOData().getClassType());
        playerInfo.put("role", character.getRaiderIOData().getActiveSpecRole());
        playerInfo.put("ilevel", character.getILevel() != null ? character.getILevel() : "0");
        
        String progress = character.getRaidProgressions().stream()
            .findFirst()
            .map(RaidProgressionResponse::getSummary)
            .orElse("0/8N");  // Default değer
            
        playerInfo.put("progress", progress);

        log.info("[PLAYER_INFO] Created player info map: {}", playerInfo);

        return filterService.shouldSendMessage(settings.getChannel().getServer().getServerId(), playerInfo);
    }

    private void sendFilteredMessage(CharacterInfoResponse character, UserSettings settings) {
        try {
            EmbedBuilder embedBuilder = getEmbedBuilder(character, settings);
            
            Map<String, String> playerInfo = new HashMap<>();
            playerInfo.put("class", character.getRaiderIOData().getClassType());
            playerInfo.put("role", character.getRaiderIOData().getActiveSpecRole());
            playerInfo.put("ilevel", character.getILevel() != null ? character.getILevel() : "0");
            
            String progress = character.getRaidProgressions().stream()
                .findFirst()
                .map(raid -> raid.getSummary())
                .orElse("0/8N");  // Default değer
                
            playerInfo.put("progress", progress);

            discordBotService.sendEmbedMessageToChannel(
                settings.getChannel().getChannelId(), 
                embedBuilder,
                playerInfo
            );

            discordService.updateLastSentCharacters(settings.getChannel(), character.getName());
        } catch (Exception e) {
            log.error("[FILTER_ERROR] Error sending filtered message for character {}: {}", 
                character.getName(), e.getMessage(), e);
        }
    }

    private static @NotNull EmbedBuilder getEmbedBuilder(CharacterInfoResponse character, UserSettings settings) {
        String raiderIOLink = "https://raider.io/characters/" + encodeURL(character.getRegion()) + "/" + encodeURL(character.getRealm().replace(' ', '-')) + "/" + encodeURL(character.getName());
        String wowProgressLink = "https://www.wowprogress.com/character/" + encodeURL(character.getRegion()) + "/" + encodeURL(character.getRealm().replace(' ', '-')) + "/" + encodeURL(character.getName());
        String warcraftLogsLink = "https://www.warcraftlogs.com/character/" + encodeURL(character.getRegion()) + "/" + encodeURL(character.getRealm().replace(' ', '-')) + "/" + encodeURL(character.getName());
        String armoryLink = "https://worldofwarcraft.blizzard.com/en-gb/character/" + encodeURL(character.getRegion()) + "/" + encodeURL(character.getRealm().replace(' ', '-')) + "/" + encodeURL(character.getName());

        String title = character.getRaiderIOData().getClassType() == null
                ? character.getName() + " | " + character.getRealm()
                : character.getName() + " | " + character.getRealm() + " | " + character.getRaiderIOData().getClassType() + " | " + character.getRaiderIOData().getActiveSpecRole() + " | " + character.getRaiderIOData().getActiveSpecName();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title, wowProgressLink);
        embedBuilder.addField("Language", character.getLanguages(), true);
        embedBuilder.addField("Item Level", StringUtils.hasText(character.getILevel()) ? character.getILevel() : "No Info", true);

        if (settings.isFaction()) {
            embedBuilder.addField("Faction", StringUtils.hasText(character.getRaiderIOData().getFaction()) ? character.getRaiderIOData().getFaction() : "No Info", true);
        }

        if (settings.isProgress()) {
            StringBuilder progression = new StringBuilder();
            for (var raidProgression : character.getRaidProgressions()) {
                progression.append(raidProgression.getRaidName()).append("\n").append(raidProgression.getSummary()).append("\n\n");
            }

            if (progression.isEmpty()) {
                progression.append("Use the links below to check the progression.");
            }

            embedBuilder.addField("Raid Progression", progression.toString(), false);
        }

        if (settings.isRanks() && character.getWarcraftLogsData().getBestPerformanceAverage() != 0) {
            embedBuilder.addField("WarcraftLogs", "Overall Performance\n" + String.format("%.2f", character.getWarcraftLogsData().getBestPerformanceAverage()) + "\n----------\n" + prepareLogs(character.getBossRanks()), false);
        }

        if (settings.isPlayerInfo()) {
            embedBuilder.addField("Information About Player", character.getCommentary().length() > 1020
                    ? character.getCommentary().substring(0, 1020) + "..."
                    : character.getCommentary(), false);
        }

        embedBuilder.addField("External Links",
                "[Armory]" + "(" + armoryLink + ")" +
                        " | [Raider IO]" + "(" + raiderIOLink + ")" +
                        " | [WowProgress]" + "(" + wowProgressLink + ")" +
                        " | [Warcraftlogs]" + "(" + warcraftLogsLink + ")"
                , false);
        embedBuilder.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");
        embedBuilder.setThumbnail(character.getRaiderIOData().getThumbnailUrl());

        embedBuilder.setColor(Color.decode(classColorCodeHelper.getClassColorCode(character.getRaiderIOData().getClassType())));
        return embedBuilder;
    }

    private static String encodeURL(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String prepareLogs(List<BossRankResponse> responses) {
        return responses.stream()
                .map(response -> response.getEncounterName() + "\n" + String.format("%.2f", response.getRankPercent()))
                .collect(Collectors.joining("\n"));
    }

}
