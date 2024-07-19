package followarcane.wow_lfg_discord_bot.application.util;

import org.springframework.stereotype.Component;

@Component
public class ClassColorCodeHelper {

    public String getClassColorCode(String className) {
        if (className == null) {
            return "#FFFFF1";
        }
        return switch (className) {
            case "Death Knight" -> "#C41E3A";
            case "Demon Hunter" -> "#A330C9";
            case "Druid" -> "#FF7C0A";
            case "Hunter" -> "#AAD372";
            case "Mage" -> "#3FC7EB";
            case "Monk" -> "#00FF98";
            case "Paladin" -> "#F48CBA";
            case "Priest" -> "#FFFFFF";
            case "Rogue" -> "#FFF468";
            case "Shaman" -> "#0070DD";
            case "Warlock" -> "#8788EE";
            case "Warrior" -> "#C69B6D";
            case "Evoker" -> "#33937F";
            default -> "#FFFFF1";
        };
    }
}
