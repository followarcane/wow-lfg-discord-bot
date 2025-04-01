package followarcane.wow_lfg_discord_bot.application.model;

import lombok.Getter;

@Getter
public enum RaidbotsProfileInfo {
    // Death Knight
    DEATH_KNIGHT_BLOOD_DEATHBRINGER("Death_Knight", "Blood", "Deathbringer", "player2", "/html/body/div[7]/div/div[12]/div/table"),
    DEATH_KNIGHT_BLOOD_SANLAYN("Death_Knight", "Blood", "San'layn", "player1", "/html/body/div[8]/div/div[12]/div/table"),
    DEATH_KNIGHT_FROST_DEATHBRINGER("Death_Knight", "Frost", "Deathbringer", "player4", "/html/body/div[9]/div/div[12]/div/table"),
    DEATH_KNIGHT_FROST_RIDER("Death_Knight", "Frost", "Rider", "player3", "/html/body/div[10]/div/div[12]/div/table"),
    DEATH_KNIGHT_UNHOLY_RIDER("Death_Knight", "Unholy", "Rider", "player6", "/html/body/div[11]/div/div[12]/div/table"),
    DEATH_KNIGHT_UNHOLY_SANLAYN("Death_Knight", "Unholy", "San'layn", "player5", "/html/body/div[12]/div/div[12]/div/table"),

    // Demon Hunter
    DEMON_HUNTER_HAVOC("Demon_Hunter", "Havoc", "", "player7", "/html/body/div[13]/div/div[11]/div/table"),

    // Druid
    DRUID_BALANCE("Druid", "Balance", "", "player8", "/html/body/div[14]/div/div[11]/div/table"),
    DRUID_FERAL("Druid", "Feral", "", "player9", "/html/body/div[15]/div/div[11]/div/table"),

    // Evoker
    EVOKER_DEVASTATION_FS("Evoker", "Devastation", "Flameshaper", "player10", "/html/body/div[16]/div/div[10]/div/table"),
    EVOKER_DEVASTATION_SC("Evoker", "Devastation", "Scalecommander", "player11", "/html/body/div[17]/div/div[10]/div/table"),

    // Hunter
    HUNTER_BEAST_MASTERY_PACKLEADER("Hunter", "Beast_Mastery", "Pack_Leader", "player12", "/html/body/div[18]/div/div[11]/div/table"),
    HUNTER_MARKSMANSHIP_DARKRANGER("Hunter", "Marksmanship", "Dark_Ranger", "player13", "/html/body/div[19]/div/div[11]/div/table"),
    HUNTER_SURVIVAL_PACKLEADER("Hunter", "Survival", "Pack_Leader", "player14", "/html/body/div[20]/div/div[11]/div/table"),

    // Mage
    MAGE_ARCANE_SPELLSLINGER("Mage", "Arcane", "Spellslinger", "player15", "/html/body/div[21]/div/div[11]/div/table"),
    MAGE_ARCANE_SUNFURY("Mage", "Arcane", "Sunfury", "player16", "/html/body/div[22]/div/div[11]/div/table"),
    MAGE_FIRE_FROSTFIRE("Mage", "Fire", "Frostfire", "player17", "/html/body/div[23]/div/div[11]/div/table"),
    MAGE_FIRE_SUNFURY("Mage", "Fire", "Sunfury", "player18", "/html/body/div[24]/div/div[11]/div/table"),
    MAGE_FROST_FROSTFIRE("Mage", "Frost", "Frostfire", "player19", "/html/body/div[25]/div/div[11]/div/table"),
    MAGE_FROST_SPELLSLINGER("Mage", "Frost", "Spellslinger", "player20", "/html/body/div[26]/div/div[11]/div/table"),

    // Monk
    MONK_BREWMASTER("Monk", "Brewmaster", "", "player21", "/html/body/div[27]/div/div[13]/div/table"),
    MONK_WINDWALKER("Monk", "Windwalker", "", "player22", "/html/body/div[28]/div/div[13]/div/table"),
    MONK_WINDWALKER_SHADOPAN("Monk", "Windwalker", "Shadopan", "player23", "/html/body/div[29]/div/div[13]/div/table"),

