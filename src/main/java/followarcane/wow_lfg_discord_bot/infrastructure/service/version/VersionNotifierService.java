package followarcane.wow_lfg_discord_bot.infrastructure.service.version;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionNotifierService {

    @Value("${app.version}")
    private String currentVersion;

    @Value("${app.discord.webhook-url}")
    private String discordWebhookUrl;

    private final VersionStorageService versionStorage;

    @EventListener(ApplicationReadyEvent.class)
    public void notifyVersionChange() {
        String lastVersion = versionStorage.getLastVersion();

        if (!currentVersion.equals(lastVersion)) {
            sendDiscordNotification(currentVersion, lastVersion);
            versionStorage.saveCurrentVersion(currentVersion);
        }
    }

    private void sendDiscordNotification(String newVersion, String oldVersion) {
        String message = createDiscordEmbedMessage(newVersion, oldVersion);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(message, headers);
        ResponseEntity<String> response = restTemplate.exchange(discordWebhookUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("Version change notification sent to Discord: {} -> {}", oldVersion, newVersion);
        } else {
            log.error("Failed to send version change notification to Discord. Response: {}", response.getBody());
        }
    }

    private String createDiscordEmbedMessage(String newVersion, String oldVersion) {
        // Embed message fields
        JSONObject embed = new JSONObject();
        embed.put("title", "Azerite Deployment Notification");
        embed.put("description", String.format("Azerite has been updated from version **%s** to **%s**.", oldVersion, newVersion));
        embed.put("color", 16777215);//white

        JSONObject footer = new JSONObject();
        footer.put("text", "Powered by Azerite!");
        footer.put("icon_url", "https://i.imgur.com/fK2PvPV.png");
        embed.put("footer", footer);

        JSONArray embeds = new JSONArray();
        embeds.put(embed);

        JSONObject payload = new JSONObject();
        payload.put("embeds", embeds);

        return payload.toString();
    }
}
