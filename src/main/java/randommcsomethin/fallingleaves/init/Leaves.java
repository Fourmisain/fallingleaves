package randommcsomethin.fallingleaves.init;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources.MutableSpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;
import randommcsomethin.fallingleaves.particle.FallingLeafParticle;
import randommcsomethin.fallingleaves.util.LeafUtil;
import randommcsomethin.fallingleaves.util.TextureCache;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;
import static randommcsomethin.fallingleaves.FallingLeavesClient.id;
import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.util.LeafUtil.getLeafSettingsEntry;

public class Leaves {
    public static final ParticleType<BlockParticleOption> FALLING_LEAF;
    public static final ParticleType<BlockParticleOption> FALLING_CONIFER_LEAF;
    public static final ParticleType<BlockParticleOption> FALLING_CHERRY;
    public static final ParticleType<BlockParticleOption> FALLING_SNOW;

    public static final Map<ParticleType<BlockParticleOption>, Identifier> LEAVES;
    public static final Map<ParticleType<BlockParticleOption>, ParticleProvider<BlockParticleOption>> FACTORIES = new IdentityHashMap<>();

    public static final Map<Identifier, MutableSpriteSet> CUSTOM_LEAVES = new HashMap<>();

    private static boolean preLoadedRegisteredLeafBlocks = false;

    static {
        FALLING_LEAF = FabricParticleTypes.complex(true, BlockParticleOption::codec, BlockParticleOption::streamCodec);
        FALLING_CONIFER_LEAF = FabricParticleTypes.complex(true, BlockParticleOption::codec, BlockParticleOption::streamCodec);
        FALLING_CHERRY = FabricParticleTypes.complex(true, BlockParticleOption::codec, BlockParticleOption::streamCodec);
        FALLING_SNOW = FabricParticleTypes.complex(true, BlockParticleOption::codec, BlockParticleOption::streamCodec);

        LEAVES = Map.of(
            FALLING_LEAF, id("falling_leaf"),
            FALLING_CONIFER_LEAF, id("falling_leaf_conifer"),
            FALLING_CHERRY, id("falling_cherry"),
            FALLING_SNOW, id("falling_snow")
        );
    }

    public static void init() {
        if (CONFIG.registerParticles) {
            LOGGER.info("Registering leaf particles.");
            registerLeafParticles();
        }

        registerReloadListener();
        registerAttackBlockLeaves();
    }

    private static void registerLeafParticles() {
        for (var entry : LEAVES.entrySet()) {
            Registry.register(BuiltInRegistries.PARTICLE_TYPE, entry.getValue(), entry.getKey());
            ParticleFactoryRegistry.getInstance().register(entry.getKey(), FallingLeafParticle.BlockStateFactory::new);
        }
    }

    private static void registerReloadListener() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(id("resource_reload_listener"), new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                // This is called before the block tags are usable, so we'll get an incomplete list of leaf blocks
                // Still better than having an empty settings menu on first launch
                if (!preLoadedRegisteredLeafBlocks) {
                    for (var registered : LeafUtil.getRegisteredLeafBlocks(false).entrySet())
                        CONFIG.leafSettings.computeIfAbsent(registered.getKey(), k -> registered.getValue());

                    preLoadedRegisteredLeafBlocks = true;
                }

                TextureCache.INST.clear();
            }
        });
    }

    /** Spawn between 0 and 3 leaves on hitting a leaf block */
    private static void registerAttackBlockLeaves() {
        AttackBlockCallback.EVENT.register((Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction) -> {
            if (!CONFIG.enabled || !CONFIG.leavesOnBlockHit || !level.isClientSide())
                return InteractionResult.PASS;

            BlockState state = level.getBlockState(pos);
            LeafSettingsEntry leafSettings = getLeafSettingsEntry(state);

            if (leafSettings != null) {
                if (leafSettings.spawnBreakingLeaves) {
                    // binomial distribution - extremes (0 or 3 leaves) are less likely
                    int count = 0;
                    for (int i = 0; i < 3; i++) {
                        if (level.random.nextBoolean()) {
                            count++;
                        }
                    }

                    LeafUtil.spawnLeafParticles(count, false, state, level, pos, level.random, leafSettings);
                }

                // spawn a bit of snow too
                if (CONFIG.getSnowflakeSpawnChance() != 0) {
                    int snowCount = 0;
                    for (int i = 0; i < 6; i++) {
                        if (level.random.nextBoolean()) {
                            snowCount++;
                        }
                    }

                    LeafUtil.spawnSnowParticles(snowCount, false, state, level, pos, level.random);
                }
            }

            return InteractionResult.PASS;
        });
    }
}
