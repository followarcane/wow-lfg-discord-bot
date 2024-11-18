package followarcane.wow_lfg_discord_bot.application.controller;

import followarcane.wow_lfg_discord_bot.application.request.CodeRequest;
import followarcane.wow_lfg_discord_bot.application.service.DiscordBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oauth2")
public class OAuthController {

    private final DiscordBotService discordBotService;

    public OAuthController(DiscordBotService discordBotService) {
        this.discordBotService = discordBotService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CodeRequest codeRequest) {
        log.info("Login endpoint triggered with code: {}", codeRequest.getCode());

        String tokenResponse = discordBotService.exchangeCodeForToken(codeRequest.getCode(), true);
        if (tokenResponse != null) {
            Map<String, Object> response = discordBotService.getUserDetails(tokenResponse);
            return ResponseEntity.ok(Map.of("token", response));
        } else {
            log.error("An error occurred while adding the bot");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while adding the bot!");
        }
    }

    @PostMapping("/inviteBot")
    public ResponseEntity<?> inviteBot(@RequestBody CodeRequest codeRequest) {
        String tokenResponse = discordBotService.exchangeCodeForToken(codeRequest.getCode(), false);
        if (tokenResponse != null) {
            Map<String, Object> response = discordBotService.handleServerInvite(tokenResponse);
            return ResponseEntity.ok(Map.of("token", response.get("tokenResponse")));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while inviting the bot!");
        }
    }

}
