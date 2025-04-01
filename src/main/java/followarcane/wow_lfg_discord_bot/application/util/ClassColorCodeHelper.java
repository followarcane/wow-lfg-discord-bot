package followarcane.wow_lfg_discord_bot.application.util;

import org.springframework.stereotype.Component;

@Component
public class ClassColorCodeHelper {

    public String getClassColorCode(String className) {
        if (className == null) {
            return "#FFFFFF";
        }
        String lowerClassName = className.toLowerCase();
        switch (lowerClassName) {
            case "death knight":
            case "death_knight":
            case "deathknight":
                return "#C41E3A";
            case "demon hunter":
            case "demon_hunter":
            case "demonhunter":
                return "#A330C9";
            case "druid":
                return "#FF7C0A";
            case "evoker":
                return "#33937F";
            case "hunter":
                return "#AAD372";
            case "mage":
                return "#3FC7EB";
            case "monk":
                return "#00FF98";
            case "paladin":
                return "#F48CBA";
            case "priest":
                return "#FFFFFF";
            case "rogue":
                return "#FFF468";
            case "shaman":
                return "#0070DD";
            case "warlock":
                return "#8788EE";
            case "warrior":
                return "#C69B6D";
            default:
                return "#FFFFFF";
        }
    }
}
