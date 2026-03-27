package randommcsomethin.fallingleaves.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.util.Wind;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "setLevel", at = @At("HEAD"))
    public void initWind(CallbackInfo ci) {
        Wind.init();
    }

}
