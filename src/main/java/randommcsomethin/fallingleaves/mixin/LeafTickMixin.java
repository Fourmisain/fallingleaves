package randommcsomethin.fallingleaves.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    @Inject(method = "animateTick", at = @At("HEAD"))
    private void captureBlockState(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        fallingleaves$blockState = state;
    }

    @ModifyExpressionValue(
        method = "makeFallingLeavesParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/LeavesBlock;leafParticleChance:F"
        )
    )
    private float adjustSpawnChance(float original, @Local(argsOnly = true, ordinal = 0) Level level, @Local(argsOnly = true, ordinal = 0) BlockPos pos,
            @Share("leafSettings") LocalRef<LeafSettingsEntry> leafSettingsRef) {
        if (!CONFIG.enabled)
            return original;

        LeafSettingsEntry leafSettings = getLeafSettingsEntry(fallingleaves$blockState);
        leafSettingsRef.set(leafSettings);

        return getModifiedSpawnChance(pos, fallingleaves$blockState, leafSettings);
    }

    @WrapOperation(
        method = "makeFallingLeavesParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/LeavesBlock;isFaceFull(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/core/Direction;)Z"
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
        method = "makeFallingLeavesParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/LeavesBlock;spawnFallingLeavesParticle(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
        )
    )
    private void replaceLeafParticles(LeavesBlock instance, Level level, BlockPos pos, RandomSource random, Operation<Void> original,
            @Share("leafSettings") LocalRef<LeafSettingsEntry> leafSettingsRef) {
        LeafSettingsEntry leafSettings = leafSettingsRef.get();

        if (!CONFIG.enabled || (leafSettings != null && leafSettings.getImplementation() == ParticleImplementation.VANILLA)) {
            original.call(instance, level, pos, random);
            return;
        }

        if (leafSettings != null) {
            spawnLeafParticles(1, false, fallingleaves$blockState, level, pos, random, leafSettings);
        }
    }

    @Inject(method = "animateTick", at = @At("HEAD"))
    private void letItSnowLetIsSnowLetItSnow(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!CONFIG.enabled)
            return;

        trySpawnSnowParticle(state, level, pos, random);
    }

    // TODO this only runs server-side and will thus only work in singleplayer
    @Inject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!CONFIG.enabled || CONFIG.maxDecayLeaves == 0)
            return;

        Minecraft.getInstance().execute(() -> {
            ClientLevel clientLevel = Minecraft.getInstance().level;
            if (clientLevel == null)
                return;

            LeafSettingsEntry leafSettings = getLeafSettingsEntry(state);
            if (leafSettings != null && leafSettings.spawnBreakingLeaves) {
                // binomial distribution - extremes are less likely
                int count = 0;
                for (int i = 0; i < CONFIG.maxDecayLeaves; i++) {
                    if (clientLevel.random.nextBoolean()) {
                        count++;
                    }
                }

                LeafUtil.spawnLeafParticles(count, true, state, clientLevel, pos, clientLevel.random, leafSettings);
            }

            int snowCount = 0;
            for (int i = 0; i < 2*CONFIG.maxDecayLeaves; i++) {
                if (clientLevel.random.nextBoolean()) {
                    snowCount++;
                }
            }

            LeafUtil.spawnSnowParticles(snowCount, true, state, clientLevel, pos, clientLevel.random);
        });
    }

}