    // Paladin
    PALADIN_PROTECTION_LIGHTSMITH("Paladin", "Protection", "Lightsmith", "player24", "/html/body/div[30]/div/div[11]/div/table"),
    PALADIN_RETRIBUTION("Paladin", "Retribution", "", "player25", "/html/body/div[31]/div/div[11]/div/table"),
    PALADIN_RETRIBUTION_HERALD("Paladin", "Retribution", "Herald", "player27", "/html/body/div[32]/div/div[11]/div/table"),
    PALADIN_RETRIBUTION_TEMPLAR_BIS("Paladin", "Retribution", "Templar", "player26", "/html/body/div[33]/div/div[11]/div/table"),

    // Priest
    PRIEST_SHADOW_ARCHON("Priest", "Shadow", "Archon", "player29", "/html/body/div[34]/div/div[11]/div/table"),
    PRIEST_SHADOW_VOIDWEAVER("Priest", "Shadow", "Voidweaver", "player28", "/html/body/div[35]/div/div[11]/div/table"),

    // Rogue
    ROGUE_ASSASSINATION("Rogue", "Assassination", "", "player30", "/html/body/div[36]/div/div[10]/div/table"),
    ROGUE_OUTLAW("Rogue", "Outlaw", "", "player31", "/html/body/div[37]/div/div[10]/div/table"),
    ROGUE_SUBTLETY("Rogue", "Subtlety", "", "player32", "/html/body/div[38]/div/div[10]/div/table"),

    // Shaman
    SHAMAN_ELEMENTAL_FARSEER("Shaman", "Elemental", "Farseer", "player33", "/html/body/div[39]/div/div[15]/div/table"),
    SHAMAN_ENHANCEMENT("Shaman", "Enhancement", "", "player34", "/html/body/div[40]/div/div[15]/div/table"),
    SHAMAN_ENHANCEMENT_STORMBRINGER("Shaman", "Enhancement", "Stormbringer", "player35", "/html/body/div[41]/div/div[15]/div/table"),

    // Warlock
    WARLOCK_AFFLICTION_HELLCALLER("Warlock", "Affliction", "Hellcaller", "player36", "/html/body/div[42]/div/div[10]/div/table"),
    WARLOCK_DEMONOLOGY_DIABOLIST("Warlock", "Demonology", "Diabolist", "player37", "/html/body/div[43]/div/div[10]/div/table"),
    WARLOCK_DESTRUCTION_DIABOLIST("Warlock", "Destruction", "Diabolist", "player38", "/html/body/div[44]/div/div[10]/div/table"),

    // Warrior
    WARRIOR_ARMS("Warrior", "Arms", "", "player39", "/html/body/div[45]/div/div[11]/div/table"),
    WARRIOR_FURY("Warrior", "Fury", "", "player40", "/html/body/div[46]/div/div[11]/div/table"),
    WARRIOR_PROTECTION_COLOSSUS("Warrior", "Protection", "Colossus", "player41", "/html/body/div[47]/div/div[11]/div/table"),
    WARRIOR_PROTECTION_THANE("Warrior", "Protection", "Thane", "player42", "/html/body/div[48]/div/div[11]/div/table");

    private final String className;
    private final String specName;
    private final String heroTalent;
    private final String playerId;
    private final String gearTableXPath;

    RaidbotsProfileInfo(String className, String specName, String heroTalent, String playerId, String gearTableXPath) {
        this.className = className;
        this.specName = specName;
        this.heroTalent = heroTalent;
        this.playerId = playerId;
        this.gearTableXPath = gearTableXPath;
    }

    // Verilen sınıf, spec ve hero talent için uygun profil bilgisini bul
    public static RaidbotsProfileInfo findProfileInfo(String className, String specName, String heroTalent) {
        for (RaidbotsProfileInfo profile : values()) {
            if (profile.className.equalsIgnoreCase(className) &&
                    profile.specName.equalsIgnoreCase(specName) &&
                    (heroTalent == null || heroTalent.isEmpty() || profile.heroTalent.equalsIgnoreCase(heroTalent))) {
                return profile;
            }
        }
        return null;
    }

    // Player ID'ye göre profil bilgisini bul
    public static RaidbotsProfileInfo findByPlayerId(String playerId) {
        for (RaidbotsProfileInfo profile : values()) {
            if (profile.playerId.equals(playerId)) {
                return profile;
            }
        }
        return null;
    }
} 