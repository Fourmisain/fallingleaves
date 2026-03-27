package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStateModelWrapper.class)
public interface BlockStateModelWrapperAccessor {
	@Accessor
	BlockStateModel getModel();
}
