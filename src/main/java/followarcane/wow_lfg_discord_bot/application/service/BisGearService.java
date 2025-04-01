package followarcane.wow_lfg_discord_bot.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BisGearService {
    private static final Logger logger = LoggerFactory.getLogger(BisGearService.class);
    private final ClassColorCodeHelper classColorCodeHelper;
    private final Map<String, Map<String, Map<String, Object>>> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public BisGearService(ClassColorCodeHelper classColorCodeHelper) {
        this.classColorCodeHelper = classColorCodeHelper;
        loadBisData();
    }

    private void loadBisData() {
        try {
            InputStream inputStream = new ClassPathResource("bis_data.json").getInputStream();
            Map<String, Map<String, Map<String, Object>>> bisData = objectMapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, Map<String, Map<String, Object>>>>() {
                    }
            );
            cache.putAll(bisData);
            logger.info("BIS data loaded successfully. Cache size: {}", cache.size());
        } catch (IOException e) {
            logger.error("Error loading BIS data", e);
        }
    }

    public void refreshCache() {
        cache.clear();
        loadBisData();
        logger.info("BIS data cache refreshed. New cache size: {}", cache.size());
    }

    public Map<String, Map<String, Object>> getBisGear(String className, String specName, String heroTalent) {
        // Format class and spec names
        String formattedClassName = formatName(className);
        String formattedSpecName = formatName(specName);
        String formattedHeroTalent = formatName(heroTalent);

        // If hero talent is specified, get BIS gear for that specific hero talent
        if (formattedHeroTalent != null && !formattedHeroTalent.isEmpty()) {
            String key = buildKey(formattedClassName, formattedSpecName, formattedHeroTalent);
            logger.info("Looking for specific hero talent key: {} in cache", key);

            if (cache.containsKey(key)) {
                return cache.get(key);
            } else {
                logger.info("Key not found: {}. Available keys: {}", key, cache.keySet());
                return new HashMap<>();
            }
        }

        // If hero talent is not specified, get BIS gear for all hero talents of that class and spec
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (String key : cache.keySet()) {
            // Check if the key starts with the class and spec
            if (key.startsWith(formattedClassName + "_" + formattedSpecName)) {
                logger.info("Found matching key: {}", key);

                // Get BIS gear for this hero talent
                Map<String, Map<String, Object>> bisGear = cache.get(key);

                // Add hero talent name to each item's source
                String heroTalentName = key.substring((formattedClassName + "_" + formattedSpecName + "_").length());

                // Add items to result
                for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
                    String slot = entry.getKey();
                    Map<String, Object> itemInfo = new HashMap<>(entry.getValue());

                    // Add hero talent to source
                    //String source = (String) itemInfo.get("source");
                    //itemInfo.put("source", source + " (Hero Talent: " + heroTalentName + ")");

                    // Add to result
                    result.put(slot, itemInfo);
                }
            }
        }

        if (result.isEmpty()) {
            logger.info("No BIS gear found for class: {} and spec: {}", formattedClassName, formattedSpecName);
        }

        return result;
    }

    public Map<String, Object> getBisGearForSlot(String slot, String className, String specName, String heroTalent) {
        // Format slot name (only capitalize first letter, preserve spaces)
        String formattedSlot = formatName(slot);

        logger.info("Looking for slot: {} (formatted: {}) in bisGear", slot, formattedSlot);

        // Get BIS gear for all hero talents
        Map<String, Map<String, Object>> bisGear = getBisGear(className, specName, heroTalent);

        // If slot is specified, get BIS gear for that specific slot
        if (!formattedSlot.isEmpty()) {
            logger.info("Available slots: {}", bisGear.keySet());

            // Weapon/Main Hand için özel kontrol
            if (formattedSlot.equalsIgnoreCase("Weapon") ||
                    formattedSlot.equalsIgnoreCase("Weapon1") ||
                    formattedSlot.equalsIgnoreCase("Mainhand") ||
                    formattedSlot.equalsIgnoreCase("Main-hand") ||
                    formattedSlot.equalsIgnoreCase("Main_hand") || formattedSlot.equalsIgnoreCase("Main hand") ||
                    formattedSlot.replace("_", " ").equalsIgnoreCase("Main Hand")) {

                // Main Hand slotunu ara
                for (String key : bisGear.keySet()) {
                    if (key.equalsIgnoreCase("Main Hand")) {
                        logger.info("Found Main Hand slot for weapon query: {}", formattedSlot);
                        return bisGear.get(key);
                    }
                }
            }
            
            // Özel durumlar için kontrol
            if (formattedSlot.equalsIgnoreCase("Trinket")) {
                // Tüm trinketleri içeren bir map oluştur
                Map<String, Object> allTrinkets = new HashMap<>();
                for (String key : bisGear.keySet()) {
                    if (key.startsWith("Trinket")) {
                        allTrinkets.put(key, bisGear.get(key));
                    }
                }

                if (!allTrinkets.isEmpty()) {
                    logger.info("Found {} trinkets", allTrinkets.size());
                    return allTrinkets;
                }
            } else if (formattedSlot.equalsIgnoreCase("Finger") || formattedSlot.equalsIgnoreCase("Ring")) {
                // Tüm yüzükleri içeren bir map oluştur
                Map<String, Object> allRings = new HashMap<>();
                for (String key : bisGear.keySet()) {
                    if (key.startsWith("Finger")) {
                        allRings.put(key, bisGear.get(key));
                    }
                }

                if (!allRings.isEmpty()) {
                    logger.info("Found {} rings", allRings.size());
                    return allRings;
                }
            }
            
            // Try exact match first
            if (bisGear.containsKey(formattedSlot)) {
                return bisGear.get(formattedSlot);
            }

            // If exact match fails, try case-insensitive match
            for (String key : bisGear.keySet()) {
                if (key.equalsIgnoreCase(formattedSlot)) {
                    logger.info("Found case-insensitive match for slot: {} -> {}", formattedSlot, key);
                    return bisGear.get(key);
                }
            }

            // If case-insensitive match fails, try partial match
            for (String key : bisGear.keySet()) {
                if (key.toLowerCase().contains(formattedSlot.toLowerCase())) {
                    logger.info("Found partial match for slot: {} -> {}", formattedSlot, key);
                    return bisGear.get(key);
                }
            }

            // If all else fails, try matching without spaces or with spaces
            for (String key : bisGear.keySet()) {
                String keyNoSpaces = key.replace(" ", "").toLowerCase();
                String slotNoSpaces = formattedSlot.replace(" ", "").toLowerCase();

                if (keyNoSpaces.equals(slotNoSpaces) ||
                        key.replace(" ", "_").equalsIgnoreCase(formattedSlot) ||
                        formattedSlot.replace("_", " ").equalsIgnoreCase(key)) {
                    logger.info("Found match by removing/replacing spaces: {} -> {}", formattedSlot, key);
                    return bisGear.get(key);
                }
            }
            
            logger.info("No match found for slot: {}", formattedSlot);
            return new HashMap<>();
        }

        // If slot is not specified, return all slots
        return new HashMap<>();
    }

    private String formatName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        // İlk harfi büyük, geri kalanı küçük yap
        String returnValue = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();

        // Weapon/Main Hand için özel kontrol
        if (returnValue.equalsIgnoreCase("Weapon") ||
                returnValue.equalsIgnoreCase("Weapon1") ||
                returnValue.equalsIgnoreCase("Mainhand") ||
                returnValue.equalsIgnoreCase("Main-hand") ||
                returnValue.equalsIgnoreCase("Main_hand") || returnValue.equalsIgnoreCase("Main hand") ||
                returnValue.replace("_", " ").equalsIgnoreCase("Main Hand")) {
            returnValue = "Main Hand";
        }
        return returnValue;
    }

    private String formatClassName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        // İlk harfi büyük, geri kalanı küçük yap
        String formatted = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
        // Boşlukları alt çizgi ile değiştir
        formatted = formatted.replace(" ", "_");

        return formatted;
    }

    private String buildKey(String className, String specName, String heroTalent) {
        logger.info("Building key from: className={}, specName={}, heroTalent={}", className, specName, heroTalent);

        // Sınıf ve spec adlarını formatla (boşlukları alt çizgi ile değiştir)
        String formattedClassName = formatClassName(className);
        String formattedSpecName = formatClassName(specName);
        String formattedHeroTalent = formatClassName(heroTalent);

        // Anahtar oluştur
        String key = formattedClassName;
        if (!formattedSpecName.isEmpty()) {
            key += "_" + formattedSpecName;

            if (!formattedHeroTalent.isEmpty()) {
                key += "_" + formattedHeroTalent;
            }
        }

        logger.info("Generated key: {}", key);
        
        return key;
    }

    public Map<String, String> fetchBisGearWithSelenium(String slot, String className, String specName, String heroTalent) {
        // Bu metod sadece test amaçlı olarak burada bulunuyor
        // Gerçek uygulamada Selenium kullanmıyoruz, önceden hazırlanmış JSON dosyasını kullanıyoruz
        Map<String, Object> itemInfo = getBisGearForSlot(slot, className, specName, heroTalent);
        Map<String, String> result = new HashMap<>();

        if (itemInfo != null && !itemInfo.isEmpty()) {
            result.put("name", (String) itemInfo.get("name"));
            result.put("url", (String) itemInfo.get("url"));
            //result.put("source", (String) itemInfo.get("source"));
            result.put("stats", (String) itemInfo.get("stats"));
        }

        return result;
    }

    /**
     * Belirli bir slot, sınıf, spec ve hero talent için BIS ekipman bilgilerini içeren bir Discord embed oluşturur.
     *
     * @param slot Ekipman slotu (Head, Neck, Shoulders, vb.)
     * @param className Karakter sınıfı (Death_Knight, Mage, vb.)
     * @param specName Spec adı (Blood, Frost, vb.)
     * @param heroTalent Hero talent adı (Deathbringer, Frostfire, vb.)
     * @return BIS ekipman bilgilerini içeren bir EmbedBuilder
     */
    public EmbedBuilder createBisGearEmbed(String slot, String className, String specName, String heroTalent) {
        // Format names
        String formattedClassName = formatName(className);
        String formattedSpecName = formatName(specName);
        String formattedHeroTalent = formatName(heroTalent);
        String formattedSlot = formatName(slot);
        
        // Sınıf rengini al
        Color classColor;
        try {
            classColor = Color.decode(classColorCodeHelper.getClassColorCode(formattedClassName));
            logger.info("Using class color for {}: {}", formattedClassName, classColor);
        } catch (Exception e) {
            logger.warn("Could not get class color for {}. Using default color.", formattedClassName);
            classColor = Color.GRAY; // Varsayılan renk
        }
        
        // Embed oluştur
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(classColor);

        // Başlık oluştur
        String title = formattedSlot.isEmpty() ? "All Slots" : formattedSlot;

        String specTitle = formattedSpecName.isEmpty() ? "All Specs" : formattedSpecName;
        if (!formattedHeroTalent.isEmpty()) {
            specTitle += " (" + formattedHeroTalent + ")";
        }

        embed.setTitle("BIS Gear for " + formattedClassName.replace("_", " ") + " " + specTitle + " - " + title);

        // BIS ekipman bilgilerini al
        if (formattedSlot.isEmpty()) {
            // Tüm slotlar için BIS ekipmanları göster
            if (!formattedHeroTalent.isEmpty()) {
                // Belirli bir hero talent için tüm slotları göster
                Map<String, Map<String, Object>> allGear = getBisGear(className, specName, heroTalent);

                if (allGear.isEmpty()) {
                    embed.setDescription("No BIS gear information found for this spec and hero talent.");
                } else {
                    // Slotları gruplandır (her 5 slot için bir field)
                    StringBuilder fieldContent = new StringBuilder();
                    int slotCount = 0;
                    int fieldCount = 1;

                    for (Map.Entry<String, Map<String, Object>> entry : allGear.entrySet()) {
                        String slotName = entry.getKey();
                        Map<String, Object> slotInfo = entry.getValue();

                        String itemName = (String) slotInfo.get("name");
                        String itemUrl = (String) slotInfo.get("url");

                        fieldContent.append("**").append(slotName).append("**: [").append(itemName).append("](").append(itemUrl).append(")\n\n");
                        
                        slotCount++;

                        // Her 5 slot için yeni bir field oluştur
                        if (slotCount % 5 == 0 || slotCount == allGear.size()) {
                            embed.addField("Slots (Part " + fieldCount + ")", fieldContent.toString(), false);
                            fieldContent = new StringBuilder();
                            fieldCount++;
                        }
                    }
                }
            } else {
                // Hero talent belirtilmediğinde, her hero talent için tüm slotları gruplandırarak göster
                boolean foundAny = false;
                int totalCharCount = 0; // Toplam karakter sayısını takip et
                int maxHeroTalents = 3; // En fazla gösterilecek hero talent sayısı
                int heroTalentCount = 0;
                
                for (String key : cache.keySet()) {
                    // Check if the key starts with the class and spec
                    if (key.startsWith(formattedClassName + "_" + formattedSpecName + "_")) {
                        foundAny = true;
                        heroTalentCount++;

                        // Karakter sınırını aşmamak için en fazla 3 hero talent göster
                        if (heroTalentCount > maxHeroTalents) {
                            embed.addField("More Hero Talents", "There are more hero talents available. Please specify a hero talent to see its BIS gear.", false);
                            break;
                        }
                        
                        // Extract hero talent name from key
                        String heroTalentName = key.substring((formattedClassName + "_" + formattedSpecName + "_").length());

                        // Add a section for this hero talent
                        embed.addField("Hero Talent: " + heroTalentName, "BIS gear for " + formattedClassName + " " + formattedSpecName + " with " + heroTalentName + " hero talent", false);
                        totalCharCount += heroTalentName.length() + formattedClassName.length() + formattedSpecName.length() + 50;
                        
                        // Get BIS gear for this hero talent
                        Map<String, Map<String, Object>> bisGear = cache.get(key);

                        // Slotları gruplandır (her 5 slot için bir field)
                        StringBuilder fieldContent = new StringBuilder();
                        int slotCount = 0;
                        int fieldCount = 1;
                        int maxSlotsPerHeroTalent = 10; // Her hero talent için en fazla gösterilecek slot sayısı
                        
                        for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
                            if (slotCount >= maxSlotsPerHeroTalent) {
                                embed.addField("More Slots", "There are more slots available. Please specify a slot to see its BIS gear.", false);
                                break;
                            }
                            
                            String slotName = entry.getKey();
                            Map<String, Object> slotInfo = entry.getValue();

                            String itemName = (String) slotInfo.get("name");
                            String itemUrl = (String) slotInfo.get("url");

                            String slotContent = "**" + slotName + "**: [" + itemName + "](" + itemUrl + ")\n\n";

                            // Karakter sınırını kontrol et
                            if (totalCharCount + slotContent.length() > 5500) { // 6000'e yaklaşıyoruz, güvenli bir sınır koy
                                embed.addField("Character Limit Reached", "Too many items to display. Please specify a hero talent or slot to see more details.", false);
                                break;
                            }

                            fieldContent.append(slotContent);
                            totalCharCount += slotContent.length();
                            slotCount++;

                            // Her 5 slot için yeni bir field oluştur
                            if (slotCount % 5 == 0 || slotCount == Math.min(maxSlotsPerHeroTalent, bisGear.size())) {
                                embed.addField("Slots (Part " + fieldCount + ")", fieldContent.toString(), false);
                                fieldContent = new StringBuilder();
                                fieldCount++;
                            }
                        }
                    }
                }

                if (!foundAny) {
                    embed.setDescription("No BIS gear information found for this spec.");
                }
            }
        } else {
            // Belirli bir slot için BIS ekipmanı göster
            if (!formattedHeroTalent.isEmpty()) {
                // Belirli bir hero talent için belirli bir slotu göster
                Map<String, Object> itemInfo = getBisGearForSlot(slot, className, specName, heroTalent);

                if (itemInfo.isEmpty()) {
                    embed.setDescription("No BIS gear information found for this slot, spec, and hero talent.");
                } else if (formattedSlot.equalsIgnoreCase("Trinket") || formattedSlot.equalsIgnoreCase("Finger") || formattedSlot.equalsIgnoreCase("Ring")) {
                    // Birden fazla trinket veya yüzük için
                    for (Map.Entry<String, Object> entry : itemInfo.entrySet()) {
                        String slotName = entry.getKey();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> slotInfo = (Map<String, Object>) entry.getValue();

                        String itemName = (String) slotInfo.get("name");
                        String itemUrl = (String) slotInfo.get("url");
                        String stats = (String) slotInfo.get("stats");

                        embed.addField(slotName, "[" + itemName + "](" + itemUrl + ")", false);

                        if (stats != null && !stats.isEmpty()) {
                            // Stats bilgisi çok uzun olabilir, bu yüzden kısaltıyoruz
                            stats = truncateStats(stats);
                            embed.addField(slotName + " Stats", stats + "\n\n", false);
                        }
                    }
                } else {
                    // Tek bir slot için
                    String itemName = (String) itemInfo.get("name");
                    String itemUrl = (String) itemInfo.get("url");
                    String stats = (String) itemInfo.get("stats");

                    embed.addField("Item", "[" + itemName + "](" + itemUrl + ")", false);

                    if (stats != null && !stats.isEmpty()) {
                        // Stats bilgisi çok uzun olabilir, bu yüzden kısaltıyoruz
                        stats = truncateStats(stats);
                        embed.addField("Stats", stats + "\n\n", false);
                    }
                }
            } else {
                // Hero talent belirtilmediğinde, her hero talent için belirli bir slotu göster
                boolean foundAny = false;

                for (String key : cache.keySet()) {
                    // Check if the key starts with the class and spec
                    if (key.startsWith(formattedClassName + "_" + formattedSpecName + "_")) {
                        // Extract hero talent name from key
                        String heroTalentName = key.substring((formattedClassName + "_" + formattedSpecName + "_").length());

                        // Get BIS gear for this hero talent
                        Map<String, Map<String, Object>> bisGear = cache.get(key);

                        // Özel durumlar için kontrol
                        if (formattedSlot.equalsIgnoreCase("Trinket")) {
                            boolean foundTrinket = false;
                            for (String slotKey : bisGear.keySet()) {
                                if (slotKey.startsWith("Trinket")) {
                                    if (!foundTrinket) {
                                        foundAny = true;
                                        foundTrinket = true;
                                        embed.addField("Hero Talent: " + heroTalentName, "BIS trinkets for " + formattedClassName + " " + formattedSpecName + " with " + heroTalentName + " hero talent", false);
                                    }

                                    Map<String, Object> itemInfo = bisGear.get(slotKey);
                                    String itemName = (String) itemInfo.get("name");
                                    String itemUrl = (String) itemInfo.get("url");
                                    String stats = (String) itemInfo.get("stats");

                                    embed.addField(slotKey, "[" + itemName + "](" + itemUrl + ")", false);

                                    if (stats != null && !stats.isEmpty()) {
                                        if (stats.length() > 1000) {
                                            stats = stats.substring(0, 990) + "...\n\n";
                                        }
                                        embed.addField(slotKey + " Stats", stats + "\n\n", false);
                                    }
                                }
                            }
                        } else if (formattedSlot.equalsIgnoreCase("Finger") || formattedSlot.equalsIgnoreCase("Ring")) {
                            boolean foundRing = false;
                            for (String slotKey : bisGear.keySet()) {
                                if (slotKey.startsWith("Finger")) {
                                    if (!foundRing) {
                                        foundAny = true;
                                        foundRing = true;
                                        embed.addField("Hero Talent: " + heroTalentName, "BIS rings for " + formattedClassName + " " + formattedSpecName + " with " + heroTalentName + " hero talent", false);
                                    }

                                    Map<String, Object> itemInfo = bisGear.get(slotKey);
                                    String itemName = (String) itemInfo.get("name");
                                    String itemUrl = (String) itemInfo.get("url");
                                    String stats = (String) itemInfo.get("stats");

                                    embed.addField(slotKey, "[" + itemName + "](" + itemUrl + ")", false);

                                    if (stats != null && !stats.isEmpty()) {
                                        if (stats.length() > 1000) {
                                            stats = stats.substring(0, 990) + "...\n\n";
                                        }
                                        embed.addField(slotKey + " Stats", stats + "\n\n", false);
                                    }
                                }
                            }
                        } else {
                            // Check if this hero talent has the specified slot
                            if (bisGear.containsKey(formattedSlot)) {
                                foundAny = true;

                                // Add a section for this hero talent
                                embed.addField("Hero Talent: " + heroTalentName, "BIS gear for " + formattedClassName + " " + formattedSpecName + " with " + heroTalentName + " hero talent", false);

                                // Get item info
                                Map<String, Object> itemInfo = bisGear.get(formattedSlot);

                                String itemName = (String) itemInfo.get("name");
                                String itemUrl = (String) itemInfo.get("url");
                                String stats = (String) itemInfo.get("stats");

                                embed.addField("Item", "[" + itemName + "](" + itemUrl + ")", false);

                                if (stats != null && !stats.isEmpty()) {
                                    // Stats bilgisi çok uzun olabilir, bu yüzden kısaltıyoruz
                                    stats = truncateStats(stats);
                                    embed.addField("Stats", stats + "\n\n", false);
                                }
                            }
                        }
                    }
                }

                if (!foundAny) {
                    embed.setDescription("No BIS gear information found for this slot and spec.");
                }
            }
        }

        embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");
        
        return embed;
    }

    // Stats bilgisini kısalt ve düzenle
    private String truncateStats(String stats) {
        if (stats == null || stats.isEmpty()) {
            return "";
        }

        logger.info("Original stats: {}", stats);
        
        StringBuilder cleanStats = new StringBuilder();

        // İlevel bilgisini çıkar
        if (stats.contains("ilevel:")) {
            int ilvlStart = stats.indexOf("ilevel:");
            int ilvlEnd = stats.indexOf(",", ilvlStart);
            if (ilvlEnd > ilvlStart) {
                cleanStats.append(stats.substring(ilvlStart, ilvlEnd).trim()).append("\n");
            }
        }

        // Primary statları çıkar (Int, Agi, Str)
        if (stats.contains("Int") || stats.contains("Agi") || stats.contains("Str")) {
            String primaryStats = extractStat(stats, "Int") +
                    extractStat(stats, "Agi") +
                    extractStat(stats, "Str");
            if (!primaryStats.isEmpty()) {
                cleanStats.append("Primary: ").append(primaryStats.trim());
                // Son virgülü kaldır
                if (primaryStats.trim().endsWith(",")) {
                    cleanStats.delete(cleanStats.length() - 1, cleanStats.length());
                }
                cleanStats.append("\n");
            }
        }

        // Secondary statları çıkar
        String secondaryStats = extractStat(stats, "Haste") +
                extractStat(stats, "Mastery") +
                extractStat(stats, "Crit") +
                extractStat(stats, "Vers");
        if (!secondaryStats.isEmpty()) {
            cleanStats.append("Secondary: ").append(secondaryStats.trim());
            // Son virgülü kaldır
            if (secondaryStats.trim().endsWith(",")) {
                cleanStats.delete(cleanStats.length() - 1, cleanStats.length());
            }
            cleanStats.append("\n");
        }

        // Gem bilgilerini çıkar
        if (stats.contains("gems:")) {
            int gemsStart = stats.indexOf("gems:");
            int gemsEnd = stats.indexOf("}", gemsStart);
            if (gemsEnd > gemsStart) {
                String gemInfo = stats.substring(gemsStart, gemsEnd + 1)
                        .replace("gems:", "Gems:")
                        .replace("{", "")
                        .replace("}", "")
                        .trim();
                cleanStats.append(gemInfo);
            }
        }

        String result = cleanStats.toString().trim();

        // Eğer hiçbir şey bulunamadıysa, orijinal stats'ı kısalt
        if (result.isEmpty()) {
            logger.warn("Could not extract formatted stats, using original stats");
            if (stats.length() > 900) {
                return stats.substring(0, 897) + "...";
            }
            return stats;
        }

        logger.info("Formatted stats: {}", result);
        
        // Discord'un 1024 karakter sınırlaması var
        // Güvenli olmak için 900 karakterle sınırlayalım
        if (result.length() > 900) {
            return result.substring(0, 897) + "...";
        }

        return result;
    }

    // Belirli bir statı çıkarmak için yardımcı metod
    private String extractStat(String stats, String statName) {
        if (stats == null || !stats.contains(statName)) {
            return "";
        }

        try {
            int statIndex = stats.indexOf(statName);
            if (statIndex == -1) return "";

            // Stat adından önce gelen sayıyı bul
            int numStart = statIndex;
            while (numStart > 0 && (Character.isDigit(stats.charAt(numStart - 1)) || stats.charAt(numStart - 1) == ',' || stats.charAt(numStart - 1) == '+' || stats.charAt(numStart - 1) == '.')) {
                numStart--;
            }

            if (numStart < statIndex) {
                // Sayı ve stat adını birleştir
                return stats.substring(numStart, statIndex + statName.length()) + ", ";
            }
        } catch (Exception e) {
            logger.warn("Error extracting stat {}: {}", statName, e.getMessage());
        }

        return "";
    }

    public List<EmbedBuilder> createBisGearEmbedsForAllHeroTalents(String className, String specName) {
        List<EmbedBuilder> embeds = new ArrayList<>();
        String formattedClassName = formatName(className);
        String formattedSpecName = formatName(specName);

        // Beast Mastery için özel kontrol
        if (formattedSpecName.equalsIgnoreCase("Beast_mastery") ||
                formattedSpecName.equalsIgnoreCase("Beast mastery") ||
                formattedSpecName.equalsIgnoreCase("Beastmastery")) {
            formattedSpecName = "Beast_Mastery";
        }

        // Sınıf ve spec için tüm hero talent'ları bul
        for (String key : cache.keySet()) {
            if (key.startsWith(formattedClassName + "_" + formattedSpecName + "_")) {
                // Extract hero talent name from key
                String heroTalentName = key.substring((formattedClassName + "_" + formattedSpecName + "_").length());

                // Bu hero talent için bir embed oluştur
                EmbedBuilder embed = new EmbedBuilder();

                // Sınıf rengini al
                Color classColor;
                try {
                    classColor = Color.decode(classColorCodeHelper.getClassColorCode(formattedClassName));
                    logger.info("Using class color for {}: {}", formattedClassName, classColor);
                } catch (Exception e) {
                    logger.warn("Could not get class color for {}. Using default color.", formattedClassName);
                    classColor = Color.GRAY; // Varsayılan renk
                }

                embed.setColor(classColor);

                // Başlık oluştur
                embed.setTitle("BIS Gear for " + formattedClassName + " " + formattedSpecName + " (" + heroTalentName + ")");

                // BIS ekipman bilgilerini al
                Map<String, Map<String, Object>> bisGear = cache.get(key);

                // Her slot için ayrı bir field oluştur
                for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
                    String slotName = entry.getKey();
                    Map<String, Object> slotInfo = entry.getValue();

                    String itemName = (String) slotInfo.get("name");
                    String itemUrl = (String) slotInfo.get("url");

                    embed.addField(slotName, "[" + itemName + "](" + itemUrl + ")", false);
                }

                embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");
                
                embeds.add(embed);
            }
        }

        return embeds;
    }
} 