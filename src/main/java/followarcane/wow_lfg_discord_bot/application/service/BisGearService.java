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
import java.util.HashMap;
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
        String key = buildKey(className, specName, heroTalent);
        return cache.getOrDefault(key, new HashMap<>());
    }

    public Map<String, Object> getBisGearForSlot(String slot, String className, String specName, String heroTalent) {
        Map<String, Map<String, Object>> bisGear = getBisGear(className, specName, heroTalent);
        return bisGear.getOrDefault(slot, new HashMap<>());
    }

    private String buildKey(String className, String specName, String heroTalent) {
        String key = className + "_" + specName;
        if (heroTalent != null && !heroTalent.isEmpty()) {
            key += "_" + heroTalent;
        }
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
            result.put("source", (String) itemInfo.get("source"));
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
        // Sınıf rengini al
        Color classColor = Color.decode(classColorCodeHelper.getClassColorCode(className));

        // BIS ekipman bilgilerini al
        Map<String, Object> itemInfo = getBisGearForSlot(slot, className, specName, heroTalent);

        // Embed oluştur
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(classColor);

        // Başlık oluştur
        String title = slot;
        if (slot == null || slot.isEmpty()) {
            title = "All Slots";
        }

        String specTitle = specName;
        if (heroTalent != null && !heroTalent.isEmpty()) {
            specTitle += " (" + heroTalent + ")";
        }

        embed.setTitle("BIS Gear for " + className.replace("_", " ") + " " + specTitle + " - " + title);

        if (slot == null || slot.isEmpty()) {
            // Tüm slotlar için BIS ekipmanları göster
            Map<String, Map<String, Object>> allGear = getBisGear(className, specName, heroTalent);

            if (allGear.isEmpty()) {
                embed.setDescription("No BIS gear information found for this spec.");
            } else {
                for (Map.Entry<String, Map<String, Object>> entry : allGear.entrySet()) {
                    String slotName = entry.getKey();
                    Map<String, Object> slotInfo = entry.getValue();

                    String itemName = (String) slotInfo.get("name");
                    String itemUrl = (String) slotInfo.get("url");
                    String source = (String) slotInfo.get("source");

                    embed.addField(slotName, "[" + itemName + "](" + itemUrl + ")\nSource: " + source, true);
                }
            }
        } else {
            // Belirli bir slot için BIS ekipmanı göster
            if (itemInfo.isEmpty()) {
                embed.setDescription("No BIS gear information found for this slot and spec.");
            } else {
                String itemName = (String) itemInfo.get("name");
                String itemUrl = (String) itemInfo.get("url");
                String source = (String) itemInfo.get("source");
                String stats = (String) itemInfo.get("stats");

                embed.addField("Item", "[" + itemName + "](" + itemUrl + ")", false);
                embed.addField("Source", source, false);

                if (stats != null && !stats.isEmpty()) {
                    // Stats bilgisi çok uzun olabilir, bu yüzden kısaltıyoruz
                    if (stats.length() > 1000) {
                        stats = stats.substring(0, 997) + "...";
                    }
                    embed.addField("Stats", stats, false);
                }
            }
        }

        embed.setFooter("Data from SimulationCraft");

        return embed;
    }
} 