package followarcane.wow_lfg_discord_bot.application.util;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public enum WowHeroTalentEnum {
    // Death Knight
    DEATHBRINGER("Deathbringer", Arrays.asList(WowSpecEnum.BLOOD, WowSpecEnum.FROST_DK),
            Arrays.asList("deathbringer", "db")),
    RIDER("Rider", Arrays.asList(WowSpecEnum.FROST_DK, WowSpecEnum.UNHOLY),
            Arrays.asList("rider", "rider of the apocalypse", "apocalypse", "rota")),
    SANLAYN("San'layn", Arrays.asList(WowSpecEnum.UNHOLY, WowSpecEnum.BLOOD),
            Arrays.asList("san'layn", "sanlayn", "san layn")),

    // Demon Hunter
    ALDRACHI_REAVER("", Arrays.asList(WowSpecEnum.HAVOC, WowSpecEnum.VENGEANCE),
            Arrays.asList("aldrachi reaver", "aldrachi", "reaver", "ar")),
    FEL_SCARRED("", Arrays.asList(WowSpecEnum.HAVOC, WowSpecEnum.VENGEANCE),
            Arrays.asList("fel-scarred", "fel scarred", "scarred", "fs")),

    // Druid
    DRUID_OF_THE_CLAW("", Arrays.asList(WowSpecEnum.FERAL, WowSpecEnum.GUARDIAN),
            Arrays.asList("druid of the claw", "claw", "dotc")),
    ELUNES_CHOSEN("", Arrays.asList(WowSpecEnum.BALANCE, WowSpecEnum.GUARDIAN),
            Arrays.asList("elune's chosen", "elunes chosen", "elune", "ec")),
    KEEPER_OF_THE_GROVE("", Arrays.asList(WowSpecEnum.RESTORATION_DRUID, WowSpecEnum.BALANCE),
            Arrays.asList("keeper of the grove", "keeper", "grove", "kotg")),
    WILDSTALKER("", Arrays.asList(WowSpecEnum.FERAL, WowSpecEnum.RESTORATION_DRUID),
            Arrays.asList("wildstalker", "stalker", "ws")),

    // Evoker
    CHRONOWARDEN("Chronowarden", Arrays.asList(WowSpecEnum.PRESERVATION, WowSpecEnum.AUGMENTATION),
            Arrays.asList("chronowarden", "chrono", "cw")),
    FLAMESHAPER("FS", Arrays.asList(WowSpecEnum.DEVASTATION, WowSpecEnum.PRESERVATION),
            Arrays.asList("flameshaper", "flame", "fs")),
    SCALECOMMANDER("SC", Arrays.asList(WowSpecEnum.AUGMENTATION, WowSpecEnum.DEVASTATION),
            Arrays.asList("scalecommander", "scale", "sc")),

    // Hunter
    DARK_RANGER("DarkRanger", Arrays.asList(WowSpecEnum.BEAST_MASTERY, WowSpecEnum.MARKSMANSHIP),
            Arrays.asList("dark ranger", "dark_ranger", "ranger", "dr")),
    PACK_LEADER("PackLeader", Arrays.asList(WowSpecEnum.BEAST_MASTERY, WowSpecEnum.SURVIVAL),
            Arrays.asList("pack leader", "pack_leader", "pack", "pl")),
    SENTINEL("Sentinel", Arrays.asList(WowSpecEnum.MARKSMANSHIP, WowSpecEnum.SURVIVAL),
            Arrays.asList("sentinel", "sent")),

    // Mage
    FROSTFIRE("Frostfire", Arrays.asList(WowSpecEnum.FIRE, WowSpecEnum.FROST_MAGE),
            Arrays.asList("frostfire", "ff")),
    SPELLSLINGER("Spellslinger", Arrays.asList(WowSpecEnum.ARCANE, WowSpecEnum.FROST_MAGE),
            Arrays.asList("spellslinger", "slinger", "ss")),
    SUNFURY("Sunfury", Arrays.asList(WowSpecEnum.ARCANE, WowSpecEnum.FIRE),
            Arrays.asList("sunfury", "sun", "sf")),

    // Monk
    CONDUIT_OF_THE_CELESTIALS("", Arrays.asList(WowSpecEnum.MISTWEAVER, WowSpecEnum.WINDWALKER),
            Arrays.asList("conduit of the celestials", "conduit", "celestials", "cotc")),
    MASTER_OF_HARMONY("", Arrays.asList(WowSpecEnum.BREWMASTER, WowSpecEnum.MISTWEAVER),
            Arrays.asList("master of harmony", "harmony", "moh")),
    SHADO_PAN("Shadopan", Arrays.asList(WowSpecEnum.BREWMASTER, WowSpecEnum.WINDWALKER),
            Arrays.asList("shado-pan", "shado pan", "shado", "sp")),

    // Paladin
    HERALD_OF_THE_SUN("Herald", Arrays.asList(WowSpecEnum.HOLY_PALADIN, WowSpecEnum.RETRIBUTION),
            Arrays.asList("herald of the sun", "herald", "sun", "hots")),
    LIGHTSMITH("Lightsmith", Arrays.asList(WowSpecEnum.HOLY_PALADIN, WowSpecEnum.PROTECTION_PALADIN),
            Arrays.asList("lightsmith", "smith", "ls")),
    TEMPLAR("Templar_BIS", Arrays.asList(WowSpecEnum.RETRIBUTION, WowSpecEnum.PROTECTION_PALADIN),
            Arrays.asList("templar", "temp")),

    // Priest
    ARCHON("Archon", Arrays.asList(WowSpecEnum.HOLY_PRIEST, WowSpecEnum.SHADOW),
            Arrays.asList("archon", "arch")),
    ORACLE("Oracle", Arrays.asList(WowSpecEnum.DISCIPLINE, WowSpecEnum.HOLY_PRIEST),
            Arrays.asList("oracle", "ora")),
    VOIDWEAVER("Voidweaver", Arrays.asList(WowSpecEnum.DISCIPLINE, WowSpecEnum.SHADOW),
            Arrays.asList("voidweaver", "void", "vw")),

    // Rogue
    DEATHSTALKER("", Arrays.asList(WowSpecEnum.ASSASSINATION, WowSpecEnum.SUBTLETY),
            Arrays.asList("deathstalker", "death", "ds")),
    FATEBOUND("", Arrays.asList(WowSpecEnum.ASSASSINATION, WowSpecEnum.OUTLAW),
            Arrays.asList("fatebound", "fate", "fb")),
    TRICKSTER("", Arrays.asList(WowSpecEnum.OUTLAW, WowSpecEnum.SUBTLETY),
            Arrays.asList("trickster", "trick", "tr")),

    // Shaman
    FARSEER("Farseer", Arrays.asList(WowSpecEnum.ELEMENTAL, WowSpecEnum.RESTORATION_SHAMAN),
            Arrays.asList("farseer", "far", "fs")),
    STORMBRINGER("Stormbringer", Arrays.asList(WowSpecEnum.ELEMENTAL, WowSpecEnum.ENHANCEMENT),
            Arrays.asList("stormbringer", "storm", "sb")),
    TOTEMIC("", Arrays.asList(WowSpecEnum.ENHANCEMENT, WowSpecEnum.RESTORATION_SHAMAN),
            Arrays.asList("totemic", "totem", "tot")),

    // Warlock
    DIABOLIST("Diabolist", Arrays.asList(WowSpecEnum.DEMONOLOGY, WowSpecEnum.DESTRUCTION),
            Arrays.asList("diabolist", "diablo", "db")),
    HELLCALLER("Hellcaller", Arrays.asList(WowSpecEnum.AFFLICTION, WowSpecEnum.DESTRUCTION),
            Arrays.asList("hellcaller", "hell", "hc")),
    SOUL_HARVESTER("", Arrays.asList(WowSpecEnum.AFFLICTION, WowSpecEnum.DEMONOLOGY),
            Arrays.asList("soul harvester", "harvester", "soul", "sh")),

    // Warrior
    COLOSSUS("Colossus", Arrays.asList(WowSpecEnum.ARMS, WowSpecEnum.PROTECTION_WARRIOR),
            Arrays.asList("colossus", "col")),
    MOUNTAIN_THANE("Thane", Arrays.asList(WowSpecEnum.FURY, WowSpecEnum.PROTECTION_WARRIOR),
            Arrays.asList("mountain thane", "thane", "mountain", "mt")),
    SLAYER("Slayer", Arrays.asList(WowSpecEnum.ARMS, WowSpecEnum.FURY),
            Arrays.asList("slayer", "slay"));

    private final String formattedName;
    private final List<WowSpecEnum> specs;
    private final List<String> aliases;

    WowHeroTalentEnum(String formattedName, List<WowSpecEnum> specs, List<String> aliases) {
        this.formattedName = formattedName;
        this.specs = specs;
        this.aliases = aliases;
    }

    public static WowHeroTalentEnum fromString(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String lowerText = text.toLowerCase();

        for (WowHeroTalentEnum talent : WowHeroTalentEnum.values()) {
            if (talent.formattedName.equalsIgnoreCase(text) ||
                    talent.formattedName.toLowerCase().contains(lowerText) ||
                    talent.aliases.contains(lowerText)) {
                return talent;
            }
        }

        return null;
    }

    public static WowHeroTalentEnum fromString(String text, WowSpecEnum spec) {
        if (text == null || text.isEmpty() || spec == null) {
            return null;
        }

        String lowerText = text.toLowerCase();

        for (WowHeroTalentEnum talent : WowHeroTalentEnum.values()) {
            if (talent.specs.contains(spec) &&
                    (talent.formattedName.equalsIgnoreCase(text) ||
                            talent.formattedName.toLowerCase().contains(lowerText) ||
                            talent.aliases.contains(lowerText))) {
                return talent;
            }
        }

        return null;
    }

    public static List<WowHeroTalentEnum> getHeroTalentsForSpec(WowSpecEnum spec) {
        if (spec == null) {
            return List.of();
        }

        List<WowHeroTalentEnum> talents = new ArrayList<>();
        for (WowHeroTalentEnum talent : WowHeroTalentEnum.values()) {
            if (talent.specs.contains(spec)) {
                talents.add(talent);
            }
        }

        return talents;
    }

}