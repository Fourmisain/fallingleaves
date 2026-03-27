package randommcsomethin.fallingleaves.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;
import randommcsomethin.fallingleaves.init.Leaves;
import randommcsomethin.fallingleaves.mixin.LeavesBlockAccessor;
import randommcsomethin.fallingleaves.mixin.NativeImageAccessor;
import randommcsomethin.fallingleaves.mixin.ParticleSpriteManagerAccessor;
import randommcsomethin.fallingleaves.mixin.SpriteContentsAccessor;
import randommcsomethin.fallingleaves.seasons.Season;
import randommcsomethin.fallingleaves.seasons.Seasons;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;
import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.particle.ParticleImplementation.VANILLA;

public class LeafUtil {

    public static final Identifier TINTED_LEAVES_PARTICLE_ID = Identifier.withDefaultNamespace("tinted_leaves");
    public static final Identifier CHERRY_LEAVES_PARTICLE_ID = Identifier.withDefaultNamespace("cherry_leaves");
    public static final Identifier PALE_OAK_LEAVES_PARTICLE_ID = Identifier.withDefaultNamespace("pale_oak_leaves");

    private static final RandomSource renderRandom = RandomSource.createNewThreadLocalInstance();

    public static SpriteSet getSpriteProvider(Identifier spriteId) {
        return ((ParticleSpriteManagerAccessor) Minecraft.getInstance().particleEngine).getSpriteSets().get(spriteId);
    }

    public static float getModifiedSpawnChance(BlockPos pos, BlockState state, LeafSettingsEntry leafSettings) {
        if (isInsideMinimalSpawnRadius(pos))
            return 0;

        if (!CONFIG.dropFromPlayerPlacedBlocks && state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT))
            return 0;

        // Every leaf block or leaf spawner should have a settings entry, but some blocks are considered leaves when they technically aren't
        // E.g. terrestria:sakura_log can be "leaf-logged" - in that case, we simply ignore them
        if (leafSettings == null)
            return 0;

        float spawnChance = leafSettings.getSpawnChance();

        if (Seasons.currentSeason == Season.FALL) {
            spawnChance *= CONFIG.fallSpawnRateFactor;
        } else if (Seasons.currentSeason == Season.WINTER) {
            spawnChance *= CONFIG.winterSpawnRateFactor;
        }

        if (CONFIG.decaySpawnRateFactor != 1.0f) {
            if (isLeafBlock(state.getBlock(), true) && state.isRandomlyTicking()) { // decaying leaves have random ticks
                spawnChance *= CONFIG.decaySpawnRateFactor;
            }
        }

