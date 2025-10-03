package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.particle.ParticleSpriteManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ParticleSpriteManager.SimpleSpriteProvider.class)
public interface SimpleSpriteProviderInvoker {
    @Invoker("<init>")
    static ParticleSpriteManager.SimpleSpriteProvider init() {
        throw new AssertionError();
    }
}
