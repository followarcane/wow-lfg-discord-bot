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
     * Eğer hero talent belirtilmemişse, tüm hero talent'lar için ayrı ayrı embed'ler döndürür.
     *
     * @param slot Ekipman slotu (Head, Neck, Shoulders, vb.)
     * @param className Karakter sınıfı (Death_Knight, Mage, vb.)
     * @param specName Spec adı (Blood, Frost, vb.)
     * @param heroTalent Hero talent adı (Deathbringer, Frostfire, vb.)
     * @return BIS ekipman bilgilerini içeren bir EmbedBuilder listesi
     */
    public List<EmbedBuilder> createBisGearEmbed(String slot, String className, String specName, String heroTalent) {
        List<EmbedBuilder> embeds = new ArrayList<>();
        
        // Format names
        String formattedClassName = formatName(className);
        String formattedSpecName = formatName(specName);
        String formattedHeroTalent = formatName(heroTalent);
        String formattedSlot = formatName(slot);

        // Hero talent belirtilmemişse, tüm hero talent'lar için ayrı ayrı embed oluştur
        if (formattedHeroTalent == null || formattedHeroTalent.isEmpty()) {
            return createBisGearEmbedsForAllHeroTalents(className, specName, slot);
        }
        
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
            Map<String, Map<String, Object>> allGear = getBisGear(className, specName, heroTalent);

            if (allGear.isEmpty()) {
                embed.setDescription("No BIS gear information found for this spec and hero talent.");
            } else {
                // Her slot için ayrı bir field oluştur
                for (Map.Entry<String, Map<String, Object>> entry : allGear.entrySet()) {
                    String slotName = entry.getKey();
                    Map<String, Object> slotInfo = entry.getValue();

                    String itemName = (String) slotInfo.get("name");
                    String itemUrl = (String) slotInfo.get("url");
                    String stats = (String) slotInfo.get("stats");

                    StringBuilder fieldContent = new StringBuilder();
                    fieldContent.append("[").append(itemName).append("](").append(itemUrl).append(")");

                    // Stats bilgisini ekle (eğer varsa)
                    if (stats != null && !stats.isEmpty()) {
                        stats = truncateStats(stats);
                        if (!stats.isEmpty()) {
                            fieldContent.append("\n").append(stats).append("\n");
                        }
                    }

                    embed.addField(slotName, fieldContent.toString(), false);
                }
            }
        } else {
            // Belirli bir slot için BIS ekipmanı göster
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

                    StringBuilder fieldContent = new StringBuilder();
                    fieldContent.append("[").append(itemName).append("](").append(itemUrl).append(")");

                    if (stats != null && !stats.isEmpty()) {
                        stats = truncateStats(stats);
                        if (!stats.isEmpty()) {
                            fieldContent.append("\n").append(stats).append("\n");
                        }
                    }

                    embed.addField(slotName, fieldContent.toString(), false);
                }
            } else {
                // Tek bir slot için
                String itemName = (String) itemInfo.get("name");
                String itemUrl = (String) itemInfo.get("url");
                String stats = (String) itemInfo.get("stats");

                StringBuilder fieldContent = new StringBuilder();
                fieldContent.append("[").append(itemName).append("](").append(itemUrl).append(")");

                if (stats != null && !stats.isEmpty()) {
                    stats = truncateStats(stats);
                    if (!stats.isEmpty()) {
                        fieldContent.append("\n").append(stats).append("\n");
                    }
                }

                embed.addField("Item", fieldContent.toString(), false);
            }
        }

        embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");

        embeds.add(embed);
        return embeds;
    }

    // Stats bilgisini kısalt ve düzenle
    private String truncateStats(String stats) {
        if (stats == null || stats.isEmpty()) {
            return "";
        }
        
        StringBuilder cleanStats = new StringBuilder();

        // İlevel bilgisini çıkar ama gösterme
        if (stats.contains("ilevel:")) {
            int ilvlStart = stats.indexOf("ilevel:");
            int ilvlEnd = stats.indexOf(",", ilvlStart);
            if (ilvlEnd > ilvlStart) {
                // İlevel bilgisini atla, gösterme
                // cleanStats.append(stats.substring(ilvlStart, ilvlEnd).trim()).append("\n");
            }
        }

        // Stats bölümünü çıkar
        if (stats.contains("stats:")) {
            try {
                int statsStart = stats.indexOf("stats:");
                int statsEnd = -1;

                // stats: sonrası ilk { karakterini bul
                int openBrace = stats.indexOf("{", statsStart);
                if (openBrace != -1) {
                    // Eşleşen } karakterini bul
                    int braceCount = 1;
                    for (int i = openBrace + 1; i < stats.length(); i++) {
                        if (stats.charAt(i) == '{') braceCount++;
                        if (stats.charAt(i) == '}') braceCount--;

                        if (braceCount == 0) {
                            statsEnd = i + 1;
                            break;
                        }
                    }
                }

                if (statsEnd > statsStart) {
                    String statsSection = stats.substring(statsStart, statsEnd).trim();

                    // Gereksiz karakterleri temizle
                    statsSection = statsSection.replace("stats:", "Stats:").replace("{", "").replace("}", "").trim();

                    // Stamina'yı çıkar
                    statsSection = statsSection.replaceAll("\\+[\\d,]+ Sta,?\\s*", "");

                    // Armor'ı çıkar
                    statsSection = statsSection.replaceAll("[\\d,]+ Armor,?\\s*", "");

                    cleanStats.append(statsSection).append("\n");
                }
            } catch (Exception e) {
                logger.warn("Error extracting stats section: {}", e.getMessage());
            }
        }

        // Enchant bilgisini çıkar
        if (stats.contains("enchant:")) {
            try {
                int enchantStart = stats.indexOf("enchant:");
                int enchantEnd = stats.indexOf(",", enchantStart);
                if (enchantEnd == -1) enchantEnd = stats.length();
                if (enchantEnd > enchantStart) {
                    String enchant = stats.substring(enchantStart, enchantEnd).trim();
                    enchant = enchant.replace("enchant:", "Enchant: ").trim();
                    cleanStats.append(enchant).append("\n");
                }
            } catch (Exception e) {
                logger.warn("Error extracting enchant: {}", e.getMessage());
            }
        }

        // Temporary enchant bilgisini çıkar
        if (stats.contains("temporary_enchant:")) {
            try {
                int tempEnchantStart = stats.indexOf("temporary_enchant:");
                int tempEnchantEnd = stats.indexOf(",", tempEnchantStart);
                if (tempEnchantEnd == -1) {
                    // Virgül yoksa, item effects'e kadar veya sonuna kadar al
                    tempEnchantEnd = stats.indexOf("item effects:", tempEnchantStart);
                    if (tempEnchantEnd == -1) tempEnchantEnd = stats.length();
                }
                if (tempEnchantEnd > tempEnchantStart) {
                    String tempEnchant = stats.substring(tempEnchantStart, tempEnchantEnd).trim();
                    tempEnchant = tempEnchant.replace("temporary_enchant:", "Temporary Enchant: ").trim();
                    cleanStats.append(tempEnchant).append("\n");
                }
            } catch (Exception e) {
                logger.warn("Error extracting temporary enchant: {}", e.getMessage());
            }
        }

        // Gem bilgilerini çıkar
        if (stats.contains("gems:")) {
            try {
                int gemsStart = stats.indexOf("gems:");
                int gemsEnd = stats.indexOf("}", gemsStart);
                if (gemsEnd > gemsStart) {
                    String gemInfo = stats.substring(gemsStart, gemsEnd + 1)
                            .replace("gems:", "Gems: ")
                            .replace("{", "")
                            .replace("}", "")
                            .trim();
                    cleanStats.append(gemInfo).append("\n");
                }
            } catch (Exception e) {
                logger.warn("Error extracting gems: {}", e.getMessage());
            }
        }

        // Item effects bilgisini çıkar
        if (stats.contains("item effects:")) {
            try {
                int effectsStart = stats.indexOf("item effects:");
                int effectsEnd = stats.indexOf("}", effectsStart);
                if (effectsEnd > effectsStart) {
                    String effectsInfo = stats.substring(effectsStart, effectsEnd + 1)
                            .replace("item effects:", "Item Effects: ")
                            .replace("{", "")
                            .replace("}", "")
                            .trim();
                    cleanStats.append(effectsInfo).append("\n");
                }
            } catch (Exception e) {
                logger.warn("Error extracting item effects: {}", e.getMessage());
            }
        }

        String result = cleanStats.toString().trim();

        // Eğer hiçbir şey bulunamadıysa, orijinal stats'ı kısalt
        if (result.isEmpty()) {
            logger.warn("Could not extract formatted stats, using original stats");

            // Orijinal stats'ı temizle
            String cleanOriginalStats = stats
                    .replace("ilevel:", "") // İlevel etiketini tamamen kaldır
                    .replace("stats:", "Stats:")
                    .replace("enchant:", "Enchant:")
                    .replace("temporary_enchant:", "Temporary Enchant:")
                    .replace("gems:", "Gems:")
                    .replace("item effects:", "Item Effects:")
                    .replace("{", "")
                    .replace("}", "");

            if (cleanOriginalStats.length() > 900) {
                return cleanOriginalStats.substring(0, 897) + "...";
            }
            return cleanOriginalStats;
        }
        
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

    /**
     * Belirli bir sınıf ve spec için tüm hero talent'ların BIS ekipman bilgilerini içeren Discord embed'leri oluşturur.
     * Eğer slot belirtilmişse, sadece o slot için bilgileri gösterir.
     *
     * @param className Karakter sınıfı (Death_Knight, Mage, vb.)
     * @param specName  Spec adı (Blood, Frost, vb.)
     * @param slot      Ekipman slotu (Head, Neck, Shoulders, vb.) - Boş bırakılırsa tüm slotlar gösterilir
     * @return BIS ekipman bilgilerini içeren EmbedBuilder listesi
     */
    public List<EmbedBuilder> createBisGearEmbedsForAllHeroTalents(String className, String specName, String slot) {
        List<EmbedBuilder> embeds = new ArrayList<>();
        String formattedClassName = formatName(className);
        String formattedSpecName = formatName(specName);
        String formattedSlot = formatName(slot);

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
                String title = "BIS Gear for " + formattedClassName + " " + formattedSpecName + " (" + heroTalentName + ")";
                if (!formattedSlot.isEmpty()) {
                    title += " - " + formattedSlot;
                }
                embed.setTitle(title);

                // BIS ekipman bilgilerini al
                Map<String, Map<String, Object>> bisGear = cache.get(key);

                // Slot belirtilmişse, sadece o slotu göster
                if (!formattedSlot.isEmpty()) {
                    // Özel durumlar için kontrol (Weapon, Trinket, Finger)
                    if (formattedSlot.equalsIgnoreCase("Weapon") ||
                            formattedSlot.equalsIgnoreCase("Weapon1") ||
                            formattedSlot.equalsIgnoreCase("Mainhand") ||
                            formattedSlot.equalsIgnoreCase("Main-hand") ||
                            formattedSlot.equalsIgnoreCase("Main_hand") ||
                            formattedSlot.equalsIgnoreCase("Main hand") ||
                            formattedSlot.replace("_", " ").equalsIgnoreCase("Main Hand")) {

                        // Main Hand slotunu ara
                        for (String slotKey : bisGear.keySet()) {
                            if (slotKey.equalsIgnoreCase("Main Hand")) {
                                Map<String, Object> slotInfo = bisGear.get(slotKey);
                                addItemFieldToEmbed(embed, slotKey, slotInfo);
                                break;
                            }
                        }
                    } else if (formattedSlot.equalsIgnoreCase("Trinket")) {
                        // Tüm trinketleri göster
                        for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
                            if (entry.getKey().startsWith("Trinket")) {
                                addItemFieldToEmbed(embed, entry.getKey(), entry.getValue());
                            }
                        }
                    } else if (formattedSlot.equalsIgnoreCase("Finger") || formattedSlot.equalsIgnoreCase("Ring")) {
                        // Tüm yüzükleri göster
                        for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
                            if (entry.getKey().startsWith("Finger")) {
                                addItemFieldToEmbed(embed, entry.getKey(), entry.getValue());
                            }
                        }
                    } else {
                        // Diğer slotlar için tam eşleşme veya case-insensitive eşleşme ara
                        boolean found = false;

                        // Tam eşleşme
                        if (bisGear.containsKey(formattedSlot)) {
                            addItemFieldToEmbed(embed, formattedSlot, bisGear.get(formattedSlot));
                            found = true;
                        } else {
                            // Case-insensitive eşleşme
                            for (String slotKey : bisGear.keySet()) {
                                if (slotKey.equalsIgnoreCase(formattedSlot)) {
                                    addItemFieldToEmbed(embed, slotKey, bisGear.get(slotKey));
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if (!found) {
                            embed.setDescription("No BIS gear information found for slot: " + formattedSlot);
                        }
                    }
                } else {
                    // Slot belirtilmemişse, tüm slotları göster
                    for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
                        addItemFieldToEmbed(embed, entry.getKey(), entry.getValue());
                    }
                }

                embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");
                
                embeds.add(embed);
            }
        }

        return embeds;
    }

    // Yardımcı metod: Item bilgilerini embed'e ekler
    private void addItemFieldToEmbed(EmbedBuilder embed, String slotName, Map<String, Object> slotInfo) {
        String itemName = (String) slotInfo.get("name");
        String itemUrl = (String) slotInfo.get("url");
        String stats = (String) slotInfo.get("stats");

        StringBuilder fieldContent = new StringBuilder();
        fieldContent.append("[").append(itemName).append("](").append(itemUrl).append(")");

        // Stats bilgisini ekle (eğer varsa)
        if (stats != null && !stats.isEmpty()) {
            stats = truncateStats(stats);
            if (!stats.isEmpty()) {
                fieldContent.append("\n").append(stats).append("\n");
            }
        }

        embed.addField(slotName, fieldContent.toString(), false);
    }

    // Eski metodu yeni metoda yönlendir
    public List<EmbedBuilder> createBisGearEmbedsForAllHeroTalents(String className, String specName) {
        return createBisGearEmbedsForAllHeroTalents(className, specName, "");
    }
} 