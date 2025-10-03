package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.particle.ParticleSpriteManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleSpriteManager.class)
public interface ParticleSpriteManagerAccessor {
	@Accessor
	Map<Identifier, ParticleSpriteManager.SimpleSpriteProvider> getSpriteAwareParticleFactories();
}
