package randommcsomethin.fallingleaves.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;

import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.util.LeafUtil.*;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "animateTick", at = @At("HEAD"))
    private void randomLeafBlockTick(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!CONFIG.enabled)
            return;

        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());

        if (!CONFIG.isLeafSpawner(id))
            return;

        // return if block properties don't match spawner properties
        for (var entry : CONFIG.getLeafSpawnerProperties(id).entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();

            if (!state.hasProperty(property))
                continue;

            if (!state.getValue(property).equals(value)) {
                return;
            }
        }

        trySpawnSnowParticle(state, level, pos, random);

        LeafSettingsEntry leafSettings = getLeafSettingsEntry(state);
        if (leafSettings != null) {
            float spawnChance = getModifiedSpawnChance(pos, state, leafSettings);

            if (spawnChance != 0 && random.nextFloat() < spawnChance) {
                spawnLeafParticles(1, false, state, level, pos, random, leafSettings);
            }
        }
    }

}
