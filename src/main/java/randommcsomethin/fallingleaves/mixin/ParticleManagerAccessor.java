package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleManager.class)
public interface ParticleManagerAccessor {
	@Accessor
	Map<Identifier, ParticleManager.SimpleSpriteProvider> getSpriteAwareFactories();
}
