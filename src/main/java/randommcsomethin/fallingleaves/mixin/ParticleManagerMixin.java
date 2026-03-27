package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.seasons.Seasons;
import randommcsomethin.fallingleaves.util.Wind;

import static randommcsomethin.fallingleaves.init.Config.CONFIG;

@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin {

	@Shadow
	protected ClientLevel level;

	@Inject(method = "tick", at = @At("HEAD"))
	public void tick(CallbackInfo ci) {
		if (!CONFIG.enabled)
			return;

		Seasons.tick(level);
		Wind.tick(level);
	}

}
