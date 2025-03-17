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
import java.util.List;
import java.util.Arrays;

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
            log.info("[FILTER_CHECK] Starting filter check for server: {}", serverId);
            log.info("[FILTER_CHECK] Player Info: class={}, role={}, ilevel={}, progress={}", 
                playerInfo.get("class"), 
                playerInfo.get("role"), 
                playerInfo.get("ilevel"), 
                playerInfo.get("progress"));
            log.info("[FILTER_CHECK] DB Filters: class={}, role={}, minIlevel={}, progress={}", 
                filter.getClassFilter(), 
                filter.getRoleFilter(), 
                filter.getMinIlevel(), 
                filter.getRaidProgress());

            boolean classMatch = checkClassFilter(filter, playerInfo.get("class"));
            log.info("[FILTER_CHECK] Class Match: {} (required: {}, got: {})", 
                classMatch, filter.getClassFilter(), playerInfo.get("class"));

            boolean roleMatch = checkRoleFilter(filter, playerInfo.get("role"));
            log.info("[FILTER_CHECK] Role Match: {} (required: {}, got: {})", 
                roleMatch, filter.getRoleFilter(), playerInfo.get("role"));

            boolean ilevelMatch = checkIlevelFilter(filter, playerInfo.get("ilevel"));
            log.info("[FILTER_CHECK] ILevel Match: {} (required: {}, got: {})", 
                ilevelMatch, filter.getMinIlevel(), playerInfo.get("ilevel"));

            boolean progressMatch = checkProgressFilter(filter, playerInfo.get("progress"));
            log.info("[FILTER_CHECK] Progress Match: {} (required: {}, got: {})", 
                progressMatch, filter.getRaidProgress(), playerInfo.get("progress"));

            boolean finalResult = classMatch && roleMatch && ilevelMatch && progressMatch;
            log.info("[FILTER_CHECK] Final Result: {} for server: {}", finalResult, serverId);

            return finalResult;
        } catch (Exception e) {
            log.error("[FILTER_ERROR] Error checking filters for server {}: {}", serverId, e.getMessage());
            return false;
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
        if (filter.getClassFilter().equals("ANY")) return true;
        
        // Class ismini normalize et
        String normalizedPlayerClass = playerClass.toUpperCase().replace(" ", "_");
        log.info("[CLASS_CHECK] Normalized player class: {} -> {}", playerClass, normalizedPlayerClass);
        
        // DB'deki filtreyi virgülle ayır
        String[] allowedClasses = filter.getClassFilter().split(",");
        
        // Her bir class için tam eşleşme kontrol et
        for (String allowedClass : allowedClasses) {
            if (normalizedPlayerClass.equals(allowedClass.trim())) {
                return true;
            }
        }
        
        return false;
    }

    private boolean checkRoleFilter(RecruitmentFilter filter, String playerRole) {
        return filter.getRoleFilter().equals("ANY") || 
               filter.getRoleFilter().contains(playerRole);
    }

    private boolean checkIlevelFilter(RecruitmentFilter filter, String playerIlevel) {
        if (filter.getMinIlevel() == null) return true;
        
        try {
            // Decimal kısmını at
            double ilevel = Double.parseDouble(playerIlevel);
            int playerIlevelInt = (int) ilevel;
            
            log.info("[ILEVEL_CHECK] Parsed ilevel: {} -> {}", playerIlevel, playerIlevelInt);
            
            return playerIlevelInt >= filter.getMinIlevel();
        } catch (NumberFormatException e) {
            log.error("[ILEVEL_ERROR] Invalid ilevel format: {}, allowing message", playerIlevel);
            return true;  // Parse hatası olursa mesajı göster
        }
    }

    private boolean checkProgressFilter(RecruitmentFilter filter, String playerProgress) {
        if (filter.getRaidProgress().equals("ANY")) return true;
        
        try {
            // Progress formatını normalize et
            String normalizedPlayerProgress = playerProgress.replace(" ", "");
            String normalizedFilterProgress = filter.getRaidProgress().replace(" ", "");
            
            log.info("[PROGRESS_CHECK] Normalized progress: {} -> {}", playerProgress, normalizedPlayerProgress);
            
            String[] required = normalizedFilterProgress.split("/");
            String[] player = normalizedPlayerProgress.split("/");
            
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