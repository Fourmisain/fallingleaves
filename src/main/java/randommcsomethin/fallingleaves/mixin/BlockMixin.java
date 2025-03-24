package randommcsomethin.fallingleaves.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;

import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.util.LeafUtil.*;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "randomDisplayTick", at = @At("HEAD"))
    private void randomLeafBlockTick(BlockState state, World world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!CONFIG.enabled)
            return;

        Identifier id = Registries.BLOCK.getId(state.getBlock());

        if (!CONFIG.isLeafSpawner(id))
            return;

        // return if block properties don't match spawner properties
        for (var entry : CONFIG.getLeafSpawnerProperties(id).entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();

            if (!state.contains(property))
                continue;

            if (!state.get(property).equals(value)) {
                return;
            }
        }

        trySpawnSnowParticle(state, world, pos, random);

        LeafSettingsEntry leafSettings = getLeafSettingsEntry(state);
        if (leafSettings != null) {
            float spawnChance = getModifiedSpawnChance(pos, state, leafSettings);

            if (spawnChance != 0 && random.nextFloat() < spawnChance) {
                spawnLeafParticles(1, false, state, world, pos, random, leafSettings);
            }
        }
    }

}
