package randommcsomethin.fallingleaves.config;

import net.minecraft.util.Identifier;

public class LeafSettingsEntryV1 {
    public double spawnRateFactor; // won't be migrated
    public boolean isConiferBlock;
    public boolean spawnBreakingLeaves;

    public LeafSettingsEntryV1(Identifier identifier) {
        this.spawnRateFactor = ConfigDefaults.spawnRateFactor(identifier);
        this.isConiferBlock = ConfigDefaults.isConifer(identifier);
        this.spawnBreakingLeaves = ConfigDefaults.spawnBreakingLeaves(identifier);
    }

    @Override
    public String toString() {
        return String.format("LeafSettingsEntry{spawnRateFactor=%s, isConiferBlock=%s, spawnBreakingLeaves=%s}",
            spawnRateFactor,
            isConiferBlock,
            spawnBreakingLeaves);
    }
}
