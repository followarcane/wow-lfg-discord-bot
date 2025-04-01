package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.model.RaidbotsProfileInfo;
import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class BisGearService {
    private static final String SIM_URL = "https://www.simulationcraft.org/reports/TWW2_Raid.html";
    // Önbellek süresi (12 saat)
    private static final Duration CACHE_DURATION = Duration.ofHours(12);
    // Sınıf ve spec adlarını doğru formatta tutmak için map'ler
    private static final Map<String, String> CLASS_NAMES = new HashMap<>();
    private static final Map<String, List<String>> SPECS_BY_CLASS = new HashMap<>();
    // Hero talent listesi
    private static final Map<String, List<String>> HERO_TALENTS_BY_CLASS = new HashMap<>();

    static {
        // Sınıf adları
        CLASS_NAMES.put("death_knight", "Death_Knight");
        CLASS_NAMES.put("demon_hunter", "Demon_Hunter");
        CLASS_NAMES.put("druid", "Druid");
        CLASS_NAMES.put("hunter", "Hunter");
        CLASS_NAMES.put("mage", "Mage");
        CLASS_NAMES.put("monk", "Monk");
        CLASS_NAMES.put("paladin", "Paladin");
        CLASS_NAMES.put("priest", "Priest");
        CLASS_NAMES.put("rogue", "Rogue");
        CLASS_NAMES.put("shaman", "Shaman");
        CLASS_NAMES.put("warlock", "Warlock");
        CLASS_NAMES.put("warrior", "Warrior");
        CLASS_NAMES.put("evoker", "Evoker");

        // Her sınıf için spec'ler
        SPECS_BY_CLASS.put("Death_Knight", Arrays.asList("Blood", "Frost", "Unholy"));
        SPECS_BY_CLASS.put("Demon_Hunter", Arrays.asList("Havoc", "Vengeance"));
        SPECS_BY_CLASS.put("Druid", Arrays.asList("Balance", "Feral", "Guardian", "Restoration"));
        SPECS_BY_CLASS.put("Hunter", Arrays.asList("Beast_Mastery", "Marksmanship", "Survival"));
        SPECS_BY_CLASS.put("Mage", Arrays.asList("Arcane", "Fire", "Frost"));
        SPECS_BY_CLASS.put("Monk", Arrays.asList("Brewmaster", "Mistweaver", "Windwalker"));
        SPECS_BY_CLASS.put("Paladin", Arrays.asList("Holy", "Protection", "Retribution"));
        SPECS_BY_CLASS.put("Priest", Arrays.asList("Discipline", "Holy", "Shadow"));
        SPECS_BY_CLASS.put("Rogue", Arrays.asList("Assassination", "Outlaw", "Subtlety"));
        SPECS_BY_CLASS.put("Shaman", Arrays.asList("Elemental", "Enhancement", "Restoration"));
        SPECS_BY_CLASS.put("Warlock", Arrays.asList("Affliction", "Demonology", "Destruction"));
        SPECS_BY_CLASS.put("Warrior", Arrays.asList("Arms", "Fury", "Protection"));
        SPECS_BY_CLASS.put("Evoker", Arrays.asList("Devastation", "Preservation", "Augmentation"));

        // Death Knight hero talentları
        HERO_TALENTS_BY_CLASS.put("Death_Knight", Arrays.asList("Deathbringer", "Rider", "San'layn"));

        // Demon Hunter hero talentları
        HERO_TALENTS_BY_CLASS.put("Demon_Hunter", Arrays.asList("Aldrachi_Reaver", "Fel-Scarred"));

        // Druid hero talentları
        HERO_TALENTS_BY_CLASS.put("Druid", Arrays.asList("Druid_of_the_Claw", "Elune's_Chosen", "Keeper_of_the_Grove", "Wildstalker"));

        // Evoker hero talentları
        HERO_TALENTS_BY_CLASS.put("Evoker", Arrays.asList("Chronowarden", "Flameshaper", "Scalecommander"));

        // Hunter hero talentları
        HERO_TALENTS_BY_CLASS.put("Hunter", Arrays.asList("Dark_Ranger", "Pack_Leader", "Sentinel"));

        // Mage hero talentları
        HERO_TALENTS_BY_CLASS.put("Mage", Arrays.asList("Frostfire", "Spellslinger", "Sunfury"));

        // Monk hero talentları
        HERO_TALENTS_BY_CLASS.put("Monk", Arrays.asList("Conduit_of_the_Celestials", "Master_of_Harmony", "Shadopan"));

        // Paladin hero talentları
        HERO_TALENTS_BY_CLASS.put("Paladin", Arrays.asList("Herald", "Lightsmith", "Templar"));

        // Priest hero talentları
        HERO_TALENTS_BY_CLASS.put("Priest", Arrays.asList("Archon", "Oracle", "Voidweaver"));

        // Rogue hero talentları
        HERO_TALENTS_BY_CLASS.put("Rogue", Arrays.asList("Deathstalker", "Fatebound", "Trickster"));

        // Shaman hero talentları
        HERO_TALENTS_BY_CLASS.put("Shaman", Arrays.asList("Farseer", "Stormbringer", "Totemic"));

        // Warlock hero talentları
        HERO_TALENTS_BY_CLASS.put("Warlock", Arrays.asList("Diabolist", "Hellcaller", "Soul_Harvester"));

        // Warrior hero talentları
        HERO_TALENTS_BY_CLASS.put("Warrior", Arrays.asList("Colossus", "Thane", "Slayer"));
    }

    private final ClassColorCodeHelper classColorCodeHelper;
    // Önbellek için map
    private final Map<String, CachedBisData> bisCache = new ConcurrentHashMap<>();

    public BisGearService(ClassColorCodeHelper classColorCodeHelper) {
        this.classColorCodeHelper = classColorCodeHelper;
    }

    /**
     * Önbelleği temizle ve yeniden doldur (12 saatte bir çalışır)
     */
    @Scheduled(fixedRate = 12 * 60 * 60 * 1000) // 12 saat
    public void refreshCache() {
        log.info("Refreshing BIS gear cache");
        bisCache.clear();

        // Tüm sınıflar için BIS verilerini çek
        for (String className : CLASS_NAMES.values()) {
            try {
                List<String> specs = SPECS_BY_CLASS.get(className);
                if (specs != null) {
                    for (String spec : specs) {
                        List<String> heroTalents = HERO_TALENTS_BY_CLASS.get(className);
                        if (heroTalents != null && !heroTalents.isEmpty()) {
                            for (String heroTalent : heroTalents) {
                                try {
                                    fetchBisGear(null, className, spec, heroTalent);
                                } catch (Exception e) {
                                    log.error("Error fetching BIS gear for {} {} {}: {}", className, spec, heroTalent, e.getMessage());
                                }
                            }
                        } else {
                            try {
                                fetchBisGear(null, className, spec, "");
                            } catch (Exception e) {
                                log.error("Error fetching BIS gear for {} {}: {}", className, spec, e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching BIS gear for {}: {}", className, e.getMessage());
            }
        }

        log.info("BIS gear cache refresh completed. Cache size: {}", bisCache.size());
    }

    /**
     * Belirli bir sınıf, spec ve hero talent için BIS ekipman bilgilerini içeren bir embed oluşturur
     * Spec ve hero talent parametreleri opsiyoneldir. Girilmezse tüm spec'ler ve hero talent'lar için BIS ekipmanları getirir.
     */
    public EmbedBuilder createBisGearEmbed(String slot, String className, String specName, String heroTalent) {
        try {
            // Sınıf adını normalize et
            String normalizedClassName = normalizeClassName(className);
            if (normalizedClassName == null) {
                return createErrorEmbed("Invalid Class", "Could not find class: " + className);
            }

            // Spec adı boşsa, tüm spec'ler için BIS ekipmanları getir
            if (specName == null || specName.isEmpty()) {
                return createAllSpecsEmbed(slot, normalizedClassName, heroTalent);
            }

            // Spec adını normalize et
            String normalizedSpecName = normalizeSpecName(normalizedClassName, specName);
            if (normalizedSpecName == null) {
                return createErrorEmbed("Invalid Spec", "Could not find spec: " + specName + " for class: " + normalizedClassName);
            }

            // Hero talent adı boşsa, tüm hero talent'lar için BIS ekipmanları getir
            if (heroTalent == null || heroTalent.isEmpty()) {
                return createAllHeroTalentsEmbed(slot, normalizedClassName, normalizedSpecName);
            }

            // Hero talent adını normalize et
            String normalizedHeroTalent = normalizeHeroTalent(normalizedClassName, heroTalent);
            if (normalizedHeroTalent == null) {
                return createErrorEmbed("Invalid Hero Talent", "Could not find hero talent: " + heroTalent + " for class: " + normalizedClassName);
            }

            // BIS ekipmanları getir
            Map<String, String> bisItems;
            try {
                // Selenium ile BIS ekipmanları getir
                bisItems = fetchBisGearWithSelenium(slot, normalizedClassName, normalizedSpecName, normalizedHeroTalent);
            } catch (Exception e) {
                log.error("Error fetching BIS gear with Selenium: {}", e.getMessage(), e);
                return createErrorEmbed("Error", "Error fetching BIS gear: " + e.getMessage());
            }

            // Embed oluştur
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(normalizedClassName + " " + normalizedSpecName + " " + normalizedHeroTalent + " BIS Gear" + (slot != null && !slot.equalsIgnoreCase("all") ? " (" + slot + ")" : ""));
            //embed.setColor(classColorCodeHelper.getClassColor(normalizedClassName));

            // Ekipmanları ekle
            for (Map.Entry<String, String> entry : bisItems.entrySet()) {
                embed.addField(entry.getKey(), entry.getValue(), false);
            }

            return embed;
        } catch (Exception e) {
            log.error("Error creating BIS gear embed: {}", e.getMessage(), e);
            return createErrorEmbed("Error", "Error creating BIS gear embed: " + e.getMessage());
        }
    }

    /**
     * Tüm hero talentlar için BIS ekipmanları içeren bir embed oluşturur
     */
    private EmbedBuilder createAllHeroTalentsEmbed(String slot, String className, String specName) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(className + " " + specName + " - All Hero Talents BIS Gear");
        embed.setColor(Color.decode(classColorCodeHelper.getClassColorCode(className.replace("_", " "))));

        // Sınıf için tüm hero talentları al
        List<String> heroTalents = HERO_TALENTS_BY_CLASS.get(className);
        if (heroTalents == null || heroTalents.isEmpty()) {
            // Hero talent yoksa, boş hero talent ile çağır
            return createBisGearEmbed(slot, className, specName, "");
        }

        // Her hero talent için BIS ekipmanları getir
        for (String heroTalent : heroTalents) {
            try {
                EmbedBuilder heroTalentEmbed = createBisGearEmbed(slot, className, specName, heroTalent);

                if (heroTalentEmbed != null && !heroTalentEmbed.getFields().isEmpty()) {
                    embed.addField("__**" + heroTalent + "**__", "", false);
                    for (MessageEmbed.Field field : heroTalentEmbed.getFields()) {
                        embed.addField(field);
                    }
                }
            } catch (Exception e) {
                log.error("Error getting BIS gear for {} {}: {}", specName, heroTalent, e.getMessage());
                embed.addField("__**" + heroTalent + "**__", "Error: " + e.getMessage(), false);
            }
        }

        return embed;
    }

    /**
     * Gear tablosundan BIS ekipmanları çıkarır
     */
    private Map<String, String> extractBisItems(Element gearTable) {
        log.info("Extracting BIS items from gear table");

        Map<String, String> bisItems = new LinkedHashMap<>();

        // Tablo satırlarını işle
        Elements rows = gearTable.select("tr");
        log.debug("Found {} rows in gear table", rows.size());

        // Tüm satırları ve hücreleri logla
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cells = row.select("td, th");
            log.debug("Row {} has {} cells", i, cells.size());

            for (int j = 0; j < cells.size(); j++) {
                Element cell = cells.get(j);
                log.debug("  Cell {}: {}", j, cell.text());
            }
        }

        // Ekipman slotlarını içeren satırları bul
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cells = row.select("td, th");

            // Ekipman satırları 3 hücreli olmalı: Source, Slot, Item
            if (cells.size() == 3) {
                String source = cells.get(0).text().trim();
                String slot = cells.get(1).text().trim();

                // Başlık satırını atla
                if (slot.equals("Slot") || source.equals("Source")) {
                    continue;
                }

                // Ekipman slotlarını kontrol et
                if (slot.equals("Head") || slot.equals("Neck") || slot.equals("Shoulders") ||
                        slot.equals("Back") || slot.equals("Chest") || slot.equals("Waist") ||
                        slot.equals("Legs") || slot.equals("Feet") || slot.equals("Wrists") ||
                        slot.equals("Hands") || slot.equals("Finger1") || slot.equals("Finger2") ||
                        slot.equals("Trinket1") || slot.equals("Trinket2") ||
                        slot.equals("Main Hand") || slot.equals("Off Hand")) {

                    // İtem adını al - a etiketinden
                    Element itemCell = cells.get(2);
                    Element itemLink = itemCell.selectFirst("a");

                    if (itemLink != null) {
                        String item = itemLink.text().trim();
                        log.debug("Found item for slot {}: {}", slot, item);

                        // Slot adını normalize et
                        String normalizedSlot = normalizeSlotName(slot);

                        // Ekipmanı ekle
                        bisItems.put(normalizedSlot, item);
                    }
                }
            }
        }

        log.info("Extracted {} BIS items", bisItems.size());
        return bisItems;
    }

    /**
     * Hata mesajı içeren bir embed oluşturur
     */
    private EmbedBuilder createErrorEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        embed.setDescription(description);
        embed.setColor(Color.RED);
        return embed;
    }

    /**
     * Sınıf adını normalize eder
     */
    private String normalizeClassName(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }

        // Boşlukları kaldır ve küçük harfe çevir
        String normalizedName = className.trim().toLowerCase().replace(" ", "");

        // Kısaltmalar için kontrol
        if (normalizedName.equals("dk")) {
            normalizedName = "death_knight";
        } else if (normalizedName.equals("dh")) {
            normalizedName = "demon_hunter";
        } else if (normalizedName.equals("bm") || normalizedName.equals("beast")) {
            normalizedName = "beast_mastery";
        } else if (normalizedName.equals("mm")) {
            normalizedName = "marksmanship";
        } else if (normalizedName.equals("resto")) {
            normalizedName = "restoration";
        }

        // Map'ten doğru formatı al
        return CLASS_NAMES.getOrDefault(normalizedName, null);
    }

    /**
     * Spec adını normalize eder
     */
    private String normalizeSpecName(String className, String specName) {
        if (specName == null || specName.isEmpty()) {
            return null;
        }

        // Boşlukları kaldır ve ilk harfi büyük yap
        String normalizedName = specName.trim().replace(" ", "_");
        normalizedName = normalizedName.substring(0, 1).toUpperCase() + normalizedName.substring(1).toLowerCase();

        // Sınıf için geçerli spec'leri kontrol et
        List<String> validSpecs = SPECS_BY_CLASS.get(className);
        if (validSpecs == null) {
            return null;
        }

        // Spec adını doğrula
        for (String validSpec : validSpecs) {
            if (validSpec.equalsIgnoreCase(normalizedName)) {
                return validSpec;
            }
        }

        // Beast Mastery gibi özel durumlar için kontrol
        if (normalizedName.equalsIgnoreCase("beast_mastery") || normalizedName.equalsIgnoreCase("beastmastery")) {
            return "Beast_Mastery";
        }

        return null;
    }

    /**
     * Hero talent adını normalize eder
     */
    private String normalizeHeroTalent(String className, String heroTalent) {
        if (heroTalent == null || heroTalent.isEmpty()) {
            return null;
        }

        // Boşlukları kaldır ve ilk harfi büyük yap
        String normalizedName = heroTalent.trim().replace(" ", "_");
        normalizedName = normalizedName.substring(0, 1).toUpperCase() + normalizedName.substring(1).toLowerCase();

        // Sınıf için geçerli hero talentları kontrol et
        List<String> validTalents = HERO_TALENTS_BY_CLASS.get(className);
        if (validTalents == null) {
            return null;
        }

        // Hero talent adını doğrula
        for (String validTalent : validTalents) {
            if (validTalent.equalsIgnoreCase(normalizedName)) {
                return validTalent;
            }

            // Kısaltmalar için kontrol
            if (validTalent.contains(normalizedName) || normalizedName.contains(validTalent)) {
                return validTalent;
            }
        }

        return null;
    }

    /**
     * Slot adını normalize eder
     */
    private String normalizeSlotName(String slotName) {
        if (slotName == null || slotName.isEmpty()) {
            return "";
        }

        // Orijinal slot adını koru
        return slotName.trim();
    }

    /**
     * Tüm spec'ler için BIS ekipmanları içeren bir embed oluşturur
     */
    private EmbedBuilder createAllSpecsEmbed(String slot, String className, String heroTalent) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(className + " - All Specs BIS Gear");
        embed.setColor(Color.decode(classColorCodeHelper.getClassColorCode(className.replace("_", " "))));

        // Sınıf için tüm spec'leri al
        List<String> specs = SPECS_BY_CLASS.get(className);
        if (specs == null || specs.isEmpty()) {
            embed.setDescription("No specs found for " + className);
            return embed;
        }

        // Her spec için BIS ekipmanları getir
        for (String spec : specs) {
            try {
                EmbedBuilder specEmbed;
                if (heroTalent == null || heroTalent.isEmpty()) {
                    // Hero talent boşsa, tüm hero talent'lar için BIS ekipmanları getir
                    specEmbed = createAllHeroTalentsEmbed(slot, className, spec);
                } else {
                    // Belirli bir hero talent için BIS ekipmanları getir
                    specEmbed = createBisGearEmbed(slot, className, spec, heroTalent);
                }

                if (specEmbed != null && !specEmbed.getFields().isEmpty()) {
                    embed.addField("__**" + spec + "**__", "", false);
                    for (MessageEmbed.Field field : specEmbed.getFields()) {
                        embed.addField(field);
                    }
                }
            } catch (Exception e) {
                log.error("Error getting BIS gear for {}: {}", spec, e.getMessage());
                embed.addField("__**" + spec + "**__", "Error: " + e.getMessage(), false);
            }
        }

        return embed;
    }

    /**
     * SimulationCraft sayfasından BIS ekipman bilgilerini çeker (önbellekten veya yeni çeker)
     */
    private Map<String, String> fetchBisGear(String slot, String className, String specName, String heroTalent) throws IOException {
        // Önbellek anahtarı oluştur
        String cacheKey = className + "_" + specName + "_" + (heroTalent != null ? heroTalent : "");

        // Önbellekte var mı kontrol et
        CachedBisData cachedData = bisCache.get(cacheKey);
        if (cachedData != null && !cachedData.isExpired()) {
            log.debug("Using cached BIS gear data for {} {} {}", className, specName, heroTalent);
            Map<String, String> bisItems = new LinkedHashMap<>(cachedData.getBisItems());

            // Belirli bir slot için filtrele
            if (slot != null && !slot.equalsIgnoreCase("all")) {
                return filterItemsBySlot(bisItems, slot);
            }

            return bisItems;
        }

        log.info("Fetching SimulationCraft data from URL: {}", SIM_URL);

        // SimulationCraft sayfasını çek
        Document doc = Jsoup.connect(SIM_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(30000)
                .get();

        log.info("Successfully fetched SimulationCraft page");

        // Profil bilgisini bul
        RaidbotsProfileInfo profileInfo = RaidbotsProfileInfo.findProfileInfo(className, specName, heroTalent);
        if (profileInfo == null) {
            log.error("Could not find profile info for {} {} {}", className, specName, heroTalent);
            return Collections.emptyMap();
        }

        log.info("Found profile info: {}", profileInfo.name());

        Element gearTable = null;
        try {
            // XPath veya CSS selector kullanarak tabloyu bul
            String selector = profileInfo.getGearTableXPath();
            if (selector.startsWith("/")) {
                // XPath
                gearTable = doc.selectXpath(selector).first();
            } else {
                // CSS selector
                gearTable = doc.select(selector).first();
            }

            if (gearTable == null) {
                log.warn("Could not find gear table with selector: {}", selector);

                // Tüm tabloları kontrol et
                Elements tables = doc.select("table");
                log.info("Found {} tables in document", tables.size());

                // Oyuncu ID'sini kullanarak tabloyu bul
                String playerId = profileInfo.getPlayerId();
                Element playerDiv = doc.getElementById(playerId);
                if (playerDiv != null) {
                    log.info("Found player div with ID: {}", playerId);
                    gearTable = playerDiv.select("table").first();
                }

                // Hala bulunamadıysa, tüm tabloları kontrol et ve içeriğine göre bul
                if (gearTable == null) {
                    log.warn("Could not find gear table using player ID: {}", playerId);

                    for (Element table : tables) {
                        // Tablonun içeriğini kontrol et
                        Elements rows = table.select("tr");
                        for (Element row : rows) {
                            Elements cells = row.select("td, th");
                            if (cells.size() >= 2) {
                                String firstCell = cells.get(0).text();
                                if (firstCell.contains("Head") || firstCell.contains("Shoulders") || firstCell.contains("Chest")) {
                                    gearTable = table;
                                    log.info("Found gear table by checking content");
                                    break;
                                }
                            }
                        }
                        if (gearTable != null) break;
                    }
                }
            }

            if (gearTable == null) {
                log.error("Could not find gear table for {} {} {}", className, specName, heroTalent);
                return Collections.emptyMap();
            }

            // BIS ekipmanları çıkar
            Map<String, String> bisItems = extractBisItems(gearTable);

            // Önbelleğe ekle
            bisCache.put(cacheKey, new CachedBisData(bisItems));

            // Belirli bir slot için filtrele
            if (slot != null && !slot.equalsIgnoreCase("all")) {
                return filterItemsBySlot(bisItems, slot);
            }

            return bisItems;
        } catch (Exception e) {
            log.error("Error selecting with selector: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Belirli bir slot için ekipmanları filtreler
     */
    private Map<String, String> filterItemsBySlot(Map<String, String> bisItems, String slot) {
        String normalizedSlot = normalizeSlotName(slot);

        // Finger ve Trinket slotları için özel kontrol
        if (normalizedSlot.equals("finger") || normalizedSlot.equals("fingers") ||
                normalizedSlot.equals("ring") || normalizedSlot.equals("rings")) {
            Map<String, String> filteredItems = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : bisItems.entrySet()) {
                if (entry.getKey().startsWith("Finger")) {
                    filteredItems.put(entry.getKey(), entry.getValue());
                }
            }
            return filteredItems;
        } else if (normalizedSlot.equals("trinket") || normalizedSlot.equals("trinkets")) {
            Map<String, String> filteredItems = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : bisItems.entrySet()) {
                if (entry.getKey().startsWith("Trinket")) {
                    filteredItems.put(entry.getKey(), entry.getValue());
                }
            }
            return filteredItems;
        } else {
            // Diğer slotlar için normal filtreleme
            Map<String, String> filteredItems = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : bisItems.entrySet()) {
                // Slot adını normalize et ve karşılaştır
                String entrySlot = entry.getKey().toLowerCase();
                if (entrySlot.equals(normalizedSlot) ||
                        (normalizedSlot.equals("shoulder") && entrySlot.equals("shoulders")) ||
                        (normalizedSlot.equals("shoulders") && entrySlot.equals("shoulder")) ||
                        (normalizedSlot.equals("wrist") && entrySlot.equals("wrists")) ||
                        (normalizedSlot.equals("wrists") && entrySlot.equals("wrist")) ||
                        (normalizedSlot.equals("hand") && entrySlot.equals("hands")) ||
                        (normalizedSlot.equals("hands") && entrySlot.equals("hand")) ||
                        (normalizedSlot.equals("leg") && entrySlot.equals("legs")) ||
                        (normalizedSlot.equals("legs") && entrySlot.equals("leg"))) {
                    filteredItems.put(entry.getKey(), entry.getValue());
                }
            }
            return filteredItems;
        }
    }

    /**
     * SimulationCraft sayfasından BIS ekipman bilgilerini çeker (Selenium ile)
     */
    public Map<String, String> fetchBisGearWithSelenium(String slot, String className, String specName, String heroTalent) {
        WebDriver driver = null;
        try {
            // WebDriver'ı başlat
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");  // Headless mod
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            driver = new ChromeDriver(options);
            driver.get(SIM_URL);

            // Profil bilgisini bul
            RaidbotsProfileInfo profileInfo = RaidbotsProfileInfo.findProfileInfo(className, specName, heroTalent);
            if (profileInfo == null) {
                log.error("Could not find profile info for {} {} {}", className, specName, heroTalent);
                return Collections.emptyMap();
            }

            // Oyuncu ID'sini kullanarak toggle'ı bul ve tıkla
            String playerId = profileInfo.getPlayerId();
            String toggleId = playerId + "_toggle";

            try {
                WebElement toggle = driver.findElement(By.id(toggleId));
                toggle.click();

                // İçeriğin yüklenmesini bekle
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(playerId)));

                // Tabloyu bul
                WebElement table = driver.findElement(By.cssSelector("#" + playerId + " div.player-section.gear table"));

                // Tablodan BIS ekipmanları çıkar
                Map<String, String> bisItems = new LinkedHashMap<>();

                List<WebElement> rows = table.findElements(By.tagName("tr"));
                for (WebElement row : rows) {
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    if (cells.size() >= 3) {
                        String slotName = cells.get(1).getText().trim();
                        String itemName = "";

                        // İtem adını al
                        WebElement itemCell = cells.get(2);
                        WebElement itemLink = itemCell.findElement(By.tagName("a"));
                        if (itemLink != null) {
                            itemName = itemLink.getText().trim();
                        } else {
                            itemName = itemCell.getText().trim();
                        }

                        if (!slotName.isEmpty() && !itemName.isEmpty()) {
                            bisItems.put(slotName, itemName);
                        }
                    }
                }

                return bisItems;
            } catch (Exception e) {
                log.error("Error finding toggle or table: {}", e.getMessage());
                return Collections.emptyMap();
            }
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    // Önbellek için iç sınıf
    private static class CachedBisData {
        private final Map<String, String> bisItems;
        private final LocalDateTime timestamp;

        public CachedBisData(Map<String, String> bisItems) {
            this.bisItems = new LinkedHashMap<>(bisItems);
            this.timestamp = LocalDateTime.now();
        }

        public Map<String, String> getBisItems() {
            return bisItems;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(timestamp.plus(CACHE_DURATION));
        }
    }
} 