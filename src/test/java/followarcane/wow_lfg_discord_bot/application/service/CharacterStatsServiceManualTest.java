package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.util.ClassColorCodeHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Bu test sınıfı gerçek API istekleri yapar.
 * Bu testleri çalıştırmak için geçerli API anahtarlarına sahip olmanız gerekir.
 * Bu testler CI/CD pipeline'larında çalıştırılmamalıdır.
 */
public class CharacterStatsServiceManualTest {

    private CharacterStatsService characterStatsService;
    private BattleNetApiService battleNetApiService;
    private RestTemplate restTemplate;
    private ClassColorCodeHelper classColorCodeHelper;

    @BeforeEach
    public void setUp() {
        // RestTemplate oluştur
        restTemplate = new RestTemplate();

        // ClassColorCodeHelper oluştur
        classColorCodeHelper = new ClassColorCodeHelper();

        // BattleNetApiService oluştur
        battleNetApiService = new BattleNetApiService(restTemplate);

        // Test için API anahtarlarını manuel olarak ayarla
        // Bu değerleri gerçek API anahtarlarınızla değiştirin
        try {
            java.lang.reflect.Field clientIdField = BattleNetApiService.class.getDeclaredField("battleNetClientApi");
            clientIdField.setAccessible(true);
            clientIdField.set(battleNetApiService, "1f424a73215442b18894e356cc64f9bb");

            java.lang.reflect.Field clientSecretField = BattleNetApiService.class.getDeclaredField("battleNetClientSecret");
            clientSecretField.setAccessible(true);
            clientSecretField.set(battleNetApiService, "47g5cKKh36BJDRn1Vix815dTLT7Kwmry");

            java.lang.reflect.Field apiUrlField = BattleNetApiService.class.getDeclaredField("battleNetApiUrl");
            apiUrlField.setAccessible(true);
            apiUrlField.set(battleNetApiService, "https://eu.api.blizzard.com");
        } catch (Exception e) {
            System.err.println("API anahtarlarını ayarlarken hata oluştu: " + e.getMessage());
        }

        // CharacterStatsService oluştur
        characterStatsService = new CharacterStatsService(restTemplate, classColorCodeHelper, battleNetApiService);
    }

    @Test
    public void testCreateCharacterStatsEmbed() {
        // Test için karakter bilgileri
        String characterName = "Hølyimãm";
        String realm = "Twisting-Nether";
        String region = "eu";

        System.out.println("Fetching character stats for: " + characterName + " on " + realm + "-" + region);

        // Karakter istatistiklerini al
        EmbedBuilder embed = characterStatsService.createCharacterStatsEmbed(characterName, realm, region);

        // Sonuçları kontrol et
        System.out.println("Embed title: " + embed.build().getTitle());
        System.out.println("Embed color: " + embed.build().getColor());

        // Embed alanlarını yazdır
        List<MessageEmbed.Field> fields = embed.build().getFields();
        System.out.println("Number of fields: " + fields.size());

        for (MessageEmbed.Field field : fields) {
            System.out.println("\nField name: " + field.getName());
            System.out.println("Field value: " + field.getValue());
        }

        // Hata durumunu kontrol et
        if (embed.build().getColor().getRGB() == java.awt.Color.RED.getRGB()) {
            System.err.println("Error: " + embed.build().getDescription());
        } else {
            System.out.println("Character stats fetched successfully!");
        }
    }

    @Test
    public void testCreateCharacterStatsEmbedWithSpecialCharacters() {
        // Özel karakterler içeren karakter adı
        String characterName = "Hølyimãm";
        String realm = "Twisting Nether";
        String region = "eu";

        System.out.println("Fetching character stats for: " + characterName + " on " + realm + "-" + region);

        // Karakter istatistiklerini al
        EmbedBuilder embed = characterStatsService.createCharacterStatsEmbed(characterName, realm, region);

        // Sonuçları kontrol et
        System.out.println("Embed title: " + embed.build().getTitle());

        // Hata durumunu kontrol et
        if (embed.build().getColor().getRGB() == java.awt.Color.RED.getRGB()) {
            System.err.println("Error: " + embed.build().getDescription());
        } else {
            System.out.println("Character stats fetched successfully!");
        }
    }

    @Test
    public void testCreateCharacterStatsEmbedWithNonExistentCharacter() {
        // Var olmayan karakter
        String characterName = "NonExistentCharacter";
        String realm = "Twisting Nether";
        String region = "eu";

        System.out.println("Fetching character stats for: " + characterName + " on " + realm + "-" + region);

        // Karakter istatistiklerini al
        EmbedBuilder embed = characterStatsService.createCharacterStatsEmbed(characterName, realm, region);

        // Sonuçları kontrol et
        System.out.println("Embed title: " + embed.build().getTitle());
        System.out.println("Embed description: " + embed.build().getDescription());

        // Hata durumunu kontrol et
        if (embed.build().getColor().getRGB() == java.awt.Color.RED.getRGB()) {
            System.out.println("Expected error received: " + embed.build().getDescription());
        } else {
            System.err.println("Unexpected success for non-existent character!");
        }
    }
} 