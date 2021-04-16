package randommcsomethin.fallingleaves.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SynchronizeTagsS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;
import randommcsomethin.fallingleaves.init.Config;
import randommcsomethin.fallingleaves.util.LeafUtil;
import randommcsomethin.fallingleaves.util.Wind;

import java.util.Map;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;
import static randommcsomethin.fallingleaves.init.Config.CONFIG;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(at = @At("RETURN"), method = "onSynchronizeTags")
    public void onSynchronizeTags(SynchronizeTagsS2CPacket packet, CallbackInfo ci) {
        LOGGER.info("Loading all registered leaf blocks.");

        // This is pretty much the earliest point in time where we can use block tags
        // So we add all leaf blocks that weren't already read from the config file or preloaded in our ReloadListener
        for (Map.Entry<Identifier, LeafSettingsEntry> registered : LeafUtil.getRegisteredLeafBlocks(true).entrySet())
            CONFIG.leafSettings.computeIfAbsent(registered.getKey(), k -> registered.getValue());

        Config.save();

        Wind.init();
    }

}
