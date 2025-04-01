package followarcane.wow_lfg_discord_bot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.awt.*;
import java.util.*;
import java.util.List;

@Service
@Slf4j
public class WowVaultService {

    private final RestTemplate restTemplate;
    private final ClassColorCodeHelper classColorCodeHelper;
    private final BattleNetApiService battleNetApiService;

    @Value("${battle-net.client.id}")
    private String battleNetClientApi;

    @Value("${battle-net.client.secret}")
    private String battleNetClientSecret;

    @Value("${battle-net.api.url}")
    private String battleNetApiUrl;

    // Token önbelleği için değişkenler
    private String blizzardToken;
    private long tokenExpiry = 0;

    public WowVaultService(RestTemplate restTemplate, ClassColorCodeHelper classColorCodeHelper, BattleNetApiService battleNetApiService) {
        this.restTemplate = restTemplate;
        this.classColorCodeHelper = classColorCodeHelper;
        this.battleNetApiService = battleNetApiService;
    }

    /**
     * Karakter için Great Vault bilgilerini içeren bir embed oluşturur
     */
    public EmbedBuilder createVaultEmbed(String name, String realm, String region) {
        try {
            // 1. Raider.io API'den Mythic+ verilerini al
            JsonNode raiderIoData = fetchRaiderIoData(name, realm, region);
            if (raiderIoData == null) {
                return createErrorEmbed("Character not found",
                        "Could not find character: " + name + " on " + realm + "-" + region.toUpperCase());
            }

            // 2. Blizzard API'den raid verilerini al
            JsonNode blizzardData = fetchBlizzardData(name, realm, region);

            // 3. Embed mesajı oluştur
            return buildVaultEmbed(raiderIoData, blizzardData, name, realm, region);
        } catch (Exception e) {
            log.error("Error creating vault embed: {}", e.getMessage(), e);
            return createErrorEmbed("Error", "An error occurred while fetching data. Please try again later.");
        }
    }

