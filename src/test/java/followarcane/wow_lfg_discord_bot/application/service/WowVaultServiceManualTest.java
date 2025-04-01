package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Bu test sınıfı Spring Boot bağlamını yüklemeden manuel olarak servisi test eder.
 */
public class WowVaultServiceManualTest {

    private WowVaultService wowVaultService;
    private RestTemplate restTemplate;
    private ClassColorCodeHelper classColorCodeHelper;
    private BattleNetApiService battleNetApiService;

    @BeforeEach
    void setUp() {
        // Bileşenleri manuel olarak oluştur
        restTemplate = new RestTemplate();
        classColorCodeHelper = new ClassColorCodeHelper();

        // BattleNetApiService'i oluştur
        battleNetApiService = new BattleNetApiService(restTemplate);

        // Gerekli değerleri BattleNetApiService'e ayarla
        ReflectionTestUtils.setField(battleNetApiService, "battleNetClientApi", "xxxx");
        ReflectionTestUtils.setField(battleNetApiService, "battleNetClientSecret", "xxxx");
        ReflectionTestUtils.setField(battleNetApiService, "battleNetApiUrl", "https://eu.api.blizzard.com");

        // WowVaultService'i oluştur ve BattleNetApiService'i enjekte et
        wowVaultService = new WowVaultService(restTemplate, classColorCodeHelper, battleNetApiService);
    }

    @Test
    void testRealApiCall() {
        // Test için bilinen bir karakter kullan
        String characterName = "Shadlynn";
        String realm = "twisting-nether";
        String region = "eu";

        // API çağrısını yap
        EmbedBuilder result = wowVaultService.createVaultEmbed(characterName, realm, region);

        // Sonuçları kontrol et
        assertNotNull(result);
        System.out.println("Embed başlığı: " + result.build().getTitle());

        // Embed alanlarını kontrol et
        result.getFields().forEach(field -> {
            System.out.println("Alan: " + field.getName());
            System.out.println("Değer: " + field.getValue());
            System.out.println("---");
        });
    }

    @Test
    void testMultipleCharacters() {
        // Test için birkaç farklı karakter
        String[][] characters = {
                {"Shadlynn", "twisting-nether", "eu"},
                {"Remustr", "twisting-nether", "eu"},
                {"Luwynn", "twisting-nether", "eu"}
        };

        for (String[] character : characters) {
            System.out.println("\nKarakter testi: " + character[0] + " - " + character[1] + " - " + character[2]);

            // API çağrısını yap
            EmbedBuilder result = wowVaultService.createVaultEmbed(character[0], character[1], character[2]);

            // Sonuçları kontrol et
            assertNotNull(result);
            System.out.println("Embed başlığı: " + result.build().getTitle());

            // M+ ve Raid ödüllerini kontrol et
            result.getFields().stream()
                    .filter(field -> field.getName().contains("Rewards"))
                    .forEach(field -> {
                        System.out.println("Alan: " + field.getName());
                        System.out.println("Değer: " + field.getValue());
                        System.out.println("---");
                    });
        }
    }
} 