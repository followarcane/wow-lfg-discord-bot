package followarcane.wow_lfg_discord_bot.application.controller;

import followarcane.wow_lfg_discord_bot.application.request.CodeRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.service.AuthService;
import followarcane.wow_lfg_discord_bot.application.service.DiscordBotService;
import followarcane.wow_lfg_discord_bot.application.service.RequestConverter;
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
    private final AuthService authService;
    private final RequestConverter requestConverter;

    public OAuthController(DiscordBotService discordBotService, AuthService authService, RequestConverter requestConverter) {
        this.discordBotService = discordBotService;
        this.authService = authService;
        this.requestConverter = requestConverter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CodeRequest codeRequest) {
        String tokenResponse = discordBotService.exchangeCodeForToken(codeRequest.getCode(), true);
        if (tokenResponse != null) {
            UserRequest userRequest = discordBotService.getUserDetails(tokenResponse);
            String jwtToken = authService.generateToken(userRequest.getDiscordId());
            return ResponseEntity.ok(Map.of("user", userRequest, "token", jwtToken));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while adding the bot!");
        }
    }

    @PostMapping("/inviteBot")
    public ResponseEntity<?> inviteBot(@RequestBody CodeRequest codeRequest, @RequestHeader("Authorization") String jwtToken) {
        String userDiscordId = authService.getUserDiscordIdFromToken(jwtToken.replace("Bearer ", ""));
        if (userDiscordId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token!");
        }

        String tokenResponse = discordBotService.exchangeCodeForToken(codeRequest.getCode(), false);
        if (tokenResponse != null) {
            UserRequest userRequest = requestConverter.convertToUserRequest(discordBotService.handleServerInvite(tokenResponse));
            return ResponseEntity.ok(Map.of("user", userRequest));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while inviting the bot!");
        }
    }
}
