package followarcane.wow_lfg_discord_bot.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TextChannelDTO {
    private String id;
    private String name;
}