    /**
     * Karakter için haftalık M+ koşularını içeren bir embed oluşturur
     */
    public EmbedBuilder createWeeklyRunsEmbed(String name, String realm, String region) {
        try {
            // Raider.io API'den Mythic+ verilerini al
            JsonNode raiderIoData = fetchRaiderIoData(name, realm, region);
            if (raiderIoData == null) {
                return createErrorEmbed("Character not found",
                        "Could not find character: " + name + " on " + realm + "-" + region.toUpperCase());
            }

            // Embed mesajı oluştur
            EmbedBuilder embed = new EmbedBuilder();

            // Karakter bilgilerini ayarla
            String characterName = raiderIoData.get("name").asText();
            String characterClass = raiderIoData.get("class").asText();
            String characterRealm = raiderIoData.get("realm").asText();
            String profileUrl = raiderIoData.get("profile_url").asText();
            String thumbnailUrl = raiderIoData.get("thumbnail_url").asText();

            embed.setTitle(characterName + " | " + characterRealm + " | Weekly Mythic+ Runs", profileUrl);
            embed.setThumbnail(thumbnailUrl);
            embed.setColor(Color.decode(classColorCodeHelper.getClassColorCode(characterClass)));

            // Haftalık M+ koşularını ekle
            addWeeklyRunsToEmbed(embed, raiderIoData);

            // Footer ekle
            embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");

            return embed;
        } catch (Exception e) {
            log.error("Error creating weekly runs embed: {}", e.getMessage(), e);
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
                    .queryParam("fields", "mythic_plus_scores_by_season:current,mythic_plus_weekly_highest_level_runs")
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
     * Blizzard API'den raid verilerini çeker
     */
    private JsonNode fetchBlizzardData(String name, String realm, String region) {
        return battleNetApiService.fetchCharacterRaidData(name, realm, region);
    }

    /**
     * Vault embed'ini oluşturur
     */
    private EmbedBuilder buildVaultEmbed(JsonNode raiderIoData, JsonNode blizzardData, String name, String realm, String region) {
        EmbedBuilder embed = new EmbedBuilder();

        // Karakter bilgilerini ayarla
        String characterName = raiderIoData.get("name").asText();
        String characterClass = raiderIoData.get("class").asText();
        String characterRealm = raiderIoData.get("realm").asText();
        String profileUrl = raiderIoData.get("profile_url").asText();
        String thumbnailUrl = raiderIoData.get("thumbnail_url").asText();

        // Altın rengini kullan (WoW UI'ına benzer)
        Color goldColor = new Color(207, 171, 49);
        embed.setColor(goldColor);

        // Başlık ve açıklama
        embed.setTitle(characterName + "'s Great Vault", profileUrl);
        embed.setDescription("**Add items to the Great Vault by completing activities each week.\nOnce per week you may select a single reward.**");
        
        // M+ koşu sayısını hesapla
        int totalMythicPlusRuns = 0;
        JsonNode runsNode = raiderIoData.path("mythic_plus_weekly_highest_level_runs");
        if (!runsNode.isMissingNode() && runsNode.isArray()) {
            totalMythicPlusRuns = runsNode.size();
        }
        
        // M+ Great Vault ödüllerini hesapla
        String[] mythicPlusRewards = calculateMythicPlusRewards(raiderIoData);

        // Raid boss sayısını ve ödüllerini hesapla
        int[] raidBossCounts = {0, 0, 0, 0}; // [total, mythic, heroic, normal]
        String[] raidRewards = {"No Reward", "No Reward", "No Reward"};

        if (blizzardData != null) {
            // Her boss için en yüksek zorluk seviyesini takip et
            Map<String, String> bossHighestDifficulty = calculateBossHighestDifficulty(blizzardData, region);

            // Toplam unique boss sayısını hesapla
            raidBossCounts[0] = bossHighestDifficulty.size();

            // Zorluk seviyelerine göre boss sayılarını hesapla
            for (String difficulty : bossHighestDifficulty.values()) {
                switch (difficulty) {
                    case "mythic":
                        raidBossCounts[1]++;
                        break;
                    case "heroic":
                        raidBossCounts[2]++;
                        break;
                    case "normal":
                        raidBossCounts[3]++;
                        break;
                }
            }

            // Raid ödüllerini hesapla
            raidRewards = calculateRaidRewardsFromCounts(raidBossCounts);
        }

        // Raid bölümü
        StringBuilder raidSection = new StringBuilder();
        raidSection.append("**Raids**\n");
        raidSection.append("```\n");
        raidSection.append("Defeat 2 Bosses    Defeat 4 Bosses    Defeat 6 Bosses\n");
        raidSection.append("\n");

        // Raid ödüllerinin durumunu göster
        String slot1Status = raidBossCounts[0] >= 2 ? "✅" : "🔒";
        String slot2Status = raidBossCounts[0] >= 4 ? "✅" : "🔒";
        String slot3Status = raidBossCounts[0] >= 6 ? "✅" : "🔒";

        raidSection.append(String.format("%-25s %-25s %-25s\n", slot1Status, slot2Status, slot3Status));
        raidSection.append(String.format("%-25s %-25s %-25s\n",
                raidBossCounts[0] + "/2",
                raidBossCounts[0] + "/4",
                raidBossCounts[0] + "/6"));
        raidSection.append("```\n");

        // Mythic+ bölümü
        StringBuilder dungeonSection = new StringBuilder();
        dungeonSection.append("**Dungeons**\n");
        dungeonSection.append("```\n");
        dungeonSection.append("Complete 1 Dungeon,      Complete 4 Dungeons,      Complete 8 Dungeons\n");
        dungeonSection.append("\n");

        // M+ ödüllerinin durumunu göster
        String m1Status = totalMythicPlusRuns >= 1 ? "✅" : "🔒";
        String m2Status = totalMythicPlusRuns >= 4 ? "✅" : "🔒";
        String m3Status = totalMythicPlusRuns >= 8 ? "✅" : "🔒";

        dungeonSection.append(String.format("%-25s %-25s %-25s\n", m1Status, m2Status, m3Status));
        dungeonSection.append(String.format("%-25s %-25s %-25s\n",
                totalMythicPlusRuns + "/1",
                totalMythicPlusRuns + "/4",
                totalMythicPlusRuns + "/8"));
        dungeonSection.append("```\n");

        // World bölümü (opsiyonel)
        StringBuilder worldSection = new StringBuilder();
        worldSection.append("**World**\n");
        worldSection.append("```\n");
        worldSection.append("Complete 2 Delves or     Complete 4 Delves or     Complete 8 Delves or\n");
        worldSection.append("World Activities          World Activities          World Activities\n");
        worldSection.append("\n");
        worldSection.append(String.format("%-25s %-25s %-25s\n", "🔒", "🔒", "🔒"));
        worldSection.append(String.format("%-25s %-25s %-25s\n", "0/2", "0/4", "0/8"));
        worldSection.append("```\n");

        // Ödüller bölümü
        StringBuilder rewardsSection = new StringBuilder();
        rewardsSection.append("**Available Rewards**\n");
        rewardsSection.append("```\n");

        boolean hasRewards = false;

        // Raid ödülleri
        if (!raidRewards[0].equals("No Reward")) {
            rewardsSection.append("Raid Slot 1: " + raidRewards[0] + "\n");
            hasRewards = true;
        }
        if (!raidRewards[1].equals("No Reward")) {
            rewardsSection.append("Raid Slot 2: " + raidRewards[1] + "\n");
            hasRewards = true;
        }
        if (!raidRewards[2].equals("No Reward")) {
            rewardsSection.append("Raid Slot 3: " + raidRewards[2] + "\n");
            hasRewards = true;
        }

        // M+ ödülleri
        if (!mythicPlusRewards[0].equals("No Reward")) {
            rewardsSection.append("M+ Slot 1: " + mythicPlusRewards[0] + "\n");
            hasRewards = true;
        }
        if (!mythicPlusRewards[1].equals("No Reward")) {
            rewardsSection.append("M+ Slot 2: " + mythicPlusRewards[1] + "\n");
            hasRewards = true;
        }
        if (!mythicPlusRewards[2].equals("No Reward")) {
            rewardsSection.append("M+ Slot 3: " + mythicPlusRewards[2] + "\n");
            hasRewards = true;
        }

        if (!hasRewards) {
            rewardsSection.append("No rewards available yet. Complete activities to unlock rewards.\n");
        }

        rewardsSection.append("```\n");

        // Alanları ekle
        embed.addField("", raidSection.toString(), false);
        embed.addField("", dungeonSection.toString(), false);
        embed.addField("", worldSection.toString(), false);
        embed.addField("", rewardsSection.toString(), false);

        // Karakter bilgilerini thumbnail olarak ekle
        embed.setThumbnail(thumbnailUrl);

        // Footer ekle
        embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");

        return embed;
    }

    /**
     * Statü metnini belirli bir uzunluğa göre formatlar
     */
    private String formatStatusText(String text, int maxLength) {
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        } else {
            return text + " ".repeat(maxLength - text.length());
        }
    }

    /**
     * M+ koşularına göre Great Vault ödüllerini hesaplar
     */
    private String[] calculateMythicPlusRewards(JsonNode raiderIoData) {
        String[] rewards = new String[3];

        try {
            // Haftalık M+ koşularını al ve seviyelerine göre sırala
            JsonNode runsNode = raiderIoData.path("mythic_plus_weekly_highest_level_runs");
            List<Integer> runLevels = new ArrayList<>();

            if (!runsNode.isMissingNode() && runsNode.isArray()) {
                for (JsonNode run : runsNode) {
                    runLevels.add(run.get("mythic_level").asInt());
                }

                Collections.sort(runLevels, Collections.reverseOrder());

                // Ödülleri hesapla
                // Slot 1: En yüksek koşu
                if (!runLevels.isEmpty()) {
                    rewards[0] = getVaultReward(runLevels.get(0));
                } else {
                    rewards[0] = "No Reward";
                }

                // Slot 2: En yüksek 4 koşunun en düşüğü
                if (runLevels.size() >= 4) {
                    rewards[1] = getVaultReward(runLevels.get(3));
                } else {
                    rewards[1] = "No Reward";
                }

                // Slot 3: En yüksek 8 koşunun en düşüğü
                if (runLevels.size() >= 8) {
                    rewards[2] = getVaultReward(runLevels.get(7));
                } else {
                    rewards[2] = "No Reward";
                }

                return rewards;
            }
        } catch (Exception e) {
            log.error("Error calculating M+ rewards: {}", e.getMessage(), e);
        }

        // Varsayılan değerler
        rewards[0] = "No M+ runs this week";
        rewards[1] = "No M+ runs this week";
        rewards[2] = "No M+ runs this week";

        return rewards;
    }

    /**
     * Raid Great Vault ödüllerini hesaplar
     */
    private String[] calculateRaidRewards(JsonNode blizzardData, String region) {
        String[] rewards = {"No Reward", "No Reward", "No Reward"};
        
        try {
            if (blizzardData == null) {
                return rewards;
            }

            // Her boss için en yüksek zorluk seviyesini takip et
            Map<String, String> bossHighestDifficulty = new HashMap<>();

            // Haftalık reset zamanını hesapla
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
            cal.set(Calendar.HOUR_OF_DAY, region.equalsIgnoreCase("eu") ? 7 : 15);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long currentTime = System.currentTimeMillis();
            if (cal.getTimeInMillis() > currentTime) {
                cal.add(Calendar.WEEK_OF_YEAR, -1);
            }

            long weekStartTime = cal.getTimeInMillis();
            log.info("Weekly reset time for region {}: {}", region, new java.util.Date(weekStartTime));

            // "Liberation of Undermine" raid'ini bul
            if (blizzardData.has("expansions") && blizzardData.get("expansions").isArray()) {
                for (JsonNode expansion : blizzardData.get("expansions")) {
                    if (expansion.has("instances") && expansion.get("instances").isArray()) {
                        for (JsonNode instanceNode : expansion.get("instances")) {
                            if (instanceNode.has("instance") &&
                                    instanceNode.get("instance").has("name") &&
                                    instanceNode.get("instance").get("name").asText().equals("Liberation of Undermine")) {
                                
                                // Her zorluk seviyesi için kontrol et
                                if (instanceNode.has("modes") && instanceNode.get("modes").isArray()) {
                                    for (JsonNode modeNode : instanceNode.get("modes")) {
                                        String difficulty = modeNode.path("difficulty").path("type").asText().toLowerCase();

                                        if (modeNode.has("progress")) {
                                            JsonNode progressNode = modeNode.get("progress");

                                            if (progressNode.has("encounters") && progressNode.get("encounters").isArray()) {
                                                for (JsonNode encounterNode : progressNode.get("encounters")) {
                                                    if (encounterNode.has("last_kill_timestamp")) {
                                                        long killTime = encounterNode.get("last_kill_timestamp").asLong();

                                                        // Bu hafta öldürüldü mü kontrol et
                                                        if (killTime >= weekStartTime) {
                                                            String bossId = encounterNode.path("encounter").path("id").asText();
                                                            String bossName = encounterNode.path("encounter").path("name").asText();

                                                            log.debug("Found kill for boss {} ({}) at {} difficulty, time: {}", 
                                                                    bossName, bossId, difficulty, new Date(killTime));
                                                            
                                                            // Bu boss için daha önce daha yüksek zorluk seviyesinde kill var mı?
                                                            String currentHighest = bossHighestDifficulty.getOrDefault(bossId, "none");

                                                            if (compareDifficulty(difficulty, currentHighest) > 0) {
                                                                // Daha yüksek zorluk seviyesi bulundu
                                                                bossHighestDifficulty.put(bossId, difficulty);
                                                                log.debug("Updated highest difficulty for boss {} to {}", bossName, difficulty);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            }

            // Unique boss sayısını hesapla
            int uniqueBossesKilled = bossHighestDifficulty.size();
            log.info("Unique bosses killed this week: {}", uniqueBossesKilled);

            // Boss ID'lerini ve zorluk seviyelerini logla
            for (Map.Entry<String, String> entry : bossHighestDifficulty.entrySet()) {
                log.debug("Boss ID: {}, Highest Difficulty: {}", entry.getKey(), entry.getValue());
            }

            // Zorluk seviyelerine göre boss sayılarını hesapla
            int mythicBosses = 0;
            int heroicBosses = 0;
            int normalBosses = 0;
            int lfrBosses = 0;

            for (String difficulty : bossHighestDifficulty.values()) {
                switch (difficulty) {
                    case "mythic":
                        mythicBosses++;
                        break;
                    case "heroic":
                        heroicBosses++;
                        break;
                    case "normal":
                        normalBosses++;
                        break;
                    case "lfr":
                        lfrBosses++;
                        break;
                }
            }

            log.info("Mythic bosses: {}, Heroic bosses: {}, Normal bosses: {}, LFR bosses: {}",
                    mythicBosses, heroicBosses, normalBosses, lfrBosses);

            // Slot 1 (2+ boss) için ödül hesapla
            if (uniqueBossesKilled >= 2) {
                if (mythicBosses >= 2) {
                    rewards[0] = getRaidReward("mythic");
                } else if (heroicBosses + mythicBosses >= 2) {
                    rewards[0] = getRaidReward("heroic");
                } else if (normalBosses + heroicBosses + mythicBosses >= 2) {
                    rewards[0] = getRaidReward("normal");
                } else {
                    rewards[0] = getRaidReward("lfr");
                }
            }

            // Slot 2 (4+ boss) için ödül hesapla
            if (uniqueBossesKilled >= 4) {
                if (mythicBosses >= 4) {
                    rewards[1] = getRaidReward("mythic");
                } else if (heroicBosses + mythicBosses >= 4) {
                    rewards[1] = getRaidReward("heroic");
                } else if (normalBosses + heroicBosses + mythicBosses >= 4) {
                    rewards[1] = getRaidReward("normal");
                } else {
                    rewards[1] = getRaidReward("lfr");
                }
            }

            // Slot 3 (6+ boss) için ödül hesapla
            if (uniqueBossesKilled >= 6) {
                if (mythicBosses >= 6) {
                    rewards[2] = getRaidReward("mythic");
                } else if (heroicBosses + mythicBosses >= 6) {
                    rewards[2] = getRaidReward("heroic");
                } else if (normalBosses + heroicBosses + mythicBosses >= 6) {
                    rewards[2] = getRaidReward("normal");
                } else {
                    rewards[2] = getRaidReward("lfr");
                }
            }
        } catch (Exception e) {
            log.error("Error calculating raid rewards: {}", e.getMessage(), e);
        }

        return rewards;
    }

    /**
     * İki zorluk seviyesini karşılaştırır
     *
     * @return pozitif: difficulty1 > difficulty2, negatif: difficulty1 < difficulty2, 0: eşit
     */
    private int compareDifficulty(String difficulty1, String difficulty2) {
        int rank1 = getDifficultyRank(difficulty1);
        int rank2 = getDifficultyRank(difficulty2);
        return rank1 - rank2;
    }

    /**
     * Zorluk seviyesinin sıralama değerini döndürür
     */
    private int getDifficultyRank(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "mythic":
                return 4;
            case "heroic":
                return 3;
            case "normal":
                return 2;
            case "lfr":
                return 1;
            default:
                return 0; // "none" veya bilinmeyen
        }
    }

    /**
     * Raid zorluk seviyesine göre Great Vault ödülünü döndürür
     */
    private String getRaidReward(String difficulty) {
        // Bu değerler güncel raid'e göre ayarlanmalı
        switch (difficulty) {
            case "mythic":
                return "Myth 1 (662)";
            case "heroic":
                return "Hero 4 (658)";
            case "normal":
                return "Hero 2 (652)";
            case "lfr":
                return "Champion 4 (645)";
            default:
                return "No Reward";
        }
    }

    /**
     * Bu hafta öldürülen raid boss'larını hesaplar
     */
    private int[] calculateWeeklyRaidProgress(JsonNode blizzardData, String region) {
        int[] bossesKilledByDifficulty = new int[3]; // [normal, heroic, mythic]

        try {
            // Şu anki zamanı al
            long currentTime = System.currentTimeMillis();

            // Bu haftanın başlangıcını hesapla (bölgeye göre)
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(currentTime);

            // Bölgeye göre reset gününü ve saatini ayarla
            if (region.equalsIgnoreCase("eu")) {
                // EU: Çarşamba 04:00 UTC
                cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                cal.set(Calendar.HOUR_OF_DAY, 4);
            } else {
                // US, Latin, Oceanic: Salı 15:00 UTC
                cal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                cal.set(Calendar.HOUR_OF_DAY, 15);
            }

            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            // Eğer şu anki zaman, hesaplanan reset zamanından önceyse,
            // bir önceki haftanın reset zamanını al
            if (cal.getTimeInMillis() > currentTime) {
                cal.add(Calendar.WEEK_OF_YEAR, -1);
            }

            long weekStartTime = cal.getTimeInMillis();
            log.info("Weekly reset time for region {}: {}", region, new java.util.Date(weekStartTime));

            // "Liberation of Undermine" raid'ini bul
            if (blizzardData.has("expansions") && blizzardData.get("expansions").isArray()) {
                for (JsonNode expansion : blizzardData.get("expansions")) {
                    if (expansion.has("instances") && expansion.get("instances").isArray()) {
                        for (JsonNode instanceNode : expansion.get("instances")) {
                            if (instanceNode.has("instance") &&
                                    instanceNode.get("instance").has("name") &&
                                    instanceNode.get("instance").get("name").asText().equals("Liberation of Undermine")) {

                                // Her zorluk seviyesi için kontrol et
                                if (instanceNode.has("modes") && instanceNode.get("modes").isArray()) {
                                    for (JsonNode modeNode : instanceNode.get("modes")) {
                                        String difficulty = modeNode.path("difficulty").path("type").asText().toLowerCase();
                                        int difficultyIndex = getDifficultyIndex(difficulty);

                                        if (difficultyIndex >= 0 && modeNode.has("progress")) {
                                            JsonNode progressNode = modeNode.get("progress");

                                            if (progressNode.has("encounters") && progressNode.get("encounters").isArray()) {
                                                int weeklyKills = 0;

                                                for (JsonNode encounterNode : progressNode.get("encounters")) {
                                                    if (encounterNode.has("last_kill_timestamp")) {
                                                        long killTime = encounterNode.get("last_kill_timestamp").asLong();

                                                        // Bu hafta öldürüldü mü kontrol et
                                                        if (killTime >= weekStartTime) {
                                                            weeklyKills++;
                                                            log.debug("Weekly kill found: {} at {}",
                                                                    encounterNode.path("encounter").path("name").asText(),
                                                                    new java.util.Date(killTime));
                                                        }
                                                    }
                                                }

                                                bossesKilledByDifficulty[difficultyIndex] = weeklyKills;
                                                log.info("Weekly kills for {} difficulty: {}",
                                                        difficulty, weeklyKills);
                                            }
                                        }
                                    }
                                }

                                // Liberation of Undermine bulundu, döngüden çık
                                return bossesKilledByDifficulty;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calculating weekly raid progress: {}", e.getMessage(), e);
        }

        return bossesKilledByDifficulty;
    }

    /**
     * Zorluk seviyesini indekse dönüştürür
     */
    private int getDifficultyIndex(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "normal":
                return 0;
            case "heroic":
                return 1;
            case "mythic":
                return 2;
            default:
                return -1;
        }
    }

    /**
     * M+ seviyesine göre Great Vault ödülünü döndürür
     */
    private String getVaultReward(int mythicLevel) {
        // Based on the table you provided
        if (mythicLevel <= 0) return "No reward";
        else if (mythicLevel == 1) return "Champion 4 (645)";
        else if (mythicLevel == 2) return "Hero 1 (649)";
        else if (mythicLevel == 3) return "Hero 1 (649)";
        else if (mythicLevel == 4) return "Hero 2 (652)";
        else if (mythicLevel == 5) return "Hero 2 (652)";
        else if (mythicLevel == 6) return "Hero 3 (655)";
        else if (mythicLevel == 7) return "Hero 4 (658)";
        else if (mythicLevel == 8) return "Hero 4 (658)";
        else if (mythicLevel == 9) return "Hero 4 (658)";
        else return "Myth 1 (662)";
    }

    /**
     * Haftalık M+ koşularını embed'e ekler
     */
    private void addWeeklyRunsToEmbed(EmbedBuilder embed, JsonNode raiderIoData) {
        JsonNode runsNode = raiderIoData.path("mythic_plus_weekly_highest_level_runs");
        if (!runsNode.isMissingNode() && runsNode.isArray() && runsNode.size() > 0) {
            // Her 5 run için bir alan oluştur
            int runCount = runsNode.size();
            int runsPerField = 5;
            int fieldsNeeded = (int) Math.ceil(runCount / (double) runsPerField);

            for (int i = 0; i < fieldsNeeded; i++) {
                StringBuilder runsInfo = new StringBuilder();
                int startIdx = i * runsPerField;
                int endIdx = Math.min(startIdx + runsPerField, runCount);

                for (int j = startIdx; j < endIdx; j++) {
                    JsonNode run = runsNode.get(j);
                    String dungeon = run.get("dungeon").asText();
                    int level = run.get("mythic_level").asInt();
                    int upgrades = run.get("num_keystone_upgrades").asInt();
                    String completedAt = run.get("completed_at").asText().substring(0, 10);
                    double score = run.get("score").asDouble();
                    String dgUrl = run.get("url").asText();

                    String upgradeStars = "";
                    for (int k = 0; k < upgrades; k++) {
                        upgradeStars += "⭐";
                    }

                    runsInfo.append("**[").append(dungeon).append("](").append(dgUrl).append(")")
                            .append("** +").append(level)
                            .append(" ").append(upgradeStars)
                            .append("\n **Score: ").append(score).append(" - ").append(completedAt).append("**")
                            .append("\n\n");
                }

                String fieldTitle = (fieldsNeeded == 1) ?
                        "Weekly Mythic+ Runs" :
                        "Weekly Mythic+ Runs (" + (i + 1) + "/" + fieldsNeeded + ")";

                embed.addField(fieldTitle, runsInfo.toString(), false);
            }
        } else {
            embed.addField("Weekly Mythic+ Runs", "No runs found for this week", false);
        }
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

    /**
     * Bir sonraki daha iyi ödülü döndürür
     */
    private String getNextBetterReward(int currentLevel) {
        if (currentLevel < 2) return "Hero 1 (649)";
        else if (currentLevel < 4) return "Hero 2 (652)";
        else if (currentLevel < 6) return "Hero 3 (655)";
        else if (currentLevel < 10) return "Hero 4 (658)";
        else return "Myth 1 (662)";
    }

    /**
     * Belirli bir ödül için gereken minimum seviyeyi döndürür
     */
    private int getMinLevelForReward(String reward) {
        switch (reward) {
            case "Champion 4 (645)":
                return 1;
            case "Hero 1 (649)":
                return 2;
            case "Hero 2 (652)":
                return 4;
            case "Hero 3 (655)":
                return 6;
            case "Hero 4 (658)":
                return 7;
            case "Myth 1 (662)":
                return 10;
            default:
                return 1;
        }
    }

    /**
     * Her boss için en yüksek zorluk seviyesini hesaplar
     */
    private Map<String, String> calculateBossHighestDifficulty(JsonNode blizzardData, String region) {
        Map<String, String> bossHighestDifficulty = new HashMap<>();

        try {
            // Haftalık reset zamanını hesapla
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
            cal.set(Calendar.HOUR_OF_DAY, region.equalsIgnoreCase("eu") ? 7 : 15);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long currentTime = System.currentTimeMillis();
            if (cal.getTimeInMillis() > currentTime) {
                cal.add(Calendar.WEEK_OF_YEAR, -1);
            }

            long weekStartTime = cal.getTimeInMillis();

            // "Liberation of Undermine" raid'ini bul
            if (blizzardData.has("expansions") && blizzardData.get("expansions").isArray()) {
                for (JsonNode expansion : blizzardData.get("expansions")) {
                    if (expansion.has("instances") && expansion.get("instances").isArray()) {
                        for (JsonNode instanceNode : expansion.get("instances")) {
                            if (instanceNode.has("instance") &&
                                    instanceNode.get("instance").has("name") &&
                                    instanceNode.get("instance").get("name").asText().equals("Liberation of Undermine")) {

                                // Her zorluk seviyesi için kontrol et
                                if (instanceNode.has("modes") && instanceNode.get("modes").isArray()) {
                                    for (JsonNode modeNode : instanceNode.get("modes")) {
                                        String difficulty = modeNode.path("difficulty").path("type").asText().toLowerCase();

                                        if (modeNode.has("progress")) {
                                            JsonNode progressNode = modeNode.get("progress");

                                            if (progressNode.has("encounters") && progressNode.get("encounters").isArray()) {
                                                for (JsonNode encounterNode : progressNode.get("encounters")) {
                                                    if (encounterNode.has("last_kill_timestamp")) {
                                                        long killTime = encounterNode.get("last_kill_timestamp").asLong();

                                                        // Bu hafta öldürüldü mü kontrol et
                                                        if (killTime >= weekStartTime) {
                                                            String bossId = encounterNode.path("encounter").path("id").asText();

                                                            // Bu boss için daha önce daha yüksek zorluk seviyesinde kill var mı?
                                                            String currentHighest = bossHighestDifficulty.getOrDefault(bossId, "none");

                                                            if (compareDifficulty(difficulty, currentHighest) > 0) {
                                                                // Daha yüksek zorluk seviyesi bulundu
                                                                bossHighestDifficulty.put(bossId, difficulty);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calculating boss highest difficulty: {}", e.getMessage(), e);
        }

        return bossHighestDifficulty;
    }

    /**
     * Boss sayılarına göre raid ödüllerini hesaplar
     */
    private String[] calculateRaidRewardsFromCounts(int[] bossCounts) {
        String[] rewards = {"No Reward", "No Reward", "No Reward"};

        // Toplam boss sayısı
        int totalBosses = bossCounts[0];
        // Mythic, heroic ve normal boss sayıları
        int mythicBosses = bossCounts[1];
        int heroicBosses = bossCounts[2];
        int normalBosses = bossCounts[3];

        // Slot 1 (2+ boss) için ödül hesapla
        if (totalBosses >= 2) {
            if (mythicBosses >= 2) {
                rewards[0] = getRaidReward("mythic");
            } else if (heroicBosses + mythicBosses >= 2) {
                rewards[0] = getRaidReward("heroic");
            } else if (normalBosses + heroicBosses + mythicBosses >= 2) {
                rewards[0] = getRaidReward("normal");
            } else {
                rewards[0] = getRaidReward("lfr");
            }
        }

        // Slot 2 (4+ boss) için ödül hesapla
        if (totalBosses >= 4) {
            if (mythicBosses >= 4) {
                rewards[1] = getRaidReward("mythic");
            } else if (heroicBosses + mythicBosses >= 4) {
                rewards[1] = getRaidReward("heroic");
            } else if (normalBosses + heroicBosses + mythicBosses >= 4) {
                rewards[1] = getRaidReward("normal");
            } else {
                rewards[1] = getRaidReward("lfr");
            }
        }

        // Slot 3 (6+ boss) için ödül hesapla
        if (totalBosses >= 6) {
            if (mythicBosses >= 6) {
                rewards[2] = getRaidReward("mythic");
            } else if (heroicBosses + mythicBosses >= 6) {
                rewards[2] = getRaidReward("heroic");
            } else if (normalBosses + heroicBosses + mythicBosses >= 6) {
                rewards[2] = getRaidReward("normal");
            } else {
                rewards[2] = getRaidReward("lfr");
            }
        }

        return rewards;
    }
} 