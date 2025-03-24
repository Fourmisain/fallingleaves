package randommcsomethin.fallingleaves.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleManager.SimpleSpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import randommcsomethin.fallingleaves.init.Leaves;
import randommcsomethin.fallingleaves.particle.FallingLeafParticle;
import randommcsomethin.fallingleaves.seasons.Seasons;
import randommcsomethin.fallingleaves.util.Wind;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static randommcsomethin.fallingleaves.FallingLeavesClient.MOD_ID;
import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.init.Leaves.FACTORIES;

@Environment(EnvType.CLIENT)
@Mixin(value = ParticleManager.class, priority = 1010) // after Fabric API
public abstract class ParticleManagerMixin {

    @Shadow
    protected ClientWorld world;

    @Shadow @Final
    private Map<Identifier, SimpleSpriteProvider> spriteAwareFactories;

    @Shadow @Final
    private Int2ObjectMap<ParticleFactory<?>> factories;

    @SuppressWarnings("unchecked")
    @Inject(method = "registerDefaultFactories", at = @At("RETURN"))
    public void registerLeafFactories(CallbackInfo ci) {
        for (var entry : Leaves.LEAVES.entrySet()) {
            var type = entry.getKey();
            var id = entry.getValue();

            var particleFactory = (ParticleFactory<BlockStateParticleEffect>) factories.get(Registries.PARTICLE_TYPE.getRawId(type));

            if (particleFactory == null) {
                var spriteProvider = SimpleSpriteProviderInvoker.init();
                spriteAwareFactories.put(id, spriteProvider);
                particleFactory = new FallingLeafParticle.BlockStateFactory(spriteProvider);
            }

            FACTORIES.put(type, particleFactory);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if (!CONFIG.enabled)
            return;

        Seasons.tick(world);
        Wind.tick(world);
    }

    @ModifyExpressionValue(method = "loadTextureList", at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"))
    public boolean allowCustomSprites(boolean original, @Local(argsOnly = true) Identifier id) {
        // runs on worker thread
        if (id.getNamespace().equals(MOD_ID) && id.getPath().startsWith("block/"))
            return true;

        return original;
    }

    @Inject(method = "reload", at = @At("HEAD"))
    public void clearCustomSpriteProviders(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        // runs on Render thread
        Leaves.CUSTOM_LEAVES.clear();
    }

    // may be replaced with ModifyExpressionValue + ModifyArgs to get ReloadResult.id() (which is a local class that can't be accesswidened)
    @WrapOperation(
        method = "method_45767", // last lambda inside reload, forEach(result -> ...)
        at = @At(
            // spriteAwareFactories.get(result.id()))
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 1
        )
    )
    public Object useCustomSpriteProvider(Map<Identifier, Object> instance, Object id, Operation<Object> operation) {
        // runs on Render thread
        Object spriteProvider = operation.call(instance, id);

        if (spriteProvider == null) {
            var customSpriteProvider = SimpleSpriteProviderInvoker.init();
            Leaves.CUSTOM_LEAVES.put((Identifier) id, customSpriteProvider);
            return customSpriteProvider;
        }

        return spriteProvider;
    }

}
