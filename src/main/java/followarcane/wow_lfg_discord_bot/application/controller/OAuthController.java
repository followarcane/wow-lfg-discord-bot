package followarcane.wow_lfg_discord_bot.application.controller;

import followarcane.wow_lfg_discord_bot.application.request.CodeRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.service.DiscordBotService;
import followarcane.wow_lfg_discord_bot.application.service.DiscordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oauth2")
public class OAuthController {

    private final DiscordBotService discordBotService;
    private final DiscordService discordService;

    public OAuthController(DiscordBotService discordBotService, DiscordService discordService) {
        this.discordBotService = discordBotService;
        this.discordService = discordService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CodeRequest codeRequest) {
        String tokenResponse = discordBotService.exchangeCodeForToken(codeRequest.getCode(), true);
        if (tokenResponse != null) {
            Map<String, Object> response = discordBotService.getUserDetails(tokenResponse);
            UserRequest userRequest = (UserRequest) response.get("user");

            discordService.addUser(userRequest);

            return ResponseEntity.ok(userRequest);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while adding the bot!");
        }
    }

    @PostMapping("/inviteBot")
    public ResponseEntity<?> inviteBot(@RequestBody CodeRequest codeRequest) {
        String tokenResponse = discordBotService.exchangeCodeForToken(codeRequest.getCode(), false);
        if (tokenResponse != null) {
            discordBotService.handleServerInvite(tokenResponse);
            return ResponseEntity.ok("Bot invited successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while inviting the bot!");
        }
    }


    @GetMapping("/getServers")
    public ResponseEntity<?> getServers(@RequestParam("userId") String userId) {
        return ResponseEntity.ok(discordService.getServersByUserDiscordId(userId));
    }

}
