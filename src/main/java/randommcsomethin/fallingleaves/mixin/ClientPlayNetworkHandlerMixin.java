package randommcsomethin.fallingleaves.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.init.Config;
import randommcsomethin.fallingleaves.util.LeafUtil;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;
import static randommcsomethin.fallingleaves.init.Config.CONFIG;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    public void loadRegisteredLeafBlocks(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        LOGGER.info("Loading all registered leaf blocks.");

        // This is pretty much the earliest point in time where we can use block tags
        // So we add all leaf blocks that weren't already read from the config file or preloaded in our ReloadListener
        for (var registered : LeafUtil.getRegisteredLeafBlocks(true).entrySet())
            CONFIG.leafSettings.computeIfAbsent(registered.getKey(), k -> registered.getValue());

        Config.save();
    }

}
