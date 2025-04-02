package followarcane.wow_lfg_discord_bot.application.util;

import org.springframework.stereotype.Component;

import java.awt.*;

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

    public Color getClassColor(String className) {
        if (className == null || className.isEmpty()) {
            return Color.GRAY; // Varsayılan renk
        }

        // Sınıf adını normalize et
        String normalizedClassName = className.toLowerCase().replace("_", "").replace(" ", "");

        switch (normalizedClassName) {
            case "deathknight":
                return new Color(196, 30, 58); // Kırmızı
            case "demonhunter":
                return new Color(163, 48, 201); // Mor
            case "druid":
                return new Color(255, 124, 10); // Turuncu
            case "evoker":
                return new Color(51, 147, 127); // Yeşil-Mavi
            case "hunter":
                return new Color(170, 211, 114); // Açık Yeşil
            case "mage":
                return new Color(63, 199, 235); // Açık Mavi
            case "monk":
                return new Color(0, 255, 152); // Yeşil
            case "paladin":
                return new Color(244, 140, 186); // Pembe
            case "priest":
                return new Color(255, 255, 255); // Beyaz
            case "rogue":
                return new Color(255, 244, 104); // Sarı
            case "shaman":
                return new Color(0, 112, 221); // Mavi
            case "warlock":
                return new Color(135, 136, 238); // Mor-Mavi
            case "warrior":
                return new Color(198, 155, 109); // Kahverengi
            default:
                return Color.GRAY; // Bilinmeyen sınıflar için gri
        }
    }
}
