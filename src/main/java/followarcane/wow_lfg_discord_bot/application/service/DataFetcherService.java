package followarcane.wow_lfg_discord_bot.application.service;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return character.getLanguages().toLowerCase().contains(settings.getLanguage())
                && (character.getRealm().equalsIgnoreCase(settings.getRealm()) || settings.getRealm().equalsIgnoreCase("all"))
                && character.getRegion().equalsIgnoreCase(settings.getRegion());
    }

    private static @NotNull EmbedBuilder getEmbedBuilder(CharacterInfoResponse character) {
        String raiderIOLink = ("https://raider.io/characters/" + character.getRegion() + "/" + character.getRealm().replace(' ', '-') + "/" + character.getName()).toLowerCase();
        String wowProgressLink = ("https://www.wowprogress.com/character/" + character.getRegion() + "/" + character.getRealm().replace(' ', '-') + "/" + character.getName()).toLowerCase();
        String warcraftLogsLink = ("https://www.warcraftlogs.com/character/" + character.getRegion() + "/" + character.getRealm().replace(' ', '-') + "/" + character.getName()).toLowerCase();
        String armoryLink = ("https://worldofwarcraft.blizzard.com/en-gb/character/" + character.getRegion() + "/" + character.getRealm().replace(' ', '-') + "/" + character.getName()).toLowerCase();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(character.getName() + " | " + character.getRealm() + " | " + character.getRaiderIOData().getClassType() + " | " + character.getRaiderIOData().getActiveSpecRole() + " | " + character.getRaiderIOData().getActiveSpecName(), raiderIOLink);
        embedBuilder.addField("Languages", character.getLanguages(), true);
        embedBuilder.addField("Item Level", StringUtils.hasText(character.getILevel()) ? character.getILevel() : "No Info", true);
        embedBuilder.addField("Faction", StringUtils.hasText(character.getRaiderIOData().getFaction()) ? character.getRaiderIOData().getFaction() : "No Info", true);

        for (var raidProgression : character.getRaidProgressions()) {
            embedBuilder.addField(raidProgression.getRaidName(), raidProgression.getSummary(), false);
        }
        embedBuilder.addField("Information About Player", character.getCommentary().length() > 1020 ?
                character.getCommentary().substring(0, 1020) + "..." : character.getCommentary(), false);
        embedBuilder.addField("External Links",
                "[Armory]" + "(" + armoryLink + ")" +
                        " | [Raider IO]" + "(" + raiderIOLink + ")" +
                        " | [WowProgress]" + "(" + wowProgressLink + ")" +
                        " | [Warcraftlogs]" + "(" + warcraftLogsLink + ")"
                , false);
        embedBuilder.setFooter("Donate -> https://www.patreon.com/Shadlynn/membership","https://imgur.com/iZpMxz0.jpg");
        embedBuilder.setThumbnail(character.getRaiderIOData().getThumbnailUrl());

        embedBuilder.setColor(Color.decode(classColorCodeHelper.getClassColorCode(character.getRaiderIOData().getClassType())));
        return embedBuilder;
    }
}
