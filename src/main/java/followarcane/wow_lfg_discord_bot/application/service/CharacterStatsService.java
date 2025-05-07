package followarcane.wow_lfg_discord_bot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.awt.*;
import java.text.DecimalFormat;

@Service
@Slf4j
public class CharacterStatsService {

    private final RestTemplate restTemplate;
    private final ClassColorCodeHelper classColorCodeHelper;
    private final BattleNetApiService battleNetApiService;

    private final DecimalFormat df = new DecimalFormat("#,###.##");

    public CharacterStatsService(RestTemplate restTemplate, ClassColorCodeHelper classColorCodeHelper,
                                 BattleNetApiService battleNetApiService) {
        this.restTemplate = restTemplate;
        this.classColorCodeHelper = classColorCodeHelper;
        this.battleNetApiService = battleNetApiService;
    }

    /**
     * Karakter istatistiklerini içeren bir embed oluşturur
     */
    public EmbedBuilder createCharacterStatsEmbed(String name, String realm, String region) {
        try {
            // Karakter sınıfı ve görüntüsü için Raider.io API'den veri al
            JsonNode raiderIoData = fetchRaiderIoData(name, realm, region);
            if (raiderIoData == null) {
                return createErrorEmbed("Character not found",
                        "Could not find character: " + name + " on " + realm + "-" + region.toUpperCase());
            }

            // Blizzard API'den istatistik verilerini al
            JsonNode statsData = fetchCharacterStats(name, realm, region);
            if (statsData == null) {
                return createErrorEmbed("Stats not found",
                        "Could not fetch stats for: " + name + " on " + realm + "-" + region.toUpperCase());
            }

            // Embed mesajı oluştur
            return buildStatsEmbed(raiderIoData, statsData, name, realm, region);
        } catch (Exception e) {
            log.error("Error creating character stats embed: {}", e.getMessage(), e);
            return createErrorEmbed("Error", "An error occurred while fetching data. Please try again later.");
        }
    }

