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
import java.util.*;
import java.util.List;

@Service
@Slf4j
public class WowVaultService {

    private final RestTemplate restTemplate;
    private final ClassColorCodeHelper classColorCodeHelper;
    private final BattleNetApiService battleNetApiService;

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

        // Raid ödüllerinin durumunu göster
        String slot1Status = raidBossCounts[0] >= 2 ? "✅" : "🔒";
        String slot2Status = raidBossCounts[0] >= 4 ? "✅" : "🔒";
        String slot3Status = raidBossCounts[0] >= 6 ? "✅" : "🔒";

        // İlerleme durumunu göster
        String raid1Progress = raidBossCounts[0] >= 2 ? "2/2" : raidBossCounts[0] + "/2";
        String raid2Progress = raidBossCounts[0] >= 4 ? "4/4" : raidBossCounts[0] + "/4";
        String raid3Progress = raidBossCounts[0] >= 6 ? "6/6" : raidBossCounts[0] + "/6";

        // String.format kullanarak sabit genişlikli alanlar oluştur
        raidSection.append("Slot 1: Defeat 2 Bosses\n");
        raidSection.append(slot1Status + " " + raid1Progress);
        if (!raidRewards[0].equals("No Reward")) {
            raidSection.append(" → " + raidRewards[0]);
        }
        raidSection.append("\n\n");

        raidSection.append("Slot 2: Defeat 4 Bosses\n");
        raidSection.append(slot2Status + " " + raid2Progress);
        if (!raidRewards[1].equals("No Reward")) {
            raidSection.append(" → " + raidRewards[1]);
        }
        raidSection.append("\n\n");

        raidSection.append("Slot 3: Defeat 6 Bosses\n");
        raidSection.append(slot3Status + " " + raid3Progress);
        if (!raidRewards[2].equals("No Reward")) {
            raidSection.append(" → " + raidRewards[2]);
        }
        raidSection.append("\n");

        // Mythic+ bölümü
        StringBuilder dungeonSection = new StringBuilder();
        dungeonSection.append("**Dungeons**\n");

        // M+ ödüllerinin durumunu göster
        String m1Status = totalMythicPlusRuns >= 1 ? "✅" : "🔒";
        String m2Status = totalMythicPlusRuns >= 4 ? "✅" : "🔒";
        String m3Status = totalMythicPlusRuns >= 8 ? "✅" : "🔒";

        // İlerleme durumunu göster (tamamlanan slotlar için max değeri göster)
        String dungeon1Progress = totalMythicPlusRuns >= 1 ? "1/1" : totalMythicPlusRuns + "/1";
        String dungeon2Progress = totalMythicPlusRuns >= 4 ? "4/4" : totalMythicPlusRuns + "/4";
        String dungeon3Progress = totalMythicPlusRuns >= 8 ? "8/8" : totalMythicPlusRuns + "/8";

        // Her slot için ayrı bir satır oluştur
        dungeonSection.append("Slot 1: Complete 1 Dungeon\n");
        dungeonSection.append(m1Status + " " + dungeon1Progress);
        if (!mythicPlusRewards[0].equals("No Reward")) {
            dungeonSection.append(" → " + mythicPlusRewards[0]);
        }
        dungeonSection.append("\n\n");

        dungeonSection.append("Slot 2: Complete 4 Dungeons\n");
        dungeonSection.append(m2Status + " " + dungeon2Progress);
        if (!mythicPlusRewards[1].equals("No Reward")) {
            dungeonSection.append(" → " + mythicPlusRewards[1]);
        }
        dungeonSection.append("\n\n");

        dungeonSection.append("Slot 3: Complete 8 Dungeons\n");
        dungeonSection.append(m3Status + " " + dungeon3Progress);
        if (!mythicPlusRewards[2].equals("No Reward")) {
            dungeonSection.append(" → " + mythicPlusRewards[2]);
        }
        dungeonSection.append("\n");

        // World bölümü
        StringBuilder worldSection = new StringBuilder();
        worldSection.append("**World**\n");

        // World slotları için bilgi
        worldSection.append("Slot 1: Complete 2 Activities\n");
        worldSection.append("🔒 0/2");
        worldSection.append(" → Coming Soon");
        worldSection.append("\n\n");

        worldSection.append("Slot 2: Complete 4 Activities\n");
        worldSection.append("🔒 0/4");
        worldSection.append(" → Coming Soon");
        worldSection.append("\n\n");

        worldSection.append("Slot 3: Complete 8 Activities\n");
        worldSection.append("🔒 0/8");
        worldSection.append(" → Coming Soon");
        worldSection.append("\n\n");

        worldSection.append("Note: World activities tracking will be implemented soon!");

        // Alanları ekle
        embed.addField("", raidSection.toString(), false);
        embed.addField("", dungeonSection.toString(), false);
        embed.addField("", worldSection.toString(), false);

        // Karakter bilgilerini ekle
        embed.setThumbnail(thumbnailUrl);
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
} 