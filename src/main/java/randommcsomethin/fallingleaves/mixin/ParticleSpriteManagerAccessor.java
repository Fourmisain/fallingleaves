package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.particle.ParticleResources;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleResources.class)
public interface ParticleSpriteManagerAccessor {
	@Accessor
	Map<Identifier, ParticleResources.MutableSpriteSet> getSpriteSets();
}
