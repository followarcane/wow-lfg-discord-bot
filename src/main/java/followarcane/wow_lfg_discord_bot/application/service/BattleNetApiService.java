package followarcane.wow_lfg_discord_bot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class BattleNetApiService {

    private final RestTemplate restTemplate;

    @Value("${battle-net.client.id}")
    private String battleNetClientApi;

    @Value("${battle-net.client.secret}")
    private String battleNetClientSecret;

    @Value("${battle-net.api.url}")
    private String battleNetApiUrl;

    // Token önbelleği için değişkenler
    private String blizzardToken;
    private long tokenExpiry = 0;

    public BattleNetApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Blizzard API'den karakter raid verilerini çeker
     */
    public JsonNode fetchCharacterRaidData(String name, String realm, String region) {
        try {
            String token = getBlizzardToken();
            if (token == null) {
                log.error("Failed to get Blizzard API token");
                return null;
            }

            String blizzardUrl = UriComponentsBuilder.fromHttpUrl(battleNetApiUrl + "/profile/wow/character/" +
                            realm.toLowerCase() + "/" + name.toLowerCase() + "/encounters/raids")
                    .queryParam("namespace", "profile-" + region)
                    .queryParam("locale", "en_US")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            log.info("Fetching Blizzard data from: {}", blizzardUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    blizzardUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new ObjectMapper().readTree(response.getBody());
            }
        } catch (Exception e) {
            log.error("Error fetching Blizzard data: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Blizzard API token'ı alır (önbellek kullanarak)
     */
    public String getBlizzardToken() {
        long now = System.currentTimeMillis();
        if (blizzardToken == null || now >= tokenExpiry) {
            try {
                String tokenUrl = "https://oauth.battle.net/token";

                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth(battleNetClientApi, battleNetClientSecret);
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("grant_type", "client_credentials");

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

                log.info("Requesting Blizzard API token");
                ResponseEntity<String> response = restTemplate.exchange(
                        tokenUrl,
                        HttpMethod.POST,
                        request,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
                    blizzardToken = jsonNode.get("access_token").asText();
                    int expiresIn = jsonNode.get("expires_in").asInt();
                    tokenExpiry = now + (expiresIn * 1000) - 300000; // 5 dakika önce yenile
                    log.info("Blizzard API token obtained, expires in {} seconds", expiresIn);
                    return blizzardToken;
                }
            } catch (Exception e) {
                log.error("Error getting Blizzard API token: {}", e.getMessage(), e);
            }
            return null;
        } else
            log.info("Blizzard API token used from cache");
        return blizzardToken;
    }

    /**
     * Blizzard API'den karakter istatistiklerini çeker
     */
    public JsonNode fetchCharacterStats(String name, String realm, String region) {
        try {
            // Özel karakterleri URL kodlaması ile değiştir
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString());
            String encodedRealm = URLEncoder.encode(realm.toLowerCase().replace("-", " "), StandardCharsets.UTF_8.toString());
            
            // API URL'sini oluştur
            String url = String.format("%s/profile/wow/character/%s/%s/statistics?namespace=profile-%s&locale=en_US",
                    getApiBaseUrl(region), encodedRealm, encodedName.toLowerCase(), region.toLowerCase());
            
            log.info("Fetching character stats from: {}", url);

            // API isteği yap
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(getBlizzardToken());
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // Yanıtı işle
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                return new ObjectMapper().readTree(responseBody);
            } else {
                log.error("Error fetching character stats: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching character stats: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getApiBaseUrl(String region) {
        // Bu metodun içeriği, region'a göre API URL'ini döndürmesi gerekiyor
        // Bu örnekte, region'a göre API URL'ini döndüren bir basit uygulama kullanılmıştır
        // Gerçek uygulamada, region'a göre API URL'ini döndüren uygun bir mekanizma kullanılmalıdır
        return battleNetApiUrl;
    }
} 