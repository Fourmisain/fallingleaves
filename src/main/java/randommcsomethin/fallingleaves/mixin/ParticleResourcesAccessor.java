package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.particle.ParticleResources.MutableSpriteSet;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleResources.class)
public interface ParticleResourcesAccessor {
	@Accessor
	Map<Identifier, MutableSpriteSet> getSpriteSets();
}
