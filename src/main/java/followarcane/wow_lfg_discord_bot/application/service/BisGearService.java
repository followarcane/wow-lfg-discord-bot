package followarcane.wow_lfg_discord_bot.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import followarcane.wow_lfg_discord_bot.application.util.*;
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

        // Sınıf kontrolü (en genel kategori)
        WowClassEnum wowClass = WowClassEnum.fromString(name);
        if (wowClass != null) {
            return wowClass.getFormattedName();
        }

        // Spec kontrolü (sınıftan sonra en genel kategori)
        WowSpecEnum spec = WowSpecEnum.fromString(name);
        if (spec != null) {
            return spec.getFormattedName();
        }

        // Slot kontrolü
        WowSlotEnum slot = WowSlotEnum.fromString(name);
        if (slot != null) {
            return slot.getFormattedName();
        }

        // Hero Talent kontrolü (en spesifik kategori)
        WowHeroTalentEnum heroTalent = WowHeroTalentEnum.fromString(name);
        if (heroTalent != null) {
            return heroTalent.getFormattedName();
        }

        // Hiçbir eşleşme bulunamazsa, ilk harfi büyük yap
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    private String formatClassName(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }

        // WowClassEnum kullanarak sınıf adını düzelt
        WowClassEnum wowClass = WowClassEnum.fromString(className);
        if (wowClass != null) {
            return wowClass.getFormattedName();
        }

        // Enum'da bulunamazsa manuel olarak düzelt
        String formattedName = className.trim();

        // Özel durumlar
        if (formattedName.equalsIgnoreCase("dk") || formattedName.equalsIgnoreCase("death knight")) {
            return "Death_Knight";
        } else if (formattedName.equalsIgnoreCase("dh") || formattedName.equalsIgnoreCase("demon hunter")) {
            return "Demon_Hunter";
        }

        // Genel düzeltme
        String[] parts = formattedName.split("_| ");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(parts[i].substring(0, 1).toUpperCase());
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1).toLowerCase());
                }
            }

            if (i < parts.length - 1) {
                result.append("_");
            }
        }

        return result.toString();
    }

    private String formatSpecName(String specName) {
        if (specName == null || specName.isEmpty()) {
            return "";
        }

        // WowSpecEnum kullanarak spec adını düzelt
        WowSpecEnum wowSpec = WowSpecEnum.fromString(specName);
        if (wowSpec != null) {
            return wowSpec.getFormattedName();
        }

        // Enum'da bulunamazsa manuel olarak düzelt
        String formattedName = specName.trim();

        // Özel durumlar
        if (formattedName.equalsIgnoreCase("bm") || formattedName.equalsIgnoreCase("beast mastery")) {
            return "Beast_Mastery";
        }

        // Genel düzeltme
        String[] parts = formattedName.split("_| ");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(parts[i].substring(0, 1).toUpperCase());
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1).toLowerCase());
                }
            }

            if (i < parts.length - 1) {
                result.append("_");
            }
        }

        return result.toString();
    }

    private String formatHeroTalentName(String heroTalent) {
        if (heroTalent == null || heroTalent.isEmpty()) {
            return "";
        }

        // WowHeroTalentEnum kullanarak hero talent adını düzelt
        WowHeroTalentEnum wowHeroTalent = WowHeroTalentEnum.fromString(heroTalent);
        if (wowHeroTalent != null && !wowHeroTalent.getFormattedName().isEmpty()) {
            return wowHeroTalent.getFormattedName();
        }

        // Enum'da bulunamazsa manuel olarak düzelt
        String formattedName = heroTalent.trim();

        // Özel durumlar
        if (formattedName.equalsIgnoreCase("rota") ||
                formattedName.equalsIgnoreCase("rider of the apocalypse") ||
                formattedName.equalsIgnoreCase("apocalypse")) {
            return "Rider";
        }

        // Genel düzeltme
        String[] parts = formattedName.split("_| ");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(parts[i].substring(0, 1).toUpperCase());
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1).toLowerCase());
                }
            }

            if (i < parts.length - 1) {
                result.append("_");
            }
        }

        return result.toString();
    }

    /**
     * Sınıf, spec ve hero talent adlarını birleştirerek önbellek anahtarı oluşturur
     */
    private String buildKey(String className, String specName, String heroTalent) {
        // Sınıf adını düzelt (ilk harfi büyük, diğerleri küçük)
        String formattedClassName = formatClassName(className);

        // Spec adını düzelt (ilk harfi büyük, diğerleri küçük)
        String formattedSpecName = formatSpecName(specName);

        // Hero talent adını düzelt
        String formattedHeroTalent = formatHeroTalentName(heroTalent);
        
        // Anahtar oluştur
        String key = formattedClassName;

        if (formattedSpecName != null && !formattedSpecName.isEmpty()) {
            key += "_" + formattedSpecName;
        }

        if (formattedHeroTalent != null && !formattedHeroTalent.isEmpty()) {
            key += "_" + formattedHeroTalent;
        }

        logger.info("Building key from: className={}, specName={}, heroTalent={}", formattedClassName, formattedSpecName, formattedHeroTalent);
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
        specTitle += " (" + formattedHeroTalent + ")";

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

        // İlevel bilgisini çıkar
        try {
            int ilevelStart = stats.indexOf("ilevel:");
            if (ilevelStart != -1) {
                int ilevelEnd = stats.indexOf(",", ilevelStart);
                if (ilevelEnd == -1) ilevelEnd = stats.indexOf("}", ilevelStart);
                if (ilevelEnd == -1) ilevelEnd = stats.length();

                String ilevelInfo = stats.substring(ilevelStart, ilevelEnd)
                        .replace("ilevel:", "Item Level: ")
                        .trim();
                cleanStats.append(ilevelInfo).append("\n");
            }
        } catch (Exception e) {
            logger.warn("Error extracting ilevel: {}", e.getMessage());
        }

        // Stats bilgisini çıkar
        try {
            int statsStart = stats.indexOf("stats:");
            if (statsStart != -1) {
                int statsEnd = stats.indexOf("}", statsStart);
                if (statsEnd == -1) statsEnd = stats.length();

                String statsInfo = stats.substring(statsStart, statsEnd)
                        .replace("stats:", "Stats: ")
                        .replace("{", "")
                        .replace("}", "")
                        .trim();
                cleanStats.append(statsInfo).append("\n");
            }
        } catch (Exception e) {
            logger.warn("Error extracting stats: {}", e.getMessage());
        }

        // Enchant bilgisini çıkar
        try {
            int enchantStart = stats.indexOf("enchant:");
            if (enchantStart != -1) {
                int enchantEnd = stats.indexOf("}", enchantStart);
                if (enchantEnd == -1) enchantEnd = stats.length();

                String enchantInfo = stats.substring(enchantStart, enchantEnd)
                        .replace("enchant:", "Enchant: ")
                        .replace("{", "")
                        .replace("}", "")
                        .trim();
                cleanStats.append(enchantInfo).append("\n");
            }
        } catch (Exception e) {
            logger.warn("Error extracting enchant: {}", e.getMessage());
        }

        // Temporary Enchant bilgisini çıkar
        try {
            int tempEnchantStart = stats.indexOf("temporary_enchant:");
            if (tempEnchantStart != -1) {
                int tempEnchantEnd = stats.indexOf("}", tempEnchantStart);
                if (tempEnchantEnd == -1) tempEnchantEnd = stats.length();

                String tempEnchantInfo = stats.substring(tempEnchantStart, tempEnchantEnd)
                        .replace("temporary_enchant:", "Temporary Enchant: ")
                        .replace("{", "")
                        .replace("}", "")
                        .trim();
                cleanStats.append(tempEnchantInfo).append("\n");
            }
        } catch (Exception e) {
            logger.warn("Error extracting temporary enchant: {}", e.getMessage());
        }

        // Gems bilgisini çıkar
        try {
            int gemsStart = stats.indexOf("gems:");
            if (gemsStart != -1) {
                int gemsEnd = stats.indexOf("}", gemsStart);
                if (gemsEnd == -1) gemsEnd = stats.length();

                String gemsInfo = stats.substring(gemsStart, gemsEnd)
                        .replace("gems:", "Gems: ")
                        .replace("{", "")
                        .replace("}", "")
                        .trim();
                cleanStats.append(gemsInfo).append("\n");
            }
        } catch (Exception e) {
            logger.warn("Error extracting gems: {}", e.getMessage());
        }

        // Item Effects bilgisini çıkar
        try {
            int effectsStart = stats.indexOf("item effects:");
            if (effectsStart != -1) {
                int effectsEnd = stats.indexOf("}", effectsStart);
                if (effectsEnd == -1) effectsEnd = stats.length();

                String effectsInfo = stats.substring(effectsStart, effectsEnd)
                        .replace("item effects:", "Item Effects: ")
                        .replace("{", "")
                        .replace("}", "")
                        .trim();
                cleanStats.append(effectsInfo).append("\n");
            }
        } catch (Exception e) {
            logger.warn("Error extracting item effects: {}", e.getMessage());
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

        // Kalan süslü parantezleri temizle
        result = result.replace("{", "").replace("}", "");
        
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

        // Sınıf ve spec için tüm hero talent'ları bul
        boolean foundAnyHeroTalent = false;
        String searchPrefix = formattedClassName + "_" + formattedSpecName + "_";
        
        for (String key : cache.keySet()) {
            if (key.startsWith(searchPrefix)) {
                foundAnyHeroTalent = true;
                // Extract hero talent name from key
                String heroTalentName = key.substring(searchPrefix.length());

                // Bu hero talent için bir embed oluştur
                EmbedBuilder embed = createEmbedForHeroTalent(formattedClassName, formattedSpecName, heroTalentName, formattedSlot, cache.get(key));
                embeds.add(embed);
            }
        }

        // Eğer hero talent bulunamadıysa, doğrudan sınıf_spec anahtarını kontrol et
        if (!foundAnyHeroTalent) {
            String directKey = formattedClassName + "_" + formattedSpecName;
            if (cache.containsKey(directKey)) {
                // Hero talent olmadan doğrudan spec için bir embed oluştur
                EmbedBuilder embed = createEmbedForHeroTalent(formattedClassName, formattedSpecName, "", formattedSlot, cache.get(directKey));
                embeds.add(embed);
            }
        }

        return embeds;
    }

    // Hero talent için embed oluşturan yardımcı metod
    private EmbedBuilder createEmbedForHeroTalent(String className, String specName, String heroTalentName, String slot, Map<String, Map<String, Object>> bisGear) {
        EmbedBuilder embed = new EmbedBuilder();

        // Sınıf rengini al
        Color classColor;
        try {
            classColor = Color.decode(classColorCodeHelper.getClassColorCode(className));
            logger.info("Using class color for {}: {}", className, classColor);
        } catch (Exception e) {
            logger.warn("Could not get class color for {}. Using default color.", className);
            classColor = Color.GRAY; // Varsayılan renk
        }

        embed.setColor(classColor);

        // Başlık oluştur
        String title = "BIS Gear for " + className + " " + specName;
        if (!heroTalentName.isEmpty()) {
            title += " (" + heroTalentName + ")";
        }
        if (!slot.isEmpty()) {
            title += " - " + slot;
        }
        embed.setTitle(title);

        // Slot belirtilmişse, sadece o slotu göster
        if (!slot.isEmpty()) {
            // Özel durumlar için kontrol (Weapon, Trinket, Finger)
            if (slot.equalsIgnoreCase("Weapon") ||
                    slot.equalsIgnoreCase("Weapon1") ||
                    slot.equalsIgnoreCase("Mainhand") ||
                    slot.equalsIgnoreCase("Main-hand") ||
                    slot.equalsIgnoreCase("Main_hand") ||
                    slot.equalsIgnoreCase("Main hand") ||
                    slot.replace("_", " ").equalsIgnoreCase("Main Hand")) {

                // Main Hand slotunu ara
                for (String slotKey : bisGear.keySet()) {
                    if (slotKey.equalsIgnoreCase("Main Hand")) {
                        Map<String, Object> slotInfo = bisGear.get(slotKey);
                        addItemFieldToEmbed(embed, slotKey, slotInfo);
                        break;
                    }
                }
            } else if (slot.equalsIgnoreCase("Trinket")) {
                // Tüm trinketleri göster
                for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
                    if (entry.getKey().startsWith("Trinket")) {
                        addItemFieldToEmbed(embed, entry.getKey(), entry.getValue());
                    }
                }
            } else if (slot.equalsIgnoreCase("Finger") || slot.equalsIgnoreCase("Ring")) {
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
                if (bisGear.containsKey(slot)) {
                    addItemFieldToEmbed(embed, slot, bisGear.get(slot));
                    found = true;
                } else {
                    // Case-insensitive eşleşme
                    for (String slotKey : bisGear.keySet()) {
                        if (slotKey.equalsIgnoreCase(slot)) {
                            addItemFieldToEmbed(embed, slotKey, bisGear.get(slotKey));
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    embed.setDescription("No BIS gear information found for slot: " + slot);
                }
            }
        } else {
            // Slot belirtilmemişse, tüm slotları göster
            for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
                addItemFieldToEmbed(embed, entry.getKey(), entry.getValue());
            }
        }

        embed.setFooter("Powered by Azerite!\nVisit -> https://azerite.app\nDonate -> https://www.patreon.com/Shadlynn/membership", "https://i.imgur.com/fK2PvPV.png");

        return embed;
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