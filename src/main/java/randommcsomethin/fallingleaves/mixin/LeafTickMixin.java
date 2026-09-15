package randommcsomethin.fallingleaves.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingParticlesLeavesBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;

import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.util.LeafUtil.*;

@Environment(EnvType.CLIENT)
@Mixin(LeavesBlock.class)
public abstract class LeafTickMixin {

    @SuppressWarnings("ConstantValue")
    @Inject(method = "animateTick", at = @At("HEAD"))
    private void addLeafParticles(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!CONFIG.enabled)
            return;

        // add leaves only to non-FallingParticlesLeavesBlocks (modify those instead, see LeafTickMixin2)
        if (!((Object) this instanceof FallingParticlesLeavesBlock)) {
            LeafSettingsEntry leafSettings = getLeafSettingsEntry(state);
            if (leafSettings != null) {
                float spawnChance = getModifiedSpawnChance(pos, state, leafSettings);

                if (spawnChance != 0 && random.nextFloat() < spawnChance) {
                    spawnLeafParticles(1, false, state, level, pos, random, leafSettings);
                }
            }
        }

        trySpawnSnowParticle(state, level, pos, random);
    }

    // TODO this only runs server-side and will thus only work in singleplayer
    @Inject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public void addDecayingLeafParticles(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
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
                    if (clientLevel.getRandom().nextBoolean()) {
                        count++;
                    }
                }

                spawnLeafParticles(count, true, state, clientLevel, pos, clientLevel.getRandom(), leafSettings);
            }

            int snowCount = 0;
            for (int i = 0; i < 2*CONFIG.maxDecayLeaves; i++) {
                if (clientLevel.getRandom().nextBoolean()) {
                    snowCount++;
                }
            }

            spawnSnowParticles(snowCount, true, state, clientLevel, pos, clientLevel.getRandom());
        });
    }

}
