package followarcane.wow_lfg_discord_bot.application.response;

import lombok.Data;

@Data
public class RecruitmentFilterResponse {
    private String classFilter;
    private String roleFilter;
    private Integer minIlevel;
    private String raidProgress;
} 