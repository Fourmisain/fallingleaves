package randommcsomethin.fallingleaves.mixin;

import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LeavesBlock.class)
public interface LeavesBlockAccessor {
	@Accessor
	float getLeafParticleChance();

	@Invoker
	void callSpawnLeafParticle(World world, BlockPos pos, Random random);
}
