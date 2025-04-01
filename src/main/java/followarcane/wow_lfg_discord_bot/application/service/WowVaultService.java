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

        embed.setTitle(characterName + " | " + characterRealm + " | Great Vault Preview", profileUrl);
        embed.setThumbnail(thumbnailUrl);
        embed.setColor(Color.decode(classColorCodeHelper.getClassColorCode(characterClass)));

        // M+ Great Vault ödüllerini hesapla
        String[] mythicPlusRewards = calculateMythicPlusRewards(raiderIoData);

        // Raid Great Vault ödüllerini hesapla
        String[] raidRewards = {"No raid data available", "No raid data available", "No raid data available"};
        if (blizzardData != null) {
            raidRewards = calculateRaidRewards(blizzardData, region);
        }

        // Great Vault bilgilerini ekle
        embed.addField("Raid Rewards",
                "**Slot 1 (2 bosses):** " + raidRewards[0] + "\n" +
                        "**Slot 2 (4 bosses):** " + raidRewards[1] + "\n" +
                        "**Slot 3 (6 bosses):** " + raidRewards[2],
                false);

        embed.addField("Mythic+ Rewards", 
                "**Slot 1 (1 run):** " + mythicPlusRewards[0] + "\n" +
                        "**Slot 2 (4 runs):** " + mythicPlusRewards[1] + "\n" +
                        "**Slot 3 (8 runs):** " + mythicPlusRewards[2],
                false);


        // How to Improve kısmını ekle
        StringBuilder howToImprove = new StringBuilder();
        boolean needsImprovement = false;

        try {
            // M+ iyileştirme önerileri
            JsonNode runsNode = raiderIoData.path("mythic_plus_weekly_highest_level_runs");
            List<Integer> runLevels = new ArrayList<>();

            if (!runsNode.isMissingNode() && runsNode.isArray()) {
                for (JsonNode run : runsNode) {
                    runLevels.add(run.get("mythic_level").asInt());
                }

                // Seviyeleri büyükten küçüğe sırala
                Collections.sort(runLevels, Collections.reverseOrder());

                // Slot 1 için öneri
                if (runLevels.isEmpty()) {
                    howToImprove.append("• Complete at least 1 Mythic+ dungeon for Slot 1\n\n");
                    needsImprovement = true;
                }

                // Slot 2 için öneri
                if (runLevels.size() < 4) {
                    howToImprove.append("• Complete ").append(4 - runLevels.size())
                            .append(" more Mythic+ dungeon").append(4 - runLevels.size() > 1 ? "s" : "")
                            .append(" for Slot 2\n\n");
                    needsImprovement = true;
                }

                // Slot 3 için öneri
                if (runLevels.size() < 8) {
                    howToImprove.append("• Complete ").append(8 - runLevels.size())
                            .append(" more Mythic+ dungeon").append(8 - runLevels.size() > 1 ? "s" : "")
                            .append(" for Slot 3\n\n");
                    needsImprovement = true;
                }

                // Ödül seviyesini artırmak için öneri
                if (!runLevels.isEmpty()) {
                    int highestLevel = runLevels.get(0);
                    String currentReward = getVaultReward(highestLevel);
                    String nextReward = getNextBetterReward(highestLevel);

                    if (!currentReward.equals(nextReward)) {
                        int targetLevel = getMinLevelForReward(nextReward);
                        howToImprove.append("• Complete a +").append(targetLevel)
                                .append(" dungeon to upgrade Slot 1 reward to ").append(nextReward).append("\n\n");
                        needsImprovement = true;
                    }

                    if (runLevels.size() >= 4) {
                        int fourthHighestLevel = runLevels.get(3);
                        String currentReward4 = getVaultReward(fourthHighestLevel);
                        String nextReward4 = getNextBetterReward(fourthHighestLevel);

                        if (!currentReward4.equals(nextReward4)) {
                            int targetLevel = getMinLevelForReward(nextReward4);
                            howToImprove.append("• Complete a +").append(targetLevel)
                                    .append(" dungeon to upgrade Slot 2 reward to ").append(nextReward4).append("\n\n");
                            needsImprovement = true;
                        }
                    }

                    if (runLevels.size() >= 8) {
                        int eighthHighestLevel = runLevels.get(7);
                        String currentReward8 = getVaultReward(eighthHighestLevel);
                        String nextReward8 = getNextBetterReward(eighthHighestLevel);

                        if (!currentReward8.equals(nextReward8)) {
                            int targetLevel = getMinLevelForReward(nextReward8);
                            howToImprove.append("• Complete a +").append(targetLevel)
                                    .append(" dungeon to upgrade Slot 3 reward to ").append(nextReward8).append("\n\n");
                            needsImprovement = true;
                        }
                    }
                }
            }

            // Raid iyileştirme önerileri
            if (blizzardData != null) {
                int[] bossesKilledByDifficulty = calculateWeeklyRaidProgress(blizzardData, region);
                int totalBossesKilled = 0;
                String difficultyName = "normal";

                if (bossesKilledByDifficulty[2] > 0) {
                    totalBossesKilled = bossesKilledByDifficulty[2];
                    difficultyName = "mythic";
                } else if (bossesKilledByDifficulty[1] > 0) {
                    totalBossesKilled = bossesKilledByDifficulty[1];
                    difficultyName = "heroic";
                } else {
                    totalBossesKilled = bossesKilledByDifficulty[0];
                }

                if (totalBossesKilled == 0) {
                    howToImprove.append("• Start raiding Liberation of Undermine to unlock raid slots\n\n");
                    needsImprovement = true;
                } else {
                    // Slot 1 için öneri (2 boss)
                    if (totalBossesKilled < 2) {
                        int remaining = 2 - totalBossesKilled;
                        howToImprove.append("• Kill ").append(remaining).append(" more ").append(difficultyName)
                                .append(" boss").append(remaining > 1 ? "es" : "")
                                .append(" to unlock Slot 1 raid reward (").append(getRaidReward(difficultyName)).append(")\n\n");
                        needsImprovement = true;
                    }

                    // Slot 2 için öneri (4 boss)
                    if (totalBossesKilled < 4) {
                        int remaining = 4 - totalBossesKilled;
                        howToImprove.append("• Kill ").append(remaining).append(" more ").append(difficultyName)
                                .append(" boss").append(remaining > 1 ? "es" : "")
                                .append(" to unlock Slot 2 raid reward (").append(getRaidReward(difficultyName)).append(")\n\n");
                        needsImprovement = true;
                    }

                    // Slot 3 için öneri (6 boss)
                    if (totalBossesKilled < 6) {
                        int remaining = 6 - totalBossesKilled;
                        howToImprove.append("• Kill ").append(remaining).append(" more ").append(difficultyName)
                                .append(" boss").append(remaining > 1 ? "es" : "")
                                .append(" to unlock Slot 3 raid reward (").append(getRaidReward(difficultyName)).append(")\n\n");
                        needsImprovement = true;
                    }

                    // Daha yüksek zorluk seviyesi için öneri
                    if (!difficultyName.equals("mythic")) {
                        String nextDifficulty = difficultyName.equals("normal") ? "heroic" : "mythic";
                        howToImprove.append("• Kill bosses in ").append(nextDifficulty)
                                .append(" difficulty to get better rewards (").append(getRaidReward(nextDifficulty)).append(")\n\n");
                        needsImprovement = true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error generating improvement suggestions: {}", e.getMessage(), e);
        }

        if (needsImprovement) {
            embed.addField("How to Improve", howToImprove.toString(), false);
        }

        // Footer ekle
        embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");

        return embed;
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
     * Raid verilerine göre Great Vault ödüllerini hesaplar
     */
    private String[] calculateRaidRewards(JsonNode blizzardData, String region) {
        String[] rewards = new String[3];

        try {
            // Her zorluk seviyesi için bu hafta öldürülen boss sayısını hesapla
            int[] bossesKilledByDifficulty = calculateWeeklyRaidProgress(blizzardData, region);

            // En yüksek zorluk seviyesini ve toplam boss sayısını bul
            String highestDifficulty = "normal";
            int totalBossesKilled = 0;

            if (bossesKilledByDifficulty[2] > 0) {
                highestDifficulty = "mythic";
                totalBossesKilled = bossesKilledByDifficulty[2];
            } else if (bossesKilledByDifficulty[1] > 0) {
                highestDifficulty = "heroic";
                totalBossesKilled = bossesKilledByDifficulty[1];
            } else {
                totalBossesKilled = bossesKilledByDifficulty[0];
            }

            log.info("Highest difficulty: {}, Total bosses killed: {}", highestDifficulty, totalBossesKilled);

            // Ödülleri hesapla
            String rewardText = getRaidReward(highestDifficulty);

            // Slot 1 (2 bosses)
            if (totalBossesKilled >= 2) {
                rewards[0] = rewardText;
            } else if (totalBossesKilled > 0) {
                rewards[0] = "No Reward";
            } else {
                rewards[0] = "No Reward";
            }

            // Slot 2 (4 bosses)
            if (totalBossesKilled >= 4) {
                rewards[1] = rewardText;
            } else if (totalBossesKilled > 0) {
                rewards[1] = "No Reward";
            } else {
                rewards[1] = "No Reward";
            }

            // Slot 3 (6 bosses)
            if (totalBossesKilled >= 6) {
                rewards[2] = rewardText;
            } else if (totalBossesKilled > 0) {
                rewards[2] = "No Reward";
            } else {
                rewards[2] = "No Reward";
            }

            return rewards;
        } catch (Exception e) {
            log.error("Error calculating raid vault rewards: {}", e.getMessage(), e);
        }

        // Varsayılan değerler
        rewards[0] = "No raid data available";
        rewards[1] = "No raid data available";
        rewards[2] = "No raid data available";

        return rewards;
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
        else if (mythicLevel == 10) return "Myth 1 (662)";
        else if (mythicLevel == 11) return "Myth 1 (662)";
        else if (mythicLevel == 12) return "Myth 1 (662)";
        else if (mythicLevel >= 13) return "Myth 1 (662)";
        else return "Unknown";
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
                return "Unknown reward";
        }
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
} 