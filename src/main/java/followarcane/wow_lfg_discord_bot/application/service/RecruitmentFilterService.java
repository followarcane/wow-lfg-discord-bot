package followarcane.wow_lfg_discord_bot.application.service;

import followarcane.wow_lfg_discord_bot.application.request.RecruitmentFilterRequest;
import followarcane.wow_lfg_discord_bot.application.response.RecruitmentFilterResponse;
import followarcane.wow_lfg_discord_bot.domain.model.DiscordServer;
import followarcane.wow_lfg_discord_bot.domain.model.RecruitmentFilter;
import followarcane.wow_lfg_discord_bot.domain.repository.DiscordServerRepository;
import followarcane.wow_lfg_discord_bot.domain.repository.RecruitmentFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

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
            // Sadece başlangıç ve gelen bilgileri logla
            log.debug("[FILTER_CHECK] Starting filter check for server: {} with player info: {}", 
                serverId, playerInfo);

            boolean classMatch = checkClassFilter(filter, playerInfo.get("class"));
            boolean roleMatch = checkRoleFilter(filter, playerInfo.get("role"));
            boolean ilevelMatch = checkIlevelFilter(filter, playerInfo.get("ilevel"));
            boolean progressMatch = checkProgressFilter(filter, playerInfo.get("progress"));

            boolean finalResult = classMatch && roleMatch && ilevelMatch && progressMatch;
            
            // Eğer filtre başarısız olduysa nedenini logla
            if (!finalResult) {
                log.info("[FILTER_FAILED] Server: {}, Failed filters: {}", 
                    serverId,
                    getFailedFilters(classMatch, roleMatch, ilevelMatch, progressMatch, filter, playerInfo));
            }

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

    private String getFailedFilters(boolean classMatch, boolean roleMatch, boolean ilevelMatch, 
        boolean progressMatch, RecruitmentFilter filter, Map<String, String> playerInfo) {
        
        StringBuilder failed = new StringBuilder();
        if (!classMatch) {
            failed.append(String.format("Class(required:%s,got:%s) ", 
                filter.getClassFilter(), playerInfo.get("class")));
        }
        if (!roleMatch) {
            failed.append(String.format("Role(required:%s,got:%s) ", 
                filter.getRoleFilter(), playerInfo.get("role")));
        }
        if (!ilevelMatch) {
            failed.append(String.format("iLevel(required:%d,got:%s) ", 
                filter.getMinIlevel(), playerInfo.get("ilevel")));
        }
        if (!progressMatch) {
            failed.append(String.format("Progress(required:%s,got:%s)", 
                filter.getRaidProgress(), playerInfo.get("progress")));
        }
        return failed.toString();
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