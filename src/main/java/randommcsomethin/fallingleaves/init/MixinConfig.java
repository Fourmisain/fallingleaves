package randommcsomethin.fallingleaves.init;

import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import randommcsomethin.fallingleaves.FallingLeavesClient;
import randommcsomethin.fallingleaves.config.gson.GsonConfigHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MixinConfig implements IMixinConfigPlugin {
	@SuppressWarnings("rawtypes")
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String mixinName = mixinClassName.substring(mixinClassName.lastIndexOf(".mixin") + ".mixin".length() + 1);

		// we can't use CONFIG here since it'll load Identifier too early, we thus manually load leafSpawners
		List leafSpawners = List.of();
		try {
			GsonConfigHelper gsonHelper = new GsonConfigHelper(FallingLeavesClient.MOD_ID);
			if (gsonHelper.exists()) {
				Map config = gsonHelper.load(Map.class);
				if (config.get("leafSpawners") != null) {
					leafSpawners = (List) config.get("leafSpawners");
				}
			}
		} catch (IOException | JsonParseException ignored) {
		} catch (Exception e) {
			FallingLeavesClient.LOGGER.error("failed checking for leafSpawners entries: {}", e.getMessage());
		}

		// only apply "leaf spawner" mixin if spawners are defined
		if (mixinName.equals("BlockMixin"))
			return !leafSpawners.isEmpty();

		return true;
	}

	@Override
	public List<String> getMixins() {
		return FabricLoader.getInstance().isModLoaded("bloom") ? null : List.of("GsonConfigSerializerMixin");
	}

	@Override public void onLoad(String mixinPackage) {}
	@Override public String getRefMapperConfig() { return null; }
	@Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
	@Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
	@Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
