package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bu test sınıfı Spring Boot bağlamını yüklemeden manuel olarak BisGearService'i test eder.
 */
public class BisGearServiceManualTest {

    private BisGearService bisGearService;
    private ClassColorCodeHelper classColorCodeHelper;

    @BeforeEach
    void setUp() {
        // Bileşenleri manuel olarak oluştur
        classColorCodeHelper = new ClassColorCodeHelper();

        // BisGearService'i oluştur
        bisGearService = new BisGearService(classColorCodeHelper);
    }

    @Test
    void testSpecificClassSpecAndHeroTalent() {
        // Test için belirli bir sınıf, spec ve hero talent
        String className = "Death_knight";
        String specName = "";
        String heroTalent = "";
        String slot = "all";

        // API çağrısını yap
        EmbedBuilder result = bisGearService.createBisGearEmbed(slot, className, specName, heroTalent);

        // Sonuçları kontrol et
        assertNotNull(result);
        System.out.println("Embed başlığı: " + result.build().getTitle());

        // Embed alanlarını kontrol et
        result.getFields().forEach(field -> {
            System.out.println("Alan: " + field.getName());
            System.out.println("Değer: " + field.getValue());
            System.out.println("---");
        });

        // En az bir ekipman parçası olduğunu kontrol et
        assertTrue(result.getFields().size() > 0, "En az bir ekipman parçası olmalı");
    }

    @Test
    void testAllHeroTalents() {
        // Test için belirli bir sınıf ve spec, hero talent boş
        String className = "Death_knight";
        String specName = "Blood";
        String heroTalent = ""; // Boş bırakarak tüm hero talentları getir
        String slot = "all";

        // API çağrısını yap
        EmbedBuilder result = bisGearService.createBisGearEmbed(slot, className, specName, heroTalent);

        // Sonuçları kontrol et
        assertNotNull(result);
        System.out.println("Embed başlığı: " + result.build().getTitle());

        // Embed alanlarını kontrol et
        result.getFields().forEach(field -> {
            System.out.println("Alan: " + field.getName());
            System.out.println("Değer: " + field.getValue());
            System.out.println("---");
        });

        // En az bir hero talent için ekipman olduğunu kontrol et
        assertTrue(result.getFields().size() > 0, "En az bir hero talent için ekipman olmalı");
    }

    @Test
    void testAllSpecs() {
        // Test için belirli bir sınıf, spec ve hero talent boş
        String className = "Death_knight";
        String specName = "";
        String heroTalent = "";
        String slot = "all";

        // API çağrısını yap
        EmbedBuilder result = bisGearService.createBisGearEmbed(slot, className, specName, heroTalent);

        // Sonuçları kontrol et
        assertNotNull(result);
        System.out.println("Embed başlığı: " + result.build().getTitle());

        // Embed alanlarını kontrol et
        result.getFields().forEach(field -> {
            System.out.println("Alan: " + field.getName());
            System.out.println("Değer: " + field.getValue());
            System.out.println("---");
        });

        // En az bir ekipman parçası olduğunu kontrol et
        assertTrue(result.getFields().size() > 0, "En az bir ekipman parçası olmalı");
    }

    @Test
    void testFilterBySlot() {
        // Test için belirli bir sınıf, spec, hero talent ve slot
        String className = "Death_knight";
        String specName = "Blood";
        String heroTalent = "Deathbringer";
        String slot = "head";

        // API çağrısını yap
        EmbedBuilder result = bisGearService.createBisGearEmbed(slot, className, specName, heroTalent);

        // Sonuçları kontrol et
        assertNotNull(result);
        System.out.println("Embed başlığı: " + result.build().getTitle());

        // Embed alanlarını kontrol et
        result.getFields().forEach(field -> {
            System.out.println("Alan: " + field.getName());
            System.out.println("Değer: " + field.getValue());
            System.out.println("---");
        });

        // Sadece head slotu için ekipman olduğunu kontrol et
        assertTrue(result.getFields().size() > 0, "Head slotu için ekipman olmalı");
        boolean hasHeadSlot = false;
        for (net.dv8tion.jda.api.entities.MessageEmbed.Field field : result.getFields()) {
            if (field.getName().equalsIgnoreCase("Head")) {
                hasHeadSlot = true;
                break;
            }
        }
        assertTrue(hasHeadSlot, "Head slotu bulunmalı");
    }

    @Test
    void testCacheRefresh() throws Exception {
        // Önbelleği temizle
        Field cacheField = BisGearService.class.getDeclaredField("bisCache");
        cacheField.setAccessible(true);
        Map<String, Object> cache = (Map<String, Object>) cacheField.get(bisGearService);
        cache.clear();

        // İlk çağrı - önbellek boş olmalı
        System.out.println("İlk çağrı öncesi önbellek boyutu: " + cache.size());

        // Death Knight sınıfı için BIS ekipman bilgilerini içeren bir embed oluştur
        bisGearService.createBisGearEmbed("all", "death_knight", "blood", "deathbringer");

        // Önbellekte en az bir öğe olduğunu doğrula
        System.out.println("İlk çağrı sonrası önbellek boyutu: " + cache.size());
        assertTrue(cache.size() > 0, "Önbellekte en az bir öğe olmalı");

        // İkinci çağrı - önbellekten alınmalı
        bisGearService.createBisGearEmbed("all", "death_knight", "blood", "deathbringer");

        // Önbelleği yenile
        System.out.println("Önbelleği yenileme öncesi önbellek boyutu: " + cache.size());
        bisGearService.refreshCache();
        System.out.println("Önbelleği yenileme sonrası önbellek boyutu: " + cache.size());

        // Önbellekte hala öğeler olduğunu doğrula
        assertTrue(cache.size() > 0, "Önbellekte hala öğeler olmalı");
    }

    @Test
    void testCacheExpiry() throws Exception {
        // Önbelleği temizle
        Field cacheField = BisGearService.class.getDeclaredField("bisCache");
        cacheField.setAccessible(true);
        Map<String, Object> cache = (Map<String, Object>) cacheField.get(bisGearService);
        cache.clear();

        // Death Knight sınıfı için BIS ekipman bilgilerini içeren bir embed oluştur
        bisGearService.createBisGearEmbed("all", "death_knight", "blood", "deathbringer");

        // Önbellekte en az bir öğe olduğunu doğrula
        assertTrue(cache.size() > 0, "Önbellekte en az bir öğe olmalı");

        // Önbellek süresini 0 olarak ayarla (hemen süresi dolsun)
        Field cacheDurationField = BisGearService.class.getDeclaredField("CACHE_DURATION");
        cacheDurationField.setAccessible(true);
        Object originalDuration = cacheDurationField.get(null);
        cacheDurationField.set(null, java.time.Duration.ofSeconds(0));

        // Biraz bekle
        Thread.sleep(1000);

        // Tekrar çağır - önbellek süresi dolduğu için yeniden çekilmeli
        bisGearService.createBisGearEmbed("all", "death_knight", "blood", "deathbringer");

        // Önbellek süresini geri ayarla
        cacheDurationField.set(null, originalDuration);
    }

    @Test
    void testSelenium() {
        // Test için belirli bir sınıf, spec ve hero talent
        String className = "Death_Knight";
        String specName = "Blood";
        String heroTalent = "Deathbringer";
        String slot = "all";

        // Selenium metodunu çağır
        Map<String, String> bisItems = bisGearService.fetchBisGearWithSelenium(slot, className, specName, heroTalent);

        // Sonuçları kontrol et
        assertNotNull(bisItems);
        System.out.println("Found " + bisItems.size() + " BIS items");

        // BIS ekipmanları kontrol et
        bisItems.forEach((key, value) -> {
            System.out.println("Slot: " + key);
            System.out.println("Item: " + value);
            System.out.println("---");
        });

        // En az bir ekipman parçası olduğunu kontrol et
        assertTrue(bisItems.size() > 0, "En az bir ekipman parçası olmalı");
    }
} 