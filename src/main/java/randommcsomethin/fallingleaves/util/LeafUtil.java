package randommcsomethin.fallingleaves.util;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerType;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;
import randommcsomethin.fallingleaves.init.Leaves;
import randommcsomethin.fallingleaves.mixin.LeavesBlockAccessor;
import randommcsomethin.fallingleaves.mixin.NativeImageAccessor;
import randommcsomethin.fallingleaves.mixin.ParticleManagerAccessor;
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

    public static final Identifier TINTED_LEAVES_PARTICLE_ID = Identifier.ofVanilla("tinted_leaves");
    public static final Identifier CHERRY_LEAVES_PARTICLE_ID = Identifier.ofVanilla("cherry_leaves");
    public static final Identifier PALE_OAK_LEAVES_PARTICLE_ID = Identifier.ofVanilla("pale_oak_leaves");

    private static final Random renderRandom = Random.createLocal();

    public static SpriteProvider getSpriteProvider(Identifier spriteId) {
        return ((ParticleManagerAccessor) MinecraftClient.getInstance().particleManager).getSpriteAwareFactories().get(spriteId);
    }

    public static float getModifiedSpawnChance(BlockPos pos, BlockState state, LeafSettingsEntry leafSettings) {
        if (isInsideMinimalSpawnRadius(pos))
            return 0;

        if (!CONFIG.dropFromPlayerPlacedBlocks && state.contains(LeavesBlock.PERSISTENT) && state.get(LeavesBlock.PERSISTENT))
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
            if (isLeafBlock(state.getBlock(), true) && state.hasRandomTicks()) { // decaying leaves have random ticks
                spawnChance *= CONFIG.decaySpawnRateFactor;
            }
        }

        return spawnChance;
    }

    private static boolean isInsideMinimalSpawnRadius(BlockPos pos) {
        if (CONFIG.startingSpawnRadius == 0) return false;

        return getMaximumDistance(Objects.requireNonNull(MinecraftClient.getInstance().player).getBlockPos(), pos) < CONFIG.startingSpawnRadius;
    }

    public static void trySpawnSnowParticle(BlockState state, World world, BlockPos pos, Random random) {
        if (isInsideMinimalSpawnRadius(pos)) return;

        // snow spawns independently of leaf particles (and the leaf block settings)
        double snowSpawnChance = CONFIG.getSnowflakeSpawnChance();
        if (snowSpawnChance != 0 && random.nextDouble() < snowSpawnChance) {
            spawnSnowParticles(1, false, state, world, pos, random);
        }
    }

    public static void spawnLeafParticles(int count, boolean spawnInsideBlock, BlockState state, World world, BlockPos pos, Random random, LeafSettingsEntry leafSettings) {
        if (count == 0) return;

        if (leafSettings.getImplementation() == VANILLA && state.getBlock() instanceof LeavesBlockAccessor leavesBlock) {
            for (int i = 0; i < count; i++) {
                // doesn't respect spawnInsideBlock
                leavesBlock.callSpawnLeafParticle(world, pos, random);
            }

            return;
        }

        var particleType = switch (leafSettings.getImplementation()) {
            case CHERRY  -> Leaves.FALLING_CHERRY;
            case CONIFER -> Leaves.FALLING_CONIFER_LEAF;
	        case REGULAR -> Leaves.FALLING_LEAF;
            case VANILLA -> Leaves.FALLING_LEAF; // Vanilla non-LeavesBlock
        };

        BlockStateParticleEffect params = new BlockStateParticleEffect(particleType, state);

        spawnParticles(count, params, spawnInsideBlock, state, world, pos, random);
    }

    public static void spawnSnowParticles(int count, boolean spawnInsideBlock, BlockState state, World world, BlockPos pos, Random random) {
        if (count == 0) return;

        boolean snowy = false;

        boolean snowyVillagers = VillagerType.forBiome(world.getBiome(pos)) == VillagerType.SNOW;
        boolean isSummer = Seasons.currentSeason == Season.SUMMER;

        // matches all snowy vanilla biomes
        if (!isSummer && snowyVillagers) {
            snowy = true;
        } else {
            // check the top for snow layers/blocks
            Block topBlock = world.getBlockState(world.getTopPosition(Heightmap.Type.WORLD_SURFACE, pos).down()).getBlock();

            boolean isSnowLayer = topBlock instanceof SnowBlock; // works for seasons:seasonal_snow too
            if (isSnowLayer || topBlock == Blocks.SNOW_BLOCK || topBlock instanceof PowderSnowBlock) {
                snowy = true;
            }
        }

        // biome temperature checks for snow don't work well, Seasons globally puts them at or below 0 in winter too
        // if (world.getBiome(pos).value().getTemperature() < 0.0)
        //    snowy = true;

        if (!snowy)
            return;

        BlockStateParticleEffect params = new BlockStateParticleEffect(Leaves.FALLING_SNOW, state);

        spawnParticles(count, params, spawnInsideBlock, state, world, pos, random);
    }

    public static void spawnParticles(int count, BlockStateParticleEffect params, boolean spawnInsideBlock, BlockState state, World world, BlockPos pos, Random random) {
        if (count == 0) return;

        for (int i = 0; i < count; i++) {
            // Particle position
            double x = pos.getX() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double y;

            if (spawnInsideBlock) {
                y = pos.getY() + random.nextDouble();
            } else {
                y = pos.getY() - (state.isOpaque() ? 0.1 : 0); // move leaves outside of opaque blocks (to prevent them from appearing black)

                if (!hasRoomForLeafParticle(world, pos, x, y, z))
                    continue;
            }

            spawnParticle(params, x, y, z);
        }
    }

    public static void spawnParticle(BlockStateParticleEffect params, double x, double y, double z) {
        MinecraftClient client = MinecraftClient.getInstance();

        // note: doesn't respect shouldAlwaysSpawn, though we set it to true anyway
        if (CONFIG.registerParticles) {
            client.particleManager.addParticle(params, x, y, z, 0, 0, 0);
        } else {
            Particle particle = Leaves.FACTORIES.get(params.getType()).createParticle(params, client.world, x, y, z, 0, 0, 0);
            client.particleManager.addParticle(particle);
        }
    }

    public static double[] getBlockTextureColor(BlockState state, World world, BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockStateModel model = client.getBlockRenderManager().getModel(state);

        renderRandom.setSeed(state.getRenderingSeed(pos));
        List<BlockModelPart> parts = model.getParts(renderRandom);

        if (parts.size() > 1) {
            LOGGER.debug("block state {} has {} parts: {}", state, parts.size(), parts);
        }

        Sprite sprite = null;
        boolean shouldColor = false;

        if (!parts.isEmpty()) {
            BlockModelPart part = parts.getFirst();

            // read data from the first bottom quad if possible
            List<BakedQuad> quads = part.getQuads(Direction.DOWN);
            if (!quads.isEmpty()) {
                boolean useFirstQuad = true;

                Identifier id = Registries.BLOCK.getId(state.getBlock());
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
                shouldColor = quad.hasTint();
            }
        }

        if (sprite == null) {
            // fall back to block breaking particle
            sprite = model.particleSprite();
            shouldColor = true;
        }

        SpriteContents spriteContents = sprite.getContents();
        Identifier spriteId = spriteContents.getId();
        NativeImage texture = ((SpriteContentsAccessor) spriteContents).getMipmapLevelsImages()[0]; // directly extract texture
        int blockColor = (shouldColor ? client.getBlockColors().getColor(state, world, pos, 0) : -1);

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

    private static boolean hasRoomForLeafParticle(World world, BlockPos pos, double x, double y, double z) {
        // Never spawn a particle if there's a leaf block below
        // This test is necessary because modded leaf blocks may not have collisions
        if (isLeafBlock(world.getBlockState(pos.down()).getBlock(), true)) return false;

        double y2 = y - CONFIG.minimumFreeSpaceBelow * 0.5;
        Box collisionBox = new Box(x - 0.1, y, z - 0.1, x + 0.1, y2, z + 0.1);

        // Only spawn the particle if there's enough room for it
        return !world.getBlockCollisions(null, collisionBox).iterator().hasNext();
    }

    public static Map<Identifier, LeafSettingsEntry> getRegisteredLeafBlocks(boolean useBlockTags) {
        return Registries.BLOCK
            .getIds()
            .stream()
            .filter(entry -> isLeafBlock(Registries.BLOCK.get(entry), useBlockTags))
            .collect(Collectors.toMap(
                Function.identity(),
                LeafSettingsEntry::new
            ));
    }

    /** Block tags can only be used once the integrated server has been started */
    public static boolean isLeafBlock(Block block, boolean useBlockTags) {
        return (block instanceof LeavesBlock) || (useBlockTags && block.getDefaultState().isIn(BlockTags.LEAVES));
    }

    @Nullable
    public static LeafSettingsEntry getLeafSettingsEntry(BlockState blockState) {
        return CONFIG.leafSettings.get(Registries.BLOCK.getId(blockState.getBlock()));
    }

    public static int getMaximumDistance(Vec3i v1, Vec3i v2) {
        int dx = Math.abs(v1.getX() - v2.getX());
        int dy = Math.abs(v1.getY() - v2.getY());
        int dz = Math.abs(v1.getZ() - v2.getZ());
        return Math.max(dx, Math.max(dy, dz));
    }

    public static double[] averageColor(NativeImage image) {
        if (image.getFormat() != NativeImage.Format.RGBA) {
            LOGGER.error("RGBA image required, was {}", image.getFormat());
            return new double[] {1, 1, 1};
        }

        NativeImageAccessor imageAcc = (NativeImageAccessor) (Object) image;
        long pointer = imageAcc.getPointer();

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
