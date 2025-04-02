package followarcane.wow_lfg_discord_bot.application.util;

import java.util.Arrays;
import java.util.List;

public enum WowSpecEnum {
    // Death Knight
    BLOOD("Blood", WowClassEnum.DEATH_KNIGHT, Arrays.asList("blood", "tank", "blood dk")),
    FROST_DK("Frost", WowClassEnum.DEATH_KNIGHT, Arrays.asList("frost", "frost dk")),
    UNHOLY("Unholy", WowClassEnum.DEATH_KNIGHT, Arrays.asList("unholy", "unholy dk")),

    // Demon Hunter
    HAVOC("Havoc", WowClassEnum.DEMON_HUNTER, Arrays.asList("havoc", "dps", "havoc dh")),
    VENGEANCE("Vengeance", WowClassEnum.DEMON_HUNTER, Arrays.asList("vengeance", "veng", "tank dh")),

    // Druid
    BALANCE("Balance", WowClassEnum.DRUID, Arrays.asList("balance", "boomkin", "boom", "moonkin")),
    FERAL("Feral", WowClassEnum.DRUID, Arrays.asList("feral", "cat")),
    GUARDIAN("Guardian", WowClassEnum.DRUID, Arrays.asList("guardian", "bear", "tank druid")),
    RESTORATION_DRUID("Restoration", WowClassEnum.DRUID, Arrays.asList("restoration", "resto", "resto druid", "tree")),

    // Evoker
    DEVASTATION("Devastation", WowClassEnum.EVOKER, Arrays.asList("devastation", "dev", "dps evoker")),
    PRESERVATION("Preservation", WowClassEnum.EVOKER, Arrays.asList("preservation", "pres", "healer evoker")),
    AUGMENTATION("Augmentation", WowClassEnum.EVOKER, Arrays.asList("augmentation", "aug", "support evoker")),

    // Hunter
    BEAST_MASTERY("Beast_Mastery", WowClassEnum.HUNTER, Arrays.asList("beast mastery", "bm", "beast_mastery", "beastmastery")),
    MARKSMANSHIP("Marksmanship", WowClassEnum.HUNTER, Arrays.asList("marksmanship", "marks", "mm")),
    SURVIVAL("Survival", WowClassEnum.HUNTER, Arrays.asList("survival", "surv", "sv")),

    // Mage
    ARCANE("Arcane", WowClassEnum.MAGE, Arrays.asList("arcane", "arc")),
    FIRE("Fire", WowClassEnum.MAGE, Arrays.asList("fire")),
    FROST_MAGE("Frost", WowClassEnum.MAGE, Arrays.asList("frost", "frost mage")),

    // Monk
    BREWMASTER("Brewmaster", WowClassEnum.MONK, Arrays.asList("brewmaster", "brew", "tank monk")),
    MISTWEAVER("Mistweaver", WowClassEnum.MONK, Arrays.asList("mistweaver", "mist", "mw", "healer monk")),
    WINDWALKER("Windwalker", WowClassEnum.MONK, Arrays.asList("windwalker", "ww", "dps monk")),

    // Paladin
    HOLY_PALADIN("Holy", WowClassEnum.PALADIN, Arrays.asList("holy", "holy pala", "healer paladin")),
    PROTECTION_PALADIN("Protection", WowClassEnum.PALADIN, Arrays.asList("protection", "prot", "prot pala", "tank paladin")),
    RETRIBUTION("Retribution", WowClassEnum.PALADIN, Arrays.asList("retribution", "ret", "dps paladin")),

    // Priest
    DISCIPLINE("Discipline", WowClassEnum.PRIEST, Arrays.asList("discipline", "disc")),
    HOLY_PRIEST("Holy", WowClassEnum.PRIEST, Arrays.asList("holy", "holy priest")),
    SHADOW("Shadow", WowClassEnum.PRIEST, Arrays.asList("shadow", "sp", "shadow priest")),

    // Rogue
    ASSASSINATION("Assassination", WowClassEnum.ROGUE, Arrays.asList("assassination", "sin", "mut", "mutilation")),
    OUTLAW("Outlaw", WowClassEnum.ROGUE, Arrays.asList("outlaw", "out")),
    SUBTLETY("Subtlety", WowClassEnum.ROGUE, Arrays.asList("subtlety", "sub")),

    // Shaman
    ELEMENTAL("Elemental", WowClassEnum.SHAMAN, Arrays.asList("elemental", "ele")),
    ENHANCEMENT("Enhancement", WowClassEnum.SHAMAN, Arrays.asList("enhancement", "enh")),
    RESTORATION_SHAMAN("Restoration", WowClassEnum.SHAMAN, Arrays.asList("restoration", "resto", "resto shaman")),

    // Warlock
    AFFLICTION("Affliction", WowClassEnum.WARLOCK, Arrays.asList("affliction", "aff", "affli")),
    DEMONOLOGY("Demonology", WowClassEnum.WARLOCK, Arrays.asList("demonology", "demo")),
    DESTRUCTION("Destruction", WowClassEnum.WARLOCK, Arrays.asList("destruction", "destro")),

    // Warrior
    ARMS("Arms", WowClassEnum.WARRIOR, Arrays.asList("arms")),
    FURY("Fury", WowClassEnum.WARRIOR, Arrays.asList("fury")),
    PROTECTION_WARRIOR("Protection", WowClassEnum.WARRIOR, Arrays.asList("protection", "prot", "prot warr", "tank warrior"));

    private final String formattedName;
    private final WowClassEnum wowClass;
    private final List<String> aliases;

    WowSpecEnum(String formattedName, WowClassEnum wowClass, List<String> aliases) {
        this.formattedName = formattedName;
        this.wowClass = wowClass;
        this.aliases = aliases;
    }

    public static WowSpecEnum fromString(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String lowerText = text.toLowerCase();

        for (WowSpecEnum spec : WowSpecEnum.values()) {
            if (spec.formattedName.equalsIgnoreCase(text) ||
                    spec.formattedName.toLowerCase().contains(lowerText) ||
                    spec.aliases.contains(lowerText)) {
                return spec;
            }
        }

        return null;
    }

    public static WowSpecEnum fromString(String text, WowClassEnum wowClass) {
        if (text == null || text.isEmpty() || wowClass == null) {
            return null;
        }

        String lowerText = text.toLowerCase();

        for (WowSpecEnum spec : WowSpecEnum.values()) {
            if (spec.wowClass == wowClass &&
                    (spec.formattedName.equalsIgnoreCase(text) ||
                            spec.formattedName.toLowerCase().contains(lowerText) ||
                            spec.aliases.contains(lowerText))) {
                return spec;
            }
        }

        return null;
    }

    public String getFormattedName() {
        return formattedName;
    }

    public WowClassEnum getWowClass() {
        return wowClass;
    }

    public List<String> getAliases() {
        return aliases;
    }
} 