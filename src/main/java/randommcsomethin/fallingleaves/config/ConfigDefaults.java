package randommcsomethin.fallingleaves.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TintedParticleLeavesBlock;
import net.minecraft.block.UntintedParticleLeavesBlock;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import randommcsomethin.fallingleaves.FallingLeavesClient;
import randommcsomethin.fallingleaves.mixin.LeavesBlockAccessor;
import randommcsomethin.fallingleaves.particle.ParticleImplementation;

// TODO: this needs to be readjusted once mods are out for 1.21.5
public class ConfigDefaults {

    public static float DEFAULT_SPAWN_CHANCE = 0.01f;

    public static ParticleImplementation getImplementation(Identifier blockId) {
        if (ConfigDefaults.useCustomCherryImplementation(blockId)) {
            return ParticleImplementation.CHERRY;
        } else if (ConfigDefaults.isConifer(blockId)) {
            return ParticleImplementation.CONIFER;
        } else if (ConfigDefaults.useVanillaParticles(blockId)) {
            return ParticleImplementation.VANILLA;
        } else {
            return ParticleImplementation.REGULAR;
        }
    }

    /** Whether LeavesBlock.spawnLeafParticle() will be used */
    public static boolean useVanillaParticles(Identifier blockId) {
        Block b = Registries.BLOCK.get(blockId);

        // no LeavesBlock.spawnLeafParticle() to call
        if (!(b instanceof LeavesBlock))
            return false;

        // LeavesBlock.spawnLeafParticle() is abstract, so if it's neither Tinted nor Untinted, it has a custom implementation we wanna call
        if (!(b instanceof TintedParticleLeavesBlock) && !(b instanceof UntintedParticleLeavesBlock)) {
            FallingLeavesClient.LOGGER.debug("{} is neither Tinted nor Untinted", b);
            return true;
        }

        // use vanilla untinted particles - for now
        if (b instanceof UntintedParticleLeavesBlock) {
            FallingLeavesClient.LOGGER.debug("{} is Untinted", b);
            return true;
        }

        if (b.getClass() == TintedParticleLeavesBlock.class) {
            // replace vanilla tinted particles
            return false;
        } else if (b instanceof TintedParticleLeavesBlock) {
            // when a subclass has a custom implementation, use it by default
            try {
                String methodName = FabricLoader.getInstance().isDevelopmentEnvironment() ? "spawnLeafParticle" : "method_67234";
                b.getClass().getDeclaredMethod(methodName, World.class, BlockPos.class, Random.class);

                FallingLeavesClient.LOGGER.debug("{} implements spawnLeafParticle", b);
                return true;
            } catch (NoSuchMethodException e) {
                FallingLeavesClient.LOGGER.debug("{} does not implement spawnLeafParticle", b);
                return false;
            }
        }

        FallingLeavesClient.LOGGER.warn("{} has unhandled useVanillaParticles default", b);
        return false;
    }

    public static boolean useCustomCherryImplementation(Identifier blockId) {
        return false;
    }

    public static boolean isConifer(Identifier blockId) {
        switch (blockId.toString()) {
            case "bewitchment:cypress_leaves":
            case "bewitchment:dragons_blood_leaves":
            case "bewitchment:juniper_leaves":
            case "biomemakeover:swamp_cypress_leaves":
            case "byg:araucaria_leaves":
            case "byg:blue_spruce_leaves":
            case "byg:cypress_leaves":
            case "byg:fir_leaves":
            case "byg:orange_spruce_leaves":
            case "byg:pine_leaves":
            case "byg:red_spruce_leaves":
            case "byg:redwood_leaves":
            case "byg:yellow_spruce_leaves":
            case "minecraft:spruce_leaves":
            case "terrestria:cypress_leaves":
            case "terrestria:hemlock_leaves":
            case "terrestria:redwood_leaves":
            case "traverse:fir_leaves":
            case "woods_and_mires:pine_leaves":
                return true;

            default:
                return false;
        }
    }

    public static float spawnRateFactor(Identifier blockId) {
        switch (blockId.toString()) {
            // Shrubs and large leaved trees
            case "byg:palm_leaves":
            case "minecraft:jungle_leaves":
            case "promenade:palm_leaves":
            case "terrestria:japanese_maple_shrub_leaves":
            case "terrestria:jungle_palm_leaves":
            case "terrestria:yucca_palm_leaves":
                return 0;
/*
            // Autumn Leaves
            case "promenade:autumn_birch_leaves":
            case "promenade:autumn_oak_leaves":
            case "traverse:brown_autumnal_leaves":
            case "traverse:orange_autumnal_leaves":
            case "traverse:red_autumnal_leaves":
            case "traverse:yellow_autumnal_leaves":
                return 1.8;

            // For fun and flavor
            case "byg:pink_cherry_leaves":
            case "byg:skyris_leaves":
            case "byg:white_cherry_leaves":
            case "promenade:pink_cherry_leaves":
            case "promenade:white_cherry_leaves":
            case "terrestria:sakura_leaves":
                return 1.4;
*/
        }

        Block block = Registries.BLOCK.get(blockId);
        if (!(block instanceof LeavesBlockAccessor leavesBlock))
            return 1;

        float chance = leavesBlock.getLeafParticleChance();
        float factor = chance / DEFAULT_SPAWN_CHANCE;

        if (factor != 1.0f)
            FallingLeavesClient.LOGGER.debug("block {} has leaf particle chance {}, factor = {}", blockId, chance, factor);

        return factor;
    }

    // on block hit or when decaying
    public static boolean spawnBreakingLeaves(Identifier blockId) {
        return true;
    }

}
