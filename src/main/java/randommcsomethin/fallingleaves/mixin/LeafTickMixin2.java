package randommcsomethin.fallingleaves.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingParticlesLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;
import randommcsomethin.fallingleaves.particle.ParticleImplementation;

import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.util.LeafUtil.*;

@Environment(EnvType.CLIENT)
@Mixin(FallingParticlesLeavesBlock.class)
public abstract class LeafTickMixin2 {

    @Unique
    private BlockState fallingleaves$blockState;

    @Inject(method = "animateTick", at = @At("HEAD"))
    private void captureBlockState(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        fallingleaves$blockState = state;
    }

    @ModifyExpressionValue(
        method = "makeFallingLeavesParticles",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/FallingParticlesLeavesBlock;leafParticleChance:F",
            opcode = Opcodes.GETFIELD
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
        method = "makeFallingLeavesParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FallingParticlesLeavesBlock;isFaceFull(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/core/Direction;)Z"
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
        method = "makeFallingLeavesParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FallingParticlesLeavesBlock;spawnFallingLeavesParticle(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
        )
    )
    private void replaceLeafParticles(FallingParticlesLeavesBlock instance, Level level, BlockPos pos, RandomSource random, Operation<Void> original,
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
}
