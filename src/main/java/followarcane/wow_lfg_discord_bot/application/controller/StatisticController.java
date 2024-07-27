package followarcane.wow_lfg_discord_bot.application.controller;


import followarcane.wow_lfg_discord_bot.application.response.StatisticsResponse;
import followarcane.wow_lfg_discord_bot.application.service.DiscordBotService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/v2")
public class StatisticController {

    private final DiscordBotService discordBotService;

    @GetMapping("/statistics")
    public ResponseEntity<?> getAzeriteStatistics(HttpServletRequest request) {
        String ip = request.getHeader("Referer");
        if (ip.contains("https://azerite.app") || ip.contains("http://localhost:3000")) {
            StatisticsResponse statisticsResponse= discordBotService.getStatistics();
            return ResponseEntity.ok(statisticsResponse);
        }
        return ResponseEntity.internalServerError().body("Invalid request");
    }
}