    /**
     * Raider.io API'den karakter verilerini çeker
     */
    private JsonNode fetchRaiderIoData(String name, String realm, String region) {
        try {
            String raiderIoUrl = UriComponentsBuilder.fromHttpUrl("https://raider.io/api/v1/characters/profile")
                    .queryParam("region", region)
                    .queryParam("realm", realm)
                    .queryParam("name", name)
                    .queryParam("fields", "gear")
                    .build()
                    .toUriString();

            log.info("Fetching Raider.io data from: {}", raiderIoUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(raiderIoUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new ObjectMapper().readTree(response.getBody());
            }
        } catch (Exception e) {
            log.error("Error fetching Raider.io data: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Blizzard API'den karakter istatistiklerini çeker
     */
    private JsonNode fetchCharacterStats(String name, String realm, String region) {
        try {
            // Realm adını düzelt (tire yerine boşluk kullan)
            String formattedRealm = realm.replace("-", " ");
            
            // API isteği yap
            return battleNetApiService.fetchCharacterStats(name, formattedRealm, region);
        } catch (Exception e) {
            log.error("Error fetching character stats: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * İstatistik embed'ini oluşturur
     */
    private EmbedBuilder buildStatsEmbed(JsonNode raiderIoData, JsonNode statsData, String name, String realm, String region) {
        EmbedBuilder embed = new EmbedBuilder();

        // Karakter bilgilerini ayarla
        String characterName = raiderIoData.get("name").asText();
        String characterClass = raiderIoData.get("class").asText();
        String characterRealm = raiderIoData.get("realm").asText();
        String profileUrl = raiderIoData.get("profile_url").asText();
        String thumbnailUrl = raiderIoData.get("thumbnail_url").asText();

        // Sınıf rengini kullan
        embed.setColor(Color.decode(classColorCodeHelper.getClassColorCode(characterClass)));

        // Başlık ve thumbnail
        embed.setTitle(characterName + "'s Character Statistics", profileUrl);
        embed.setThumbnail(thumbnailUrl);

        // Ana istatistikler
        StringBuilder mainStats = new StringBuilder();
        mainStats.append("**Health:** ").append(df.format(statsData.get("health").asLong())).append("\n");

        // Power type ve değeri
        String powerType = statsData.path("power_type").path("name").asText();
        int powerValue = statsData.get("power").asInt();
        mainStats.append("**").append(powerType).append(":** ").append(powerValue).append("\n");

        // Temel özellikler
        mainStats.append("**Strength:** ").append(df.format(statsData.path("strength").path("effective").asLong())).append("\n");
        mainStats.append("**Agility:** ").append(df.format(statsData.path("agility").path("effective").asLong())).append("\n");
        mainStats.append("**Intellect:** ").append(df.format(statsData.path("intellect").path("effective").asLong())).append("\n");
        mainStats.append("**Stamina:** ").append(df.format(statsData.path("stamina").path("effective").asLong())).append("\n");
        mainStats.append("\n\n\n\n\n");

        embed.addField("__**PRIMARY STATS**__", mainStats.toString(), false);

        // İkincil istatistikler
        String secondaryStats = "**Critical Strike:** " + statsData.path("spell_crit").path("value").asText() + "%\n" +
                "**Haste:** " + statsData.path("spell_haste").path("value").asText() + "%\n" +
                "**Mastery:** " + statsData.path("mastery").path("value").asText() + "%\n" +
                "**Versatility:** " + statsData.get("versatility_damage_done_bonus").asText() + "%\n" +
                "**Leech:** " + statsData.path("lifesteal").path("value").asText() + "%\n" +
                "\n\n\n\n\n";

        embed.addField("__**SECONDARY STATS**__", secondaryStats, false);

        // Savunma istatistikleri
        String defenseStats = "**Armor:** " + df.format(statsData.path("armor").path("effective").asLong()) + "\n" +
                "**Dodge:** " + statsData.path("dodge").path("value").asText() + "%\n" +
                "**Parry:** " + statsData.path("parry").path("value").asText() + "%\n" +
                "**Block:** " + statsData.path("block").path("value").asText() + "%\n" +
                "**Avoidance:** " + statsData.path("avoidance").path("rating_bonus").asText() + "%\n" +
                "\n\n\n\n\n";

        embed.addField("__**DEFENSE**__", defenseStats, false);

        // Saldırı istatistikleri
        StringBuilder attackStats = new StringBuilder();

        // Büyü gücü veya saldırı gücü (sınıfa göre)
        if (statsData.get("spell_power").asInt() > 0) {
            attackStats.append("**Spell Power:** ").append(df.format(statsData.get("spell_power").asLong())).append("\n");
        } else {
            attackStats.append("**Attack Power:** ").append(df.format(statsData.get("attack_power").asLong())).append("\n");
        }

        // Silah hasarı
        if (statsData.get("main_hand_dps").asDouble() > 0) {
            attackStats.append("**Weapon DPS:** ").append(df.format(statsData.get("main_hand_dps").asDouble())).append("\n");
            attackStats.append("**Damage Range:** ").append(df.format(statsData.get("main_hand_damage_min").asDouble()))
                    .append(" - ").append(df.format(statsData.get("main_hand_damage_max").asDouble())).append("\n");
            attackStats.append("**Attack Speed:** ").append(statsData.get("main_hand_speed").asText()).append("\n");
        }
        attackStats.append("\n\n\n\n\n");

        embed.addField("__**ATTACK**__", attackStats.toString(), false);

        // Footer ekle
        embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership",
                "https://i.imgur.com/fK2PvPV.png");

        return embed;
    }

    /**
     * Hata durumunda gösterilecek embed'i oluşturur
     */
    private EmbedBuilder createErrorEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        embed.setDescription(description);
        embed.setColor(Color.RED);
        return embed;
    }
} 