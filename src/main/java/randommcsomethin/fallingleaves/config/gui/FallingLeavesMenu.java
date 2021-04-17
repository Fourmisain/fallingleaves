package randommcsomethin.fallingleaves.config.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import randommcsomethin.fallingleaves.config.FallingLeavesConfig;

@Environment(EnvType.CLIENT)
public class FallingLeavesMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(FallingLeavesConfig.class, parent).get();
    }

}