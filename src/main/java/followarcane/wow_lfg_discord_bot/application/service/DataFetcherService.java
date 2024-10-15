package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.response.BossRankResponse;
import followarcane.wow_lfg_discord_bot.application.response.CharacterInfoResponse;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataFetcherService {

    private final DiscordBotService discordBotService;
    private final ApiProperties apiProperties;
    private final DiscordService discordService;

    private static ClassColorCodeHelper classColorCodeHelper;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String wowApi = "http://localhost:8080/api/v1/wdc/latest-lfg";
    private Set<CharacterInfoResponse> previousData = new HashSet<>();

    @Autowired
    public DataFetcherService(DiscordBotService discordBotService, ApiProperties apiProperties, DiscordService discordService, ClassColorCodeHelper classColorCodeHelper) {
        this.discordBotService = discordBotService;
        this.apiProperties = apiProperties;
        this.discordService = discordService;
        this.classColorCodeHelper = classColorCodeHelper;

        // Check if username and password are not null or empty
        Assert.notNull(apiProperties.getUsername(), "Username must not be null");
        Assert.notNull(apiProperties.getPassword(), "Password must not be null");

        this.restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(apiProperties.getUsername(), apiProperties.getPassword()));
    }

    @Scheduled(fixedRate = 30000)
    public void fetchData() {
        log.info("Fetching data from WoW API...");

        try {
            ResponseEntity<List<CharacterInfoResponse>> response = restTemplate.exchange(
                    wowApi, HttpMethod.GET, null, new ParameterizedTypeReference<List<CharacterInfoResponse>>() {
                    });
            List<CharacterInfoResponse> data = response.getBody();

            if (data != null && !data.isEmpty()) {
                log.info("Data fetched successfully: {}", data);

                Set<CharacterInfoResponse> newData = new HashSet<>(data);
                newData.removeAll(previousData);

                if (!newData.isEmpty()) {
                    List<UserSettings> matchedLfgs = discordService.getAllUserSettings();
                    for (UserSettings settings : matchedLfgs) {
                        List<CharacterInfoResponse> filteredData = newData.stream()
                                .filter(character -> characterMatchesSettings(character, settings))
                                .toList();

                        for (CharacterInfoResponse character : filteredData) {
                            if (!settings.getChannel().getLastSentCharacters().contains(character.getName())) {
                                log.info("Character: {}", character);

                                EmbedBuilder embedBuilder = getEmbedBuilder(character);
                                discordBotService.sendEmbedMessageToChannel(settings.getChannel().getChannelId(), embedBuilder);

                                discordService.updateLastSentCharacters(settings.getChannel(), character.getName());
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
            log.error("Error fetching data from WoW API", e);
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

        return languageMatch && realmMatch && regionMatch;
    }


    private static @NotNull EmbedBuilder getEmbedBuilder(CharacterInfoResponse character) {
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
        embedBuilder.addField("Faction", StringUtils.hasText(character.getRaiderIOData().getFaction()) ? character.getRaiderIOData().getFaction() : "No Info", true);

        StringBuilder progression = new StringBuilder();
        for (var raidProgression : character.getRaidProgressions()) {
            progression.append(raidProgression.getRaidName()).append("\n").append(raidProgression.getSummary()).append("\n\n");
        }

        if (progression.isEmpty()) {
            progression.append("Use the links below to check the progression.");
        }

        if (character.getWarcraftLogsData().getBestPerformanceAverage() != 0) {
            embedBuilder.addField("WarcraftLogs", "Overall Performance : " + character.getWarcraftLogsData().getBestPerformanceAverage() + "\n----------\n" + prepareLogs(character.getBossRanks()), false);
        }

        embedBuilder.addField("Raid Progression", progression.toString(), false);
        embedBuilder.addField("Information About Player", character.getCommentary().length() > 1020
                ? character.getCommentary().substring(0, 1020) + "..."
                : character.getCommentary(), false);
        embedBuilder.addField("External Links",
                "[Armory]" + "(" + armoryLink + ")" +
                        " | [Raider IO]" + "(" + raiderIOLink + ")" +
                        " | [WowProgress]" + "(" + wowProgressLink + ")" +
                        " | [Warcraftlogs]" + "(" + warcraftLogsLink + ")"
                , false);
        embedBuilder.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://imgur.com/kk6VClj.png");
        embedBuilder.setThumbnail(character.getRaiderIOData().getThumbnailUrl());

        embedBuilder.setColor(Color.decode(classColorCodeHelper.getClassColorCode(character.getRaiderIOData().getClassType())));
        return embedBuilder;
    }

    private static String encodeURL(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String prepareLogs(List<BossRankResponse> responses) {
        return responses.stream()
                .map(response -> response.getEncounterName() + " : " + response.getRankPercent())
                .collect(Collectors.joining("\n"));
    }

}
