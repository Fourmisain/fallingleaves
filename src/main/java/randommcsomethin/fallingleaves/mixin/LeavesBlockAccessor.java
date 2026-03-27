package randommcsomethin.fallingleaves.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LeavesBlock.class)
public interface LeavesBlockAccessor {
	@Accessor
	float getLeafParticleChance();

	@Invoker
	void callSpawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random);
}
