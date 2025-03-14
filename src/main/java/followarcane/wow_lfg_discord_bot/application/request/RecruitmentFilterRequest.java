package followarcane.wow_lfg_discord_bot.application.request;

import lombok.Data;

@Data
public class RecruitmentFilterRequest {
    private String classFilter = "ANY";  // "ANY" veya "PRIEST,WARRIOR,etc"
    private String roleFilter = "ANY";   // "ANY" veya "TANK,HEALER,DPS"
    private Integer minIlevel;           // null = no filter
    private String raidProgress = "ANY"; // "ANY" veya "8/8M" gibi
} 