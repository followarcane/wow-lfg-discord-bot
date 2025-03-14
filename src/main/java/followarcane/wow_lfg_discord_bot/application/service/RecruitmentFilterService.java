package followarcane.wow_lfg_discord_bot.application.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import followarcane.wow_lfg_discord_bot.domain.model.RecruitmentFilter;
import followarcane.wow_lfg_discord_bot.domain.repository.RecruitmentFilterRepository;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.repository.DiscordServerRepository;
import followarcane.wow_lfg_discord_bot.application.request.RecruitmentFilterRequest;
import followarcane.wow_lfg_discord_bot.application.response.RecruitmentFilterResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecruitmentFilterService {
    
    private final RecruitmentFilterRepository filterRepository;
    private final DiscordServerRepository discordServerRepository;

    public boolean shouldSendMessage(String serverId, Map<String, String> playerInfo) {
        RecruitmentFilter filter = filterRepository.findByServer_ServerId(serverId)
            .orElse(createDefaultFilter());

        try {
            return checkClassFilter(filter, playerInfo.get("class")) &&
                   checkRoleFilter(filter, playerInfo.get("role")) &&
                   checkIlevelFilter(filter, playerInfo.get("ilevel")) &&
                   checkProgressFilter(filter, playerInfo.get("progress"));
        } catch (Exception e) {
            log.error("[FILTER_ERROR] Error checking filters for server {}: {}", serverId, e.getMessage());
            return true;
        }
    }

    private RecruitmentFilter createDefaultFilter() {
        RecruitmentFilter filter = new RecruitmentFilter();
        filter.setClassFilter("ANY");
        filter.setRoleFilter("ANY");
        filter.setRaidProgress("ANY");
        return filter;
    }

    private boolean checkClassFilter(RecruitmentFilter filter, String playerClass) {
        return filter.getClassFilter().equals("ANY") || 
               filter.getClassFilter().contains(playerClass);
    }

    private boolean checkRoleFilter(RecruitmentFilter filter, String playerRole) {
        return filter.getRoleFilter().equals("ANY") || 
               filter.getRoleFilter().contains(playerRole);
    }

    private boolean checkIlevelFilter(RecruitmentFilter filter, String playerIlevel) {
        if (filter.getMinIlevel() == null) return true;
        return Integer.parseInt(playerIlevel) >= filter.getMinIlevel();
    }

    private boolean checkProgressFilter(RecruitmentFilter filter, String playerProgress) {
        if (filter.getRaidProgress().equals("ANY")) return true;
        
        try {
            String[] required = filter.getRaidProgress().split("/");
            String[] player = playerProgress.split("/");
            
            int reqBoss = Integer.parseInt(required[0]);
            int playerBoss = Integer.parseInt(player[0]);
            
            String reqDiff = required[1].substring(required[1].length() - 1);
            String playerDiff = player[1].substring(player[1].length() - 1);
            
            // Difficulty comparison (N < H < M)
            if (!reqDiff.equals(playerDiff)) {
                return getDifficultyValue(playerDiff) >= getDifficultyValue(reqDiff);
            }
            
            return playerBoss >= reqBoss;
        } catch (Exception e) {
            log.error("[PROGRESS_ERROR] Error parsing progress values: {} vs {}", 
                filter.getRaidProgress(), playerProgress);
            return true;
        }
    }

    private int getDifficultyValue(String difficulty) {
        return switch (difficulty.toUpperCase()) {
            case "N" -> 1;
            case "H" -> 2;
            case "M" -> 3;
            default -> 0;
        };
    }

    public void updateFilters(String serverId, RecruitmentFilterRequest request) {
        RecruitmentFilter filter = filterRepository.findByServer_ServerId(serverId)
            .orElse(new RecruitmentFilter());

        if (filter.getId() == null) {
            DiscordServer server = discordServerRepository.findServerByServerId(serverId);
            if (server == null) {
                throw new RuntimeException("Server not found");
            }
            filter.setServer(server);
        }

        filter.setClassFilter(request.getClassFilter());
        filter.setRoleFilter(request.getRoleFilter());
        filter.setMinIlevel(request.getMinIlevel());
        filter.setRaidProgress(request.getRaidProgress());

        filterRepository.save(filter);
    }

    public RecruitmentFilterResponse getFilters(String serverId) {
        RecruitmentFilter filter = filterRepository.findByServer_ServerId(serverId)
            .orElse(createDefaultFilter());

        RecruitmentFilterResponse response = new RecruitmentFilterResponse();
        response.setClassFilter(filter.getClassFilter());
        response.setRoleFilter(filter.getRoleFilter());
        response.setMinIlevel(filter.getMinIlevel());
        response.setRaidProgress(filter.getRaidProgress());

        return response;
    }
} 