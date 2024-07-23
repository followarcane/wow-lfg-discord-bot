package followarcane.wow_lfg_discord_bot.application.controller;

import followarcane.wow_lfg_discord_bot.application.request.DiscordChannelRequest;
import followarcane.wow_lfg_discord_bot.application.request.DiscordServerRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserSettingsRequest;
import followarcane.wow_lfg_discord_bot.application.service.AuthService;
import followarcane.wow_lfg_discord_bot.application.service.DiscordBotService;
import followarcane.wow_lfg_discord_bot.application.service.DiscordService;
import followarcane.wow_lfg_discord_bot.application.service.RequestConverter;
import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import followarcane.wow_lfg_discord_bot.security.util.TokenValidationUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/discord")
public class DiscordController {

    private final DiscordService discordService;
    private final RequestConverter requestConverter;
    private final DiscordBotService discordBotService;

    @PostMapping("/addServer")
    public ResponseEntity<?> addServer(@RequestBody DiscordServerRequest discordServerRequest, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse != null) return validationResponse;
        discordService.addServer(discordServerRequest);
        return ResponseEntity.ok("Server added successfully.");
    }

    @PostMapping("/addChannel")
    public ResponseEntity<?> addChannel(@RequestBody DiscordChannelRequest discordChannelRequest, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse != null) return validationResponse;
        discordService.addChannel(discordChannelRequest);
        return ResponseEntity.ok("Channel added successfully.");
    }

    @PostMapping("/addUser")
    public ResponseEntity<?> addUser(@RequestBody UserRequest userRequest, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse != null) return validationResponse;
        discordService.addUser(userRequest);
        return ResponseEntity.ok("User added successfully.");
    }

    @PostMapping("/addUserSettings")
    public ResponseEntity<?> addUserSettings(@RequestBody UserSettingsRequest userSettingsRequest, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse != null) return validationResponse;
        String userId = validationResponse.getBody();
        discordService.addUserSettings(userSettingsRequest, userId);
        return ResponseEntity.ok("Settings added successfully.");
    }

    @GetMapping("{guild}/settings/{featureId}")
    public ResponseEntity<?> getSettings(@PathVariable String guild, @PathVariable String featureId, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse != null) return validationResponse;
        String userId = validationResponse.getBody();
        UserSettings userSettings = discordService.getSettingsByServerIdAndUserId(guild, userId);
        if (userSettings == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User settings not found");
        }
        return ResponseEntity.ok(requestConverter.convertToUserSettingsDTO(userSettings));
    }

    @GetMapping("/getGuild/{guild}")
    public ResponseEntity<?> getGuild(@PathVariable String guild, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse != null) return validationResponse;
        Map<String, Object> response = discordBotService.getGuildDetails(token, guild);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Guild not found");
    }

    @GetMapping("/getServers")
    public ResponseEntity<?> getServers(@RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse != null) return validationResponse;
        String userId = validationResponse.getBody();
        return ResponseEntity.ok(discordService.getServersByUserDiscordId(userId));
    }
}

