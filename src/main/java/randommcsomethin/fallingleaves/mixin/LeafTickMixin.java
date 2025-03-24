package randommcsomethin.fallingleaves.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;
import randommcsomethin.fallingleaves.particle.ParticleImplementation;
import randommcsomethin.fallingleaves.util.LeafUtil;

import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.util.LeafUtil.*;

@Environment(EnvType.CLIENT)
@Mixin(LeavesBlock.class)
public abstract class LeafTickMixin {

    @Unique
    private BlockState fallingleaves$blockState;

    @Inject(method = "randomDisplayTick", at = @At("HEAD"))
    private void captureBlockState(BlockState state, World world, BlockPos pos, Random random, CallbackInfo ci) {
        fallingleaves$blockState = state;
    }

    @ModifyExpressionValue(
        method = "spawnLeafParticle(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/block/LeavesBlock;leafParticleChance:F"
        )
    )
    private float adjustSpawnChance(float original, @Local(argsOnly = true, ordinal = 0) World world, @Local(argsOnly = true, ordinal = 0) BlockPos pos,
            @Share("leafSettings") LocalRef<LeafSettingsEntry> leafSettingsRef) {
        if (!CONFIG.enabled)
            return original;

        LeafSettingsEntry leafSettings = getLeafSettingsEntry(fallingleaves$blockState);
        leafSettingsRef.set(leafSettings);

        return getModifiedSpawnChance(pos, fallingleaves$blockState, leafSettings);
    }

    @WrapOperation(
        method = "spawnLeafParticle(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/LeavesBlock;isFaceFullSquare(Lnet/minecraft/util/shape/VoxelShape;Lnet/minecraft/util/math/Direction;)Z"
        )
    )
    private boolean skipCheck(VoxelShape voxelShape, Direction direction, Operation<Boolean> original,
            @Share("leafSettings") LocalRef<LeafSettingsEntry> leafSettingsRef) {
        LeafSettingsEntry leafSettings = leafSettingsRef.get();

        if (!CONFIG.enabled || (leafSettings != null && leafSettings.getImplementation() == ParticleImplementation.VANILLA))
            return original.call(voxelShape, direction);

        // we use our own collision check -> hasRoomForLeafParticle()
        return false;
    }

    @WrapOperation(
        method = "spawnLeafParticle(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/LeavesBlock;spawnLeafParticle(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;)V"
        )
    )
    private void replaceLeafParticles(LeavesBlock instance, World world, BlockPos pos, Random random, Operation<Void> original,
            @Share("leafSettings") LocalRef<LeafSettingsEntry> leafSettingsRef) {
        LeafSettingsEntry leafSettings = leafSettingsRef.get();

        if (!CONFIG.enabled || (leafSettings != null && leafSettings.getImplementation() == ParticleImplementation.VANILLA)) {
            original.call(instance, world, pos, random);
            return;
        }

        if (leafSettings != null) {
            spawnLeafParticles(1, false, fallingleaves$blockState, world, pos, random, leafSettings);
        }
    }

    @Inject(method = "randomDisplayTick", at = @At("HEAD"))
    private void letItSnowLetIsSnowLetItSnow(BlockState state, World world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!CONFIG.enabled)
            return;

        trySpawnSnowParticle(state, world, pos, random);
    }

    // TODO this only runs server-side and will thus only work in singleplayer
    @Inject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!CONFIG.enabled || CONFIG.maxDecayLeaves == 0)
            return;

        MinecraftClient.getInstance().execute(() -> {
            ClientWorld clientWorld = MinecraftClient.getInstance().world;
            if (clientWorld == null)
                return;

            LeafSettingsEntry leafSettings = getLeafSettingsEntry(state);
            if (leafSettings != null && leafSettings.spawnBreakingLeaves) {
                // binomial distribution - extremes are less likely
                int count = 0;
                for (int i = 0; i < CONFIG.maxDecayLeaves; i++) {
                    if (clientWorld.random.nextBoolean()) {
                        count++;
                    }
                }

                LeafUtil.spawnLeafParticles(count, true, state, clientWorld, pos, clientWorld.random, leafSettings);
            }

            int snowCount = 0;
            for (int i = 0; i < 2*CONFIG.maxDecayLeaves; i++) {
                if (clientWorld.random.nextBoolean()) {
                    snowCount++;
                }
            }

            LeafUtil.spawnSnowParticles(snowCount, true, state, clientWorld, pos, clientWorld.random);
        });
    }

}
