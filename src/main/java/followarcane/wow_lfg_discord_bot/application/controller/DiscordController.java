package followarcane.wow_lfg_discord_bot.application.controller;

import followarcane.wow_lfg_discord_bot.application.request.DiscordChannelRequest;
import followarcane.wow_lfg_discord_bot.application.request.DiscordServerRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserRequest;
import followarcane.wow_lfg_discord_bot.application.request.UserSettingsRequest;
import followarcane.wow_lfg_discord_bot.application.service.DiscordService;
import followarcane.wow_lfg_discord_bot.domain.model.User;
import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/discord")
public class DiscordController {

    private final DiscordService discordService;

    @PostMapping("/addServer")
    public ResponseEntity<?> addServer(@RequestBody DiscordServerRequest discordServerRequest) {
        discordService.addServer(discordServerRequest);
        return ResponseEntity.ok("Server added successfully.");
    }

    @PostMapping("/addChannel")
    public ResponseEntity<?> addChannel(@RequestBody DiscordChannelRequest discordChannelRequest) {
        discordService.addChannel(discordChannelRequest);
        return ResponseEntity.ok("Channel added successfully.");
    }

    @PostMapping("/addUser")
    public ResponseEntity<?> addUser(@RequestBody UserRequest userRequest) {
        discordService.addUser(userRequest);
        return ResponseEntity.ok("User added successfully.");
    }

    @PostMapping("/addUserSettings")
    public ResponseEntity<?> addUserSettings(@RequestBody UserSettingsRequest userSettingsRequest) {
        discordService.addUserSettings(userSettingsRequest);
        return ResponseEntity.ok("Settings added successfully.");
    }
}