        return spawnChance;
    }

    private static boolean isInsideMinimalSpawnRadius(BlockPos pos) {
        if (CONFIG.startingSpawnRadius == 0) return false;

        return getMaximumDistance(Objects.requireNonNull(Minecraft.getInstance().player).blockPosition(), pos) < CONFIG.startingSpawnRadius;
    }

    public static void trySpawnSnowParticle(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isInsideMinimalSpawnRadius(pos)) return;

        // snow spawns independently of leaf particles (and the leaf block settings)
        double snowSpawnChance = CONFIG.getSnowflakeSpawnChance();
        if (snowSpawnChance != 0 && random.nextDouble() < snowSpawnChance) {
            spawnSnowParticles(1, false, state, level, pos, random);
        }
    }

    public static void spawnLeafParticles(int count, boolean spawnInsideBlock, BlockState state, Level level, BlockPos pos, RandomSource random, LeafSettingsEntry leafSettings) {
        if (count == 0) return;

        if (leafSettings.getImplementation() == VANILLA && state.getBlock() instanceof LeavesBlockAccessor leavesBlock) {
            for (int i = 0; i < count; i++) {
                // doesn't respect spawnInsideBlock
                leavesBlock.callSpawnFallingLeavesParticle(level, pos, random);
            }

            return;
        }

        var particleType = switch (leafSettings.getImplementation()) {
            case CHERRY  -> Leaves.FALLING_CHERRY;
            case CONIFER -> Leaves.FALLING_CONIFER_LEAF;
            case REGULAR -> Leaves.FALLING_LEAF;
            case VANILLA -> Leaves.FALLING_LEAF; // Vanilla non-LeavesBlock
        };

        BlockParticleOption params = new BlockParticleOption(particleType, state);

        spawnParticles(count, params, spawnInsideBlock, state, level, pos, random);
    }

    public static void spawnSnowParticles(int count, boolean spawnInsideBlock, BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (count == 0) return;

        boolean snowy = false;

        boolean snowyVillagers = VillagerType.byBiome(level.getBiome(pos)) == VillagerType.SNOW;
        boolean isSummer = Seasons.currentSeason == Season.SUMMER;

        // matches all snowy vanilla biomes
        if (!isSummer && snowyVillagers) {
            snowy = true;
        } else {
            // check the top for snow layers/blocks
            Block topBlock = level.getBlockState(level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos).below()).getBlock();

            boolean isSnowLayer = topBlock instanceof SnowLayerBlock; // works for seasons:seasonal_snow too
            if (isSnowLayer || topBlock == Blocks.SNOW_BLOCK || topBlock instanceof PowderSnowBlock) {
                snowy = true;
            }
        }

        // biome temperature checks for snow don't work well, Seasons globally puts them at or below 0 in winter too
        // if (level.getBiome(pos).value().getTemperature() < 0.0)
        //    snowy = true;

        if (!snowy)
            return;

        BlockParticleOption params = new BlockParticleOption(Leaves.FALLING_SNOW, state);

        spawnParticles(count, params, spawnInsideBlock, state, level, pos, random);
    }

    public static void spawnParticles(int count, BlockParticleOption params, boolean spawnInsideBlock, BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (count == 0) return;

        for (int i = 0; i < count; i++) {
            // Particle position
            double x = pos.getX() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double y;

            if (spawnInsideBlock) {
                y = pos.getY() + random.nextDouble();
            } else {
                y = pos.getY() - (state.canOcclude() ? 0.1 : 0); // move leaves outside of opaque blocks (to prevent them from appearing black)

                if (!hasRoomForLeafParticle(level, pos, x, y, z))
                    continue;
            }

            spawnParticle(params, x, y, z, random);
        }
    }

    public static void spawnParticle(BlockParticleOption params, double x, double y, double z, RandomSource random) {
        Minecraft client = Minecraft.getInstance();

        // note: doesn't respect shouldAlwaysSpawn, though we set it to true anyway
        if (CONFIG.registerParticles) {
            client.particleEngine.createParticle(params, x, y, z, 0, 0, 0);
        } else {
            Particle particle = Leaves.FACTORIES.get(params.getType()).createParticle(params, client.level, x, y, z, 0, 0, 0, random);
            client.particleEngine.add(particle);
        }
    }

    public static double[] getBlockTextureColor(BlockState state, Level level, BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        BlockStateModel model = client.getBlockRenderer().getBlockModel(state);

        renderRandom.setSeed(state.getSeed(pos));
        List<BlockModelPart> parts = model.collectParts(renderRandom);

        if (parts.size() > 1) {
            LOGGER.debug("block state {} has {} parts: {}", state, parts.size(), parts);
        }

        TextureAtlasSprite sprite = null;
        boolean shouldColor = false;

        if (!parts.isEmpty()) {
            BlockModelPart part = parts.getFirst();

            // read data from the first bottom quad if possible
            List<BakedQuad> quads = part.getQuads(Direction.DOWN);
            if (!quads.isEmpty()) {
                boolean useFirstQuad = true;

                Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                if (id.getNamespace().equals("byg")) {
                    /*
                     * some BYG leaves have their actual tinted leaf texture in an "overlay" that comes second, full list:
                     * flowering_orchard_leaves, joshua_leaves, mahogany_leaves, maple_leaves, orchard_leaves,
                     * rainbow_eucalyptus_leaves, ripe_joshua_leaves, ripe_orchard_leaves, willow_leaves
                     */
                    useFirstQuad = false;
                }

                BakedQuad quad = quads.get(useFirstQuad ? 0 : quads.size() - 1);
                sprite = quad.sprite();
                shouldColor = quad.isTinted();
            }
        }

        if (sprite == null) {
            // fall back to block breaking particle
            sprite = model.particleIcon();
            shouldColor = true;
        }

        SpriteContents spriteContents = sprite.contents();
        Identifier spriteId = spriteContents.name();
        NativeImage texture = ((SpriteContentsAccessor) spriteContents).getByMipLevel()[0]; // directly extract texture
        int blockColor = (shouldColor ? client.getBlockColors().getColor(state, level, pos, 0) : -1);

        return calculateLeafColor(spriteId, texture, blockColor);
    }

    private static double[] calculateLeafColor(Identifier spriteId, NativeImage texture, int blockColor) {
        TextureCache.Data cache = TextureCache.INST.get(spriteId);
        double[] textureColor;

        if (cache != null) {
            textureColor = cache.getColor();
        } else {
            // calculate and cache texture color
            textureColor = averageColor(texture);
            TextureCache.INST.put(spriteId, new TextureCache.Data(textureColor));
            LOGGER.debug("{}: Calculated texture color {} ", spriteId, textureColor);
        }

        if (blockColor != -1) {
            // multiply texture and block color RGB values (in range 0-1)
            textureColor[0] *= (blockColor >> 16 & 255) / 255.0;
            textureColor[1] *= (blockColor >> 8  & 255) / 255.0;
            textureColor[2] *= (blockColor       & 255) / 255.0;
        }

        return textureColor;
    }

    private static boolean hasRoomForLeafParticle(Level level, BlockPos pos, double x, double y, double z) {
        // Never spawn a particle if there's a leaf block below
        // This test is necessary because modded leaf blocks may not have collisions
        if (isLeafBlock(level.getBlockState(pos.below()).getBlock(), true)) return false;

        double y2 = y - CONFIG.minimumFreeSpaceBelow * 0.5;
        AABB collisionBox = new AABB(x - 0.1, y, z - 0.1, x + 0.1, y2, z + 0.1);

        // Only spawn the particle if there's enough room for it
        return !level.getBlockCollisions(null, collisionBox).iterator().hasNext();
    }

    public static Map<Identifier, LeafSettingsEntry> getRegisteredLeafBlocks(boolean useBlockTags) {
        return BuiltInRegistries.BLOCK
            .keySet()
            .stream()
            .filter(entry -> isLeafBlock(BuiltInRegistries.BLOCK.getValue(entry), useBlockTags))
            .collect(Collectors.toMap(
                Function.identity(),
                LeafSettingsEntry::new
            ));
    }

    /** Block tags can only be used once the integrated server has been started */
    public static boolean isLeafBlock(Block block, boolean useBlockTags) {
        return (block instanceof LeavesBlock) || (useBlockTags && block.defaultBlockState().is(BlockTags.LEAVES));
    }

    @Nullable
    public static LeafSettingsEntry getLeafSettingsEntry(BlockState blockState) {
        return CONFIG.leafSettings.get(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
    }

    public static int getMaximumDistance(Vec3i v1, Vec3i v2) {
        int dx = Math.abs(v1.getX() - v2.getX());
        int dy = Math.abs(v1.getY() - v2.getY());
        int dz = Math.abs(v1.getZ() - v2.getZ());
        return Math.max(dx, Math.max(dy, dz));
    }

    public static double[] averageColor(NativeImage image) {
        if (image.format() != NativeImage.Format.RGBA) {
            LOGGER.error("RGBA image required, was {}", image.format());
            return new double[] {1, 1, 1};
        }

        NativeImageAccessor imageAcc = (NativeImageAccessor) (Object) image;
        long pointer = imageAcc.getPixels();

        if (pointer == 0) {
            LOGGER.error("image is not allocated");
            return new double[] {1, 1, 1};
        }

        double r = 0;
        double g = 0;
        double b = 0;
        int n = 0;

        int width = image.getWidth();
        int height = image.getHeight();

        // add up all opaque color values (this variant is much faster than using image.getPixelColor(x, y))
        for (int i = 0; i < width * height; i++) {
            int c = MemoryUtil.memGetInt(pointer + 4L * i);

            // RGBA format
            int cr = (c       & 255);
            int cg = (c >> 8  & 255);
            int cb = (c >> 16 & 255);
            int ca = (c >> 24 & 255);

            if (ca != 0) {
                r += cr;
                g += cg;
                b += cb;
                n++;
            }
        }

        return new double[] {
            (r / n) / 255.0,
            (g / n) / 255.0,
            (b / n) / 255.0
        };
    }

}
