package randommcsomethin.fallingleaves.config;

import net.minecraft.util.Identifier;
import randommcsomethin.fallingleaves.particle.ParticleImplementation;

import static randommcsomethin.fallingleaves.init.Config.CONFIG;

public class LeafSettingsEntry {

    public ParticleImplementation particleImplementation;
    public float spawnRateFactor;
    public boolean isConiferBlock;
    public boolean spawnBreakingLeaves;

    public LeafSettingsEntry(Identifier identifier) {
        this.particleImplementation = ConfigDefaults.getImplementation(identifier);
        this.spawnRateFactor = ConfigDefaults.spawnRateFactor(identifier);
        this.isConiferBlock = ConfigDefaults.isConifer(identifier);
        this.spawnBreakingLeaves = ConfigDefaults.spawnBreakingLeaves(identifier);
    }

    public ParticleImplementation getImplementation() {
        if (CONFIG.alwaysUseVanillaParticles) return ParticleImplementation.VANILLA;
        return particleImplementation;
    }

    public float getSpawnChance() {
        float spawnChance = (isConiferBlock ? CONFIG.getBaseConiferLeafSpawnChance() : CONFIG.getBaseLeafSpawnChance());
        return spawnRateFactor * spawnChance;
    }

    public boolean isDefault(Identifier identifier) {
        return particleImplementation == ConfigDefaults.getImplementation(identifier)
            && spawnRateFactor == ConfigDefaults.spawnRateFactor(identifier)
            && isConiferBlock == ConfigDefaults.isConifer(identifier)
            && spawnBreakingLeaves == ConfigDefaults.spawnBreakingLeaves(identifier);
    }

    @Override
    public String toString() {
        return String.format("LeafSettingsEntry{particleImplementation=%s, spawnRateFactor=%s, isConiferBlock=%s, spawnBreakingLeaves=%s}",
            particleImplementation,
            spawnRateFactor,
            isConiferBlock,
            spawnBreakingLeaves);
    }
}
