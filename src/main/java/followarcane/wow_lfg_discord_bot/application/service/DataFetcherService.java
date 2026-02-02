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

    @Scheduled(fixedRate = 60000)
    public void fetchData() {
        log.info("[LFG_FETCH] Fetching data from WoW API...");

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
                    String newCharacterNames = newData.stream()
                            .map(c -> c.getName() + " (" + c.getRealm() + " " + c.getRegion() + ")")
                            .collect(Collectors.joining(", "));
                    log.info("[LFG_FETCH] New characters since last run: {} | count={}", newCharacterNames, newData.size());

                    List<UserSettings> allSettings = discordService.getAllUserSettings().stream()
                            .filter(settings -> settings.getChannel().getServer().isActive())
                            .toList();

                    Map<String, UserSettings> serverIdToSettings = new HashMap<>();
                    for (UserSettings settings : allSettings) {
                        String serverId = settings.getChannel().getServer().getServerId();
                        if (!serverIdToSettings.containsKey(serverId)) {
                            serverIdToSettings.put(serverId, settings);
                        }
                    }

                    List<UserSettings> matchedLfgs = new ArrayList<>(serverIdToSettings.values());
                    String serverNamesList = matchedLfgs.stream()
                            .map(s -> s.getChannel().getServer().getServerName())
                            .collect(Collectors.joining(", "));
                    log.info("[LFG_FETCH] Servers to process (active, with LFG channel): {}", serverNamesList);

                    int totalMessagesSent = 0;
                    List<String> charactersWithZeroPosts = new ArrayList<>();

                    for (CharacterInfoResponse character : newData) {
                        String charKey = character.getName() + "|" + character.getRealm() + "|" + character.getRegion();
                        List<String> postedServers = new ArrayList<>();
                        List<String> skippedServers = new ArrayList<>();
                        Map<MatchOutcome, List<String>> noMatchByReason = new EnumMap<>(MatchOutcome.class);
                        for (MatchOutcome o : MatchOutcome.values()) {
                            if (o != MatchOutcome.MATCH) noMatchByReason.put(o, new ArrayList<>());
                        }

                        for (UserSettings settings : matchedLfgs) {
                            String serverName = settings.getChannel().getServer().getServerName();
                            MatchOutcome outcome = characterMatchesSettings(character, settings, charKey);
                            if (outcome == MatchOutcome.MATCH) {
                                if (settings.getChannel().getLastSentCharacters().contains(character.getName())) {
                                    skippedServers.add(serverName);
                                    log.debug("[LFG_SKIPPED] character={} -> server=\"{}\" reason=already_in_lastSent list={}", charKey, serverName, settings.getChannel().getLastSentCharacters());
                                } else {
                                    postedServers.add(serverName);
                                    String channelId = settings.getChannel().getChannelId();
                                    log.info("[LFG_POSTED] character={} -> server=\"{}\" channel={}", charKey, serverName, channelId);
                                    sendFilteredMessage(character, settings);
                                }
                            } else {
                                noMatchByReason.get(outcome).add(serverName);
                            }
                        }

                        totalMessagesSent += postedServers.size();
                        if (postedServers.isEmpty()) {
                            charactersWithZeroPosts.add(charKey);
                        }

                        logCharacterSummary(charKey, postedServers, skippedServers, noMatchByReason);
                    }

                    log.info("[LFG_FETCH] Run complete. New characters: {}. Total messages sent: {}. Characters with 0 posts: {}", newData.size(), totalMessagesSent, charactersWithZeroPosts.isEmpty() ? "none" : String.join(", ", charactersWithZeroPosts));
                    previousData = new HashSet<>(data);
                } else {
                    log.info("[LFG_FETCH] No new data (total characters from API: {}). Characters are only processed when they first appear in the API or right after bot restart.", data.size());
                }
            } else {
                log.warn("[LFG_FETCH] No data fetched or data is empty.");
            }
        } catch (Exception e) {
            log.error("[LFG_FETCH] Error fetching data", e);
        }
    }

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

    /**
     * Per-character summary: which character was posted where, where not, and why.
     */
    private void logCharacterSummary(String charKey, List<String> postedServers, List<String> skippedServers, Map<MatchOutcome, List<String>> noMatchByReason) {
        int totalNoMatch = noMatchByReason.values().stream().mapToInt(List::size).sum();
        log.info("[LFG_SUMMARY] ---------- Character: {} ----------", charKey);
        log.info("[LFG_SUMMARY]   POSTED (message sent): {} servers {}", postedServers.size(), postedServers.isEmpty() ? "" : "-> " + postedServers);
        log.info("[LFG_SUMMARY]   SKIPPED (already sent recently): {} servers {}", skippedServers.size(), skippedServers.isEmpty() ? "" : "-> " + skippedServers);
        log.info("[LFG_SUMMARY]   NOT POSTED (no match): {} servers", totalNoMatch);
        noMatchByReason.forEach((reason, servers) -> {
            if (!servers.isEmpty()) {
                log.info("[LFG_SUMMARY]     - {}: {}", reason, servers);
            }
        });
    }

    /**
     * Returns match outcome; if rejected, which step (language/realm/region/filter). No per-server log here, aggregated in summary.
     */
    private MatchOutcome characterMatchesSettings(CharacterInfoResponse character, UserSettings settings, String charKey) {
        String settingsLanguageRaw = settings.getLanguage() != null ? settings.getLanguage() : "";
        List<String> settingLanguages = Arrays.stream(settingsLanguageRaw.replaceAll("\\s+", "").toLowerCase().split(","))
                .filter(s -> !s.isEmpty())
                .toList();

        String charLanguagesRaw = character.getLanguages() != null ? character.getLanguages().trim() : "";
        List<String> characterLanguages = charLanguagesRaw.isEmpty()
                ? List.of()
                : Arrays.stream(charLanguagesRaw.replaceAll("\\s+", "").toLowerCase().split(","))
                .filter(s -> !s.isEmpty())
                .toList();

        boolean languageMatch = !characterLanguages.isEmpty()
                && settingLanguages.stream().anyMatch(characterLanguages::contains);
        if (!languageMatch) return MatchOutcome.NO_LANGUAGE;

        String charRealm = character.getRealm() != null ? character.getRealm().trim() : "";
        String settingRealm = settings.getRealm() != null ? settings.getRealm().trim() : "";
        boolean realmMatch = charRealm.equalsIgnoreCase(settingRealm) || "All Realms".equalsIgnoreCase(settingRealm);
        if (!realmMatch) return MatchOutcome.NO_REALM;

        String charRegion = character.getRegion() != null ? character.getRegion().trim() : "";
        String settingRegion = settings.getRegion() != null ? settings.getRegion().trim() : "";
        boolean regionMatch = charRegion.equalsIgnoreCase(settingRegion);
        if (!regionMatch) return MatchOutcome.NO_REGION;

        Map<String, String> playerInfo = new HashMap<>();
        playerInfo.put("name", character.getName());
        playerInfo.put("realm", character.getRealm());
        playerInfo.put("class", character.getRaiderIOData().getClassType());
        playerInfo.put("role", character.getRaiderIOData().getActiveSpecRole());
        playerInfo.put("ilevel", character.getILevel() != null ? character.getILevel() : "0");
        String progress = character.getRaidProgressions().stream()
                .findFirst()
                .map(RaidProgressionResponse::getSummary)
                .orElse("0/8N");
        playerInfo.put("progress", progress);

        boolean filterPass = filterService.shouldSendMessage(settings.getChannel().getServer().getServerId(), playerInfo);
        if (!filterPass) return MatchOutcome.NO_FILTER;
        return MatchOutcome.MATCH;
    }

    private void sendFilteredMessage(CharacterInfoResponse character, UserSettings settings) {
        try {
            EmbedBuilder embedBuilder = getEmbedBuilder(character, settings);

            Map<String, String> playerInfo = new HashMap<>();
            playerInfo.put("name", character.getName());
            playerInfo.put("realm", character.getRealm() != null ? character.getRealm() : "");
            playerInfo.put("class", character.getRaiderIOData().getClassType());
            playerInfo.put("role", character.getRaiderIOData().getActiveSpecRole());
            playerInfo.put("ilevel", character.getILevel() != null ? character.getILevel() : "0");
            String progress = character.getRaidProgressions().stream()
                .findFirst()
                    .map(raid -> raid.getSummary())
                .orElse("0/8N");
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

    /**
     * LFG match result: which step rejected or matched.
     */
    private enum MatchOutcome {
        MATCH,
        NO_LANGUAGE,
        NO_REALM,
        NO_REGION,
        NO_FILTER
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
