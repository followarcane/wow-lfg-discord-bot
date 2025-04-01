package followarcane.wow_lfg_discord_bot.application.service;


import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
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
    public void testCreateBisGearEmbedWithOnlyClass() {
        // Sadece sınıf belirtildiğinde
        String className = "priest";
        String specName = "";
        String heroTalent = "";
        String slot = "";

        EmbedBuilder embed = bisGearService.createBisGearEmbed(slot, className, specName, heroTalent);

        // Embed'in oluşturulduğunu doğrula
        assertNotNull(embed);

        // Embed'in başlığını kontrol et
        String title = embed.build().getTitle();
        System.out.println("Title: " + title);

        // Embed'in açıklamasını kontrol et
        String description = embed.build().getDescription();
        System.out.println("Description: " + (description != null ? description : "No description"));

        // Embed'in alanlarını kontrol et
        List<MessageEmbed.Field> fields = embed.build().getFields();
        System.out.println("Number of fields: " + fields.size());
        for (MessageEmbed.Field field : fields) {
            System.out.println("Field: " + field.getName() + " - " + field.getValue());
        }
    }

    @Test
    public void testCreateBisGearEmbedWithClassAndSpec() {
        // Sınıf ve spec belirtildiğinde
        String className = "priest";
        String specName = "shadow";
        String heroTalent = "";
        String slot = "";

        EmbedBuilder embed = bisGearService.createBisGearEmbed(slot, className, specName, heroTalent);

        // Embed'in oluşturulduğunu doğrula
        assertNotNull(embed);

        // Embed'in başlığını kontrol et
        String title = embed.build().getTitle();
        System.out.println("Title: " + title);

        // Embed'in açıklamasını kontrol et
        String description = embed.build().getDescription();
        System.out.println("Description: " + (description != null ? description : "No description"));

        // Embed'in alanlarını kontrol et
        List<MessageEmbed.Field> fields = embed.build().getFields();
        System.out.println("Number of fields: " + fields.size());
        for (MessageEmbed.Field field : fields) {
            System.out.println("Field: " + field.getName() + " - " + field.getValue());
        }
    }

    @Test
    public void testCreateBisGearEmbedWithClassSpecAndHeroTalent() {
        // Sınıf, spec ve hero talent belirtildiğinde
        String className = "priest";
        String specName = "shadow";
        String heroTalent = "archon";
        String slot = "head";

        EmbedBuilder embed = bisGearService.createBisGearEmbed(slot, className, specName, heroTalent);

        // Embed'in oluşturulduğunu doğrula
        assertNotNull(embed);

        // Embed'in başlığını kontrol et
        String title = embed.build().getTitle();
        System.out.println("Title: " + title);

        // Embed'in açıklamasını kontrol et
        String description = embed.build().getDescription();
        System.out.println("Description: " + (description != null ? description : "No description"));

        // Embed'in alanlarını kontrol et
        List<MessageEmbed.Field> fields = embed.build().getFields();
        System.out.println("Number of fields: " + fields.size());
        for (MessageEmbed.Field field : fields) {
            System.out.println("Field: " + field.getName() + " - " + field.getValue());
        }
    }

    @Test
    public void testCreateBisGearEmbedWithClassSpecHeroTalentAndSlot() {
        // Sınıf, spec, hero talent ve slot belirtildiğinde
        String className = "priest";
        String specName = "shadow";
        String heroTalent = "archon";
        String slot = "Main Hand";

        EmbedBuilder embed = bisGearService.createBisGearEmbed(slot, className, specName, heroTalent);

        // Embed'in oluşturulduğunu doğrula
        assertNotNull(embed);

        // Embed'in başlığını kontrol et
        String title = embed.build().getTitle();
        System.out.println("Title: " + title);

        // Embed'in açıklamasını kontrol et
        String description = embed.build().getDescription();
        System.out.println("Description: " + (description != null ? description : "No description"));

        // Embed'in alanlarını kontrol et
        List<MessageEmbed.Field> fields = embed.build().getFields();
        System.out.println("Number of fields: " + fields.size());
        for (MessageEmbed.Field field : fields) {
            System.out.println("Field: " + field.getName() + " - " + field.getValue());
        }
    }
} 