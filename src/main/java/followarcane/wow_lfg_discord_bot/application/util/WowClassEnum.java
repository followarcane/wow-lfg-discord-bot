package followarcane.wow_lfg_discord_bot.application.util;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum WowClassEnum {
    DEATH_KNIGHT("Death_Knight", Arrays.asList("dk", "death knight", "death_knight", "deathknight")),
    DEMON_HUNTER("Demon_Hunter", Arrays.asList("dh", "demon hunter", "demon_hunter", "demonhunter")),
    DRUID("Druid", Arrays.asList("druid", "dru")),
    EVOKER("Evoker", Arrays.asList("evoker", "evo")),
    HUNTER("Hunter", Arrays.asList("hunter", "hunt")),
    MAGE("Mage", List.of("mage")),
    MONK("Monk", List.of("monk")),
    PALADIN("Paladin", Arrays.asList("paladin", "pala", "pally")),
    PRIEST("Priest", Arrays.asList("priest", "pr")),
    ROGUE("Rogue", Arrays.asList("rogue", "rog")),
    SHAMAN("Shaman", Arrays.asList("shaman", "sham")),
    WARLOCK("Warlock", Arrays.asList("warlock", "lock")),
    WARRIOR("Warrior", Arrays.asList("warrior", "warr"));

    private final String formattedName;
    private final List<String> aliases;

    WowClassEnum(String formattedName, List<String> aliases) {
        this.formattedName = formattedName;
        this.aliases = aliases;
    }

    public static WowClassEnum fromString(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String lowerText = text.toLowerCase();

        for (WowClassEnum wowClass : WowClassEnum.values()) {
            if (wowClass.formattedName.equalsIgnoreCase(text) ||
                    wowClass.formattedName.toLowerCase().contains(lowerText) ||
                    wowClass.aliases.contains(lowerText)) {
                return wowClass;
            }
        }

        return null;
    }

}