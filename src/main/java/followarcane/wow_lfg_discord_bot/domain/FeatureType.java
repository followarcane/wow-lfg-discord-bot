package followarcane.wow_lfg_discord_bot.domain;

import lombok.Getter;

@Getter
public enum FeatureType {
    LFG("Looking for Group"),
    RAID_SCHEDULER("Raid Scheduler"),
    ATTENDANCE_TRACKER("Attendance Tracker"),
    LOOT_HISTORY("Loot History");

    private final String displayName;

    FeatureType(String displayName) {
        this.displayName = displayName;
    }

}