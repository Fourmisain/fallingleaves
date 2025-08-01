package randommcsomethin.fallingleaves.init;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleManager.SimpleSpriteProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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
    public static final ParticleType<BlockStateParticleEffect> FALLING_LEAF;
    public static final ParticleType<BlockStateParticleEffect> FALLING_CONIFER_LEAF;
    public static final ParticleType<BlockStateParticleEffect> FALLING_SNOW;

    public static final Map<ParticleType<BlockStateParticleEffect>, Identifier> LEAVES;
    public static final Map<ParticleType<BlockStateParticleEffect>, ParticleFactory<BlockStateParticleEffect>> FACTORIES = new IdentityHashMap<>();

    public static final Map<Identifier, SimpleSpriteProvider> CUSTOM_LEAVES = new HashMap<>();

    private static boolean preLoadedRegisteredLeafBlocks = false;

    static {
        FALLING_LEAF = FabricParticleTypes.complex(true, BlockStateParticleEffect::createCodec, BlockStateParticleEffect::createPacketCodec);
        FALLING_CONIFER_LEAF = FabricParticleTypes.complex(true, BlockStateParticleEffect::createCodec, BlockStateParticleEffect::createPacketCodec);
        FALLING_SNOW = FabricParticleTypes.complex(true, BlockStateParticleEffect::createCodec, BlockStateParticleEffect::createPacketCodec);

        LEAVES = Map.of(
            FALLING_LEAF, id("falling_leaf"),
            FALLING_CONIFER_LEAF, id("falling_leaf_conifer"),
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
            Registry.register(Registries.PARTICLE_TYPE, entry.getValue(), entry.getKey());
            ParticleFactoryRegistry.getInstance().register(entry.getKey(), FallingLeafParticle.BlockStateFactory::new);
        }
    }

    private static void registerReloadListener() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void reload(ResourceManager resourceManager) {
                // This is called before the block tags are usable, so we'll get an incomplete list of leaf blocks
                // Still better than having an empty settings menu on first launch
                if (!preLoadedRegisteredLeafBlocks) {
                    for (var registered : LeafUtil.getRegisteredLeafBlocks(false).entrySet())
                        CONFIG.leafSettings.computeIfAbsent(registered.getKey(), k -> registered.getValue());

                    preLoadedRegisteredLeafBlocks = true;
                }

                TextureCache.INST.clear();
            }

            @Override
            public Identifier getFabricId() {
                return id("resource_reload_listener");
            }
        });
    }

    /** Spawn between 0 and 3 leaves on hitting a leaf block */
    private static void registerAttackBlockLeaves() {
        AttackBlockCallback.EVENT.register((PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction) -> {
            if (!CONFIG.enabled || !CONFIG.leavesOnBlockHit || !world.isClient)
                return ActionResult.PASS;

            BlockState state = world.getBlockState(pos);
            LeafSettingsEntry leafSettings = getLeafSettingsEntry(state);

            if (leafSettings != null) {
                if (leafSettings.spawnBreakingLeaves) {
                    // binomial distribution - extremes (0 or 3 leaves) are less likely
                    int count = 0;
                    for (int i = 0; i < 3; i++) {
                        if (world.random.nextBoolean()) {
                            count++;
                        }
                    }

                    LeafUtil.spawnLeafParticles(count, false, state, world, pos, world.random, leafSettings);
                }

                // spawn a bit of snow too
                if (CONFIG.getSnowflakeSpawnChance() != 0) {
                    int snowCount = 0;
                    for (int i = 0; i < 6; i++) {
                        if (world.random.nextBoolean()) {
                            snowCount++;
                        }
                    }

                    LeafUtil.spawnSnowParticles(snowCount, false, state, world, pos, world.random, leafSettings);
                }
            }

            return ActionResult.PASS;
        });
    }
}
