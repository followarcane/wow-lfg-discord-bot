package followarcane.wow_lfg_discord_bot.security.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

public class TokenValidationUtils {

    private static final String DISCORD_API_URL = "https://discord.com/api/v10";

    public static String validateAccessToken(String token, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(DISCORD_API_URL + "/users/@me", HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> userData = response.getBody();
            return (String) userData.get("id");
        }

        return null;
    }
}
