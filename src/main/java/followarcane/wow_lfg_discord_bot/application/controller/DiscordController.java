package followarcane.wow_lfg_discord_bot.application.controller;

import followarcane.wow_lfg_discord_bot.application.request.DiscordChannelRequest;
import followarcane.wow_lfg_discord_bot.application.request.DiscordServerRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserSettingsRequest;
import followarcane.wow_lfg_discord_bot.application.service.AuthService;
import followarcane.wow_lfg_discord_bot.application.service.DiscordService;
import followarcane.wow_lfg_discord_bot.application.service.RequestConverter;
import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/discord")
public class DiscordController {

    private final DiscordService discordService;
    private final RequestConverter requestConverter;
    private final AuthService authService;

    private String getUserIdFromToken(String token) {
        try {
            return authService.getUserDiscordIdFromToken(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    @PostMapping("/addServer")
    public ResponseEntity<?> addServer(@RequestBody DiscordServerRequest discordServerRequest, @RequestHeader("Authorization") String token) {
        //String userId = getUserIdFromToken(token);
        discordService.addServer(discordServerRequest);
        return ResponseEntity.ok("Server added successfully.");
    }

    @PostMapping("/addChannel")
    public ResponseEntity<?> addChannel(@RequestBody DiscordChannelRequest discordChannelRequest, @RequestHeader("Authorization") String token) {
        discordService.addChannel(discordChannelRequest);
        return ResponseEntity.ok("Channel added successfully.");
    }

    @PostMapping("/addUser")
    public ResponseEntity<?> addUser(@RequestBody UserRequest userRequest, @RequestHeader("Authorization") String token) {
        discordService.addUser(userRequest);
        return ResponseEntity.ok("User added successfully.");
    }

    @PostMapping("/addUserSettings")
    public ResponseEntity<?> addUserSettings(@RequestBody UserSettingsRequest userSettingsRequest, @RequestHeader("Authorization") String token) {
        discordService.addUserSettings(userSettingsRequest);
        return ResponseEntity.ok("Settings added successfully.");
    }

    @GetMapping("/getUserSettings/{serverId}")
    public ResponseEntity<?> getSettings(@PathVariable String serverId, @RequestHeader("Authorization") String token) {
        String userId = getUserIdFromToken(token);
        UserSettings userSettings = discordService.getSettingsByServerIdAndUserId(serverId, Long.valueOf(userId));
        return ResponseEntity.ok(requestConverter.convertToUserSettingsDTO(userSettings));
    }

    @GetMapping("/getServers")
    public ResponseEntity<?> getServers(@RequestHeader("Authorization") String token) {
        String userId = getUserIdFromToken(token);
        return ResponseEntity.ok(discordService.getServersByUserDiscordId(userId));
    }
}
