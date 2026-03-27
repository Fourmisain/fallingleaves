package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.particle.ParticleResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ParticleResources.MutableSpriteSet.class)
public interface SimpleSpriteProviderInvoker {
    @Invoker("<init>")
    static ParticleResources.MutableSpriteSet init() {
        throw new AssertionError();
    }
}
