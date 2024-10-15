package followarcane.wow_lfg_discord_bot.application.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WarcraftLogsDataResponse {
    private String zoneName;
    private String metric;
    private String difficulty;
    private Double bestPerformanceAverage;
}