package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.seasons.Seasons;
import randommcsomethin.fallingleaves.util.Wind;

import static randommcsomethin.fallingleaves.init.Config.CONFIG;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

	@Shadow
	protected ClientWorld world;

	@Inject(method = "tick", at = @At("HEAD"))
	public void tick(CallbackInfo ci) {
		if (!CONFIG.enabled)
			return;

		Seasons.tick(world);
		Wind.tick(world);
	}

}
