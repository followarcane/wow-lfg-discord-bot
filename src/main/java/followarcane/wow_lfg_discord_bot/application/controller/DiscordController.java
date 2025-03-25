package followarcane.wow_lfg_discord_bot.application.controller;

import followarcane.wow_lfg_discord_bot.application.dto.TextChannelDTO;
import followarcane.wow_lfg_discord_bot.application.request.*;
import followarcane.wow_lfg_discord_bot.application.response.RecruitmentFilterResponse;
import followarcane.wow_lfg_discord_bot.application.response.ServerFeatureResponse;
import followarcane.wow_lfg_discord_bot.application.service.DiscordBotService;
import followarcane.wow_lfg_discord_bot.application.service.DiscordService;
import followarcane.wow_lfg_discord_bot.application.service.RecruitmentFilterService;
import followarcane.wow_lfg_discord_bot.application.service.RequestConverter;
import followarcane.wow_lfg_discord_bot.domain.model.UserSettings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@AllArgsConstructor
@RequestMapping("/v1/discord")
@Slf4j
public class DiscordController {

    private final DiscordService discordService;
    private final RequestConverter requestConverter;
    private final DiscordBotService discordBotService;
    private final RecruitmentFilterService filterService;

    @PostMapping("/addServer")
    public ResponseEntity<?> addServer(@RequestBody DiscordServerRequest discordServerRequest, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);

        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        discordService.addServer(discordServerRequest);
        return ResponseEntity.ok("Server added successfully.");
    }

    @PostMapping("/addChannel")
    public ResponseEntity<?> addChannel(@RequestBody DiscordChannelRequest discordChannelRequest, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);

        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        discordService.addChannel(discordChannelRequest);
        return ResponseEntity.ok("Channel added successfully.");
    }

    @PostMapping("/addUser")
    public ResponseEntity<?> addUser(@RequestBody UserRequest userRequest, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);

        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        discordService.addUser(userRequest);
        return ResponseEntity.ok("User added successfully.");
    }

    @PostMapping("/addUserSettings")
    public ResponseEntity<?> addUserSettings(@RequestBody UserSettingsRequest userSettingsRequest, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        log.info("UserSettingsRequest : {}", userSettingsRequest);

        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        String userId = validationResponse.getBody();
        discordService.addUserSettings(userSettingsRequest, userId);
        return ResponseEntity.ok("Settings added successfully.");
    }

    @GetMapping("{guild}/settings/{featureId}")
    public ResponseEntity<?> getSettings(@PathVariable String guild, @PathVariable String featureId, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        try {
            String userId = validationResponse.getBody();
            UserSettingsRequest userSettings = discordService.getSettingsByServerIdAndUserId(guild, userId);
            if (userSettings == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User settings not found");
            }

            return ResponseEntity.ok(userSettings);
        } catch (Exception e) {
            log.error("[SETTINGS_ERROR] Error getting settings for guild {}: {}", guild, e.getMessage());
            return ResponseEntity.internalServerError().body("Error getting settings");
        }
    }

    @GetMapping("/getGuild/{guild}")
    public ResponseEntity<?> getGuild(@PathVariable String guild, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);

        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        Map<String, Object> response = discordBotService.getGuildDetails(token, guild);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Guild not found");
    }

    @GetMapping("/getServers")
    public ResponseEntity<?> getServers(@RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
        String userId = validationResponse.getBody();
        return ResponseEntity.ok(discordService.getServersByUserDiscordId(userId));
    }

    @GetMapping("/{guild}/channels")
    public ResponseEntity<?> getChannels(@PathVariable String guild, @RequestHeader("Authorization") String token) {
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        List<TextChannel> channels = discordBotService.getGuildChannelList(guild);
        List<TextChannelDTO> channelDTOs = requestConverter.convertToDTO(channels);
        return ResponseEntity.ok(channelDTOs);
    }

    @PutMapping("/servers/{serverId}/recruitmentFilters")
    public ResponseEntity<?> updateRecruitmentFilters(
            @PathVariable String serverId,
            @RequestBody RecruitmentFilterRequest request,
            @RequestHeader("Authorization") String token) {
        
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        try {
            filterService.updateFilters(serverId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[FILTER_ERROR] Error updating filters for server {}: {}", serverId, e.getMessage());
            return ResponseEntity.internalServerError().body("Error updating filters");
        }
    }

    @GetMapping("/servers/{serverId}/recruitment-filters")
    public ResponseEntity<?> getRecruitmentFilters(
            @PathVariable String serverId,
            @RequestHeader("Authorization") String token) {
        
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        try {
            RecruitmentFilterResponse filters = filterService.getFilters(serverId);
            return ResponseEntity.ok(filters);
        } catch (Exception e) {
            log.error("[FILTER_ERROR] Error getting filters for server {}: {}", serverId, e.getMessage());
            return ResponseEntity.internalServerError().body("Error getting filters");
        }
    }

    @PostMapping("/test-filter")
    public ResponseEntity<?> testFilter(@RequestBody Map<String, String> request) {
        String serverId = request.get("serverId");
        Map<String, String> playerInfo = new HashMap<>();
        playerInfo.put("class", request.get("class"));
        playerInfo.put("role", request.get("role"));
        playerInfo.put("ilevel", request.get("ilevel"));
        playerInfo.put("progress", request.get("progress"));
        
        boolean result = filterService.shouldSendMessage(serverId, playerInfo);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @GetMapping("/servers/{serverId}/features")
    public ResponseEntity<?> getServerFeatures(
            @PathVariable String serverId,
            @RequestHeader("Authorization") String token) {
        
        ResponseEntity<String> validationResponse = discordBotService.validateAndGetUserId(token);
        if (validationResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        try {
            List<ServerFeatureResponse> features = discordService.getServerFeatures(serverId);
            return ResponseEntity.ok(features);
        } catch (Exception e) {
            log.error("[FEATURE_ERROR] Error getting features for server {}: {}", serverId, e.getMessage());
            return ResponseEntity.internalServerError().body("Error getting features");
        }
    }
}

