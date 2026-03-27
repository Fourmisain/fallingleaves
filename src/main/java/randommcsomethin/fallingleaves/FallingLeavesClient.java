package randommcsomethin.fallingleaves;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import randommcsomethin.fallingleaves.init.Config;
import randommcsomethin.fallingleaves.init.Leaves;
import randommcsomethin.fallingleaves.util.Wind;

@Environment(EnvType.CLIENT)
public class FallingLeavesClient implements ClientModInitializer {
    public static final String MOD_ID = "fallingleaves";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        Config.init();
        Leaves.init();
        Wind.init(); // probably not needed anymore, just to be sure
    }

}
