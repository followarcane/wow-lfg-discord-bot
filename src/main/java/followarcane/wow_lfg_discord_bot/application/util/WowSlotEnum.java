package followarcane.wow_lfg_discord_bot.application.util;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum WowSlotEnum {
    HEAD("Head", Arrays.asList("head", "helmet", "helm")),
    NECK("Neck", Arrays.asList("neck", "necklace", "amulet")),
    SHOULDERS("Shoulders", Arrays.asList("shoulders", "shoulder", "shoulder pads")),
    BACK("Back", Arrays.asList("back", "cloak", "cape")),
    CHEST("Chest", Arrays.asList("chest", "robe", "tunic")),
    WRISTS("Wrists", Arrays.asList("wrists", "bracers", "wrist")),
    HANDS("Hands", Arrays.asList("hands", "gloves", "gauntlets")),
    WAIST("Waist", Arrays.asList("waist", "belt")),
    LEGS("Legs", Arrays.asList("legs", "pants", "leggings")),
    FEET("Feet", Arrays.asList("feet", "boots", "shoes")),
    FINGER("Finger", Arrays.asList("finger", "ring", "rings")),
    FINGER1("Finger1", Arrays.asList("finger1", "ring1")),
    FINGER2("Finger2", Arrays.asList("finger2", "ring2")),
    TRINKET("Trinket", Arrays.asList("trinket", "trinkets")),
    TRINKET1("Trinket1", List.of("trinket1")),
    TRINKET2("Trinket2", List.of("trinket2")),
    MAIN_HAND("Main Hand", Arrays.asList("main hand", "main_hand", "mainhand", "weapon", "weapon1", "mh", "main-hand")),
    OFF_HAND("Off Hand", Arrays.asList("off hand", "off_hand", "offhand", "weapon2", "oh", "off-hand"));

    private final String formattedName;
    private final List<String> aliases;

    WowSlotEnum(String formattedName, List<String> aliases) {
        this.formattedName = formattedName;
        this.aliases = aliases;
    }

    public static WowSlotEnum fromString(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String lowerText = text.toLowerCase();

        for (WowSlotEnum slot : WowSlotEnum.values()) {
            if (slot.formattedName.equalsIgnoreCase(text) ||
                    slot.formattedName.toLowerCase().contains(lowerText) ||
                    slot.aliases.contains(lowerText)) {
                return slot;
            }
        }

        return null;
    }

}