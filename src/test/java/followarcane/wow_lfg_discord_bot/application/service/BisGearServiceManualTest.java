package followarcane.wow_lfg_discord_bot.application.service;


import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bu test sınıfı Spring Boot bağlamını yüklemeden manuel olarak BisGearService'i test eder.
 */
public class BisGearServiceManualTest {

    private BisGearService bisGearService;
    private ClassColorCodeHelper classColorCodeHelper;

    @BeforeEach
    public void setUp() {
        // ClassColorCodeHelper'ı oluştur
        classColorCodeHelper = new ClassColorCodeHelper();

        // BisGearService'i oluştur
        bisGearService = new BisGearService(classColorCodeHelper);
    }

    @Test
    public void testLoadBisData() throws Exception {
        // BisGearService'in cache alanına erişim sağla
        Field cacheField = BisGearService.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        Map<String, Map<String, Map<String, Object>>> cache =
                (Map<String, Map<String, Map<String, Object>>>) cacheField.get(bisGearService);

        // Önbelleğin boş olmadığını doğrula
        assertFalse(cache.isEmpty());

        // Bazı örnek profillerin varlığını kontrol et
        assertTrue(cache.containsKey("Death_Knight_Blood_Deathbringer"));
        assertTrue(cache.containsKey("Mage_Frost_Frostfire"));

        // Örnek bir profil için slot sayısını kontrol et
        Map<String, Map<String, Object>> deathKnightGear = cache.get("Death_Knight_Blood_Deathbringer");
        assertTrue(deathKnightGear.size() > 10); // En az 10 slot olmalı

        // Örnek bir slot için item bilgilerini kontrol et
        Map<String, Object> headItem = deathKnightGear.get("Head");
        assertNotNull(headItem);
        assertTrue(headItem.containsKey("name"));
        assertTrue(headItem.containsKey("url"));
        assertTrue(headItem.containsKey("source"));

        // Önbellek içeriğini yazdır
        System.out.println("Cache size: " + cache.size());
        System.out.println("Available profiles:");
        for (String key : cache.keySet()) {
            System.out.println("  " + key);
        }
    }

    @Test
    public void testGetBisGear() {
        // Death Knight Blood Deathbringer için BIS ekipmanları getir
        Map<String, Map<String, Object>> bisGear = bisGearService.getBisGear("Death_Knight", "Blood", "Deathbringer");
        
        // Sonuçları kontrol et
        assertNotNull(bisGear);
        assertFalse(bisGear.isEmpty());

        // Head slotunu kontrol et
        assertTrue(bisGear.containsKey("Head"));
        Map<String, Object> headItem = bisGear.get("Head");
        assertNotNull(headItem.get("name"));
        assertNotNull(headItem.get("url"));

        // Sonuçları yazdır
        System.out.println("BIS gear for Death Knight Blood Deathbringer:");
        for (Map.Entry<String, Map<String, Object>> entry : bisGear.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().get("name"));
        }
    }

    @Test
    public void testGetBisGearForSlot() {
        // Death Knight Blood Deathbringer için Head slotundaki BIS ekipmanı getir
        Map<String, Object> headItem = bisGearService.getBisGearForSlot("Head", "Death_Knight", "Blood", "Deathbringer");
        
        // Sonuçları kontrol et
        assertNotNull(headItem);
        assertFalse(headItem.isEmpty());
        assertNotNull(headItem.get("name"));
        assertNotNull(headItem.get("url"));

        // Sonuçları yazdır
        System.out.println("BIS Head item for Death Knight Blood Deathbringer:");
        System.out.println("  Name: " + headItem.get("name"));
        System.out.println("  URL: " + headItem.get("url"));
        System.out.println("  Source: " + headItem.get("source"));
        System.out.println("  Stats: " + headItem.get("stats"));
    }

    @Test
    public void testRefreshCache() throws Exception {
        // BisGearService'in cache alanına erişim sağla
        Field cacheField = BisGearService.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        Map<String, Map<String, Map<String, Object>>> cache =
                (Map<String, Map<String, Map<String, Object>>>) cacheField.get(bisGearService);
        
        // Önbelleği yenile
        System.out.println("Önbelleği yenileme öncesi önbellek boyutu: " + cache.size());
        bisGearService.refreshCache();
        System.out.println("Önbelleği yenileme sonrası önbellek boyutu: " + cache.size());

        // Önbellekte hala öğeler olduğunu doğrula
        assertFalse(cache.isEmpty());
    }

    @Test
    public void testFetchBisGearWithSelenium() {
        // Test parametreleri
        String slot = "Head";
        String className = "Death_Knight";
        String specName = "Blood";
        String heroTalent = "Deathbringer";

        // Selenium metodunu çağır
        Map<String, String> bisItems = bisGearService.fetchBisGearWithSelenium(slot, className, specName, heroTalent);

        // Sonuçları kontrol et
        assertNotNull(bisItems);
        assertFalse(bisItems.isEmpty());
        assertNotNull(bisItems.get("name"));
        assertNotNull(bisItems.get("url"));

        // Sonuçları yazdır
        System.out.println("BIS Head item for Death Knight Blood Deathbringer (via Selenium):");
        System.out.println("  Name: " + bisItems.get("name"));
        System.out.println("  URL: " + bisItems.get("url"));
        System.out.println("  Source: " + bisItems.get("source"));
        System.out.println("  Stats: " + bisItems.get("stats"));
    }
} 