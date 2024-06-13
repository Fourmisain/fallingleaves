package randommcsomethin.fallingleaves.init;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import randommcsomethin.fallingleaves.FallingLeavesClient;
import randommcsomethin.fallingleaves.config.FallingLeavesConfig;
import randommcsomethin.fallingleaves.config.FallingLeavesConfigV0;
import randommcsomethin.fallingleaves.config.gson.GsonConfigHelper;
import randommcsomethin.fallingleaves.config.gson.IdentifierTypeAdapter;
import randommcsomethin.fallingleaves.config.gson.LeafSettingsTypeAdapter;
import randommcsomethin.fallingleaves.config.gui.IdentifierGuiProvider;
import randommcsomethin.fallingleaves.config.gui.LeafSettingsGuiProvider;
import randommcsomethin.fallingleaves.config.gui.StringSetGuiProvider;
import randommcsomethin.fallingleaves.util.Wind;

import java.io.IOException;
import java.util.List;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;

public class Config {

    public static FallingLeavesConfig CONFIG;
    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeAdapter(Identifier.class, IdentifierTypeAdapter.INST)
        .registerTypeAdapterFactory(new LeafSettingsTypeAdapter())
        .create();

    private static ConfigHolder<FallingLeavesConfig> configHolder;

    public static void init() {
        migrateOldConfig();

        configHolder = AutoConfig.register(FallingLeavesConfig.class, (definition, configClass) -> new GsonConfigSerializer<>(definition, configClass, GSON));
        CONFIG = configHolder.getConfig();

        configHolder.registerSaveListener((manager, data) -> {
            try {
                data.validatePostLoad();
            } catch (ConfigData.ValidationException ignored) { }
            Wind.init();
            return ActionResult.SUCCESS;
        });

        // Note: Configurator.setLevel() might not be supported in future versions of log4j.
        if (CONFIG.displayDebugData && LOGGER.getLevel().compareTo(Level.DEBUG) < 0) {
            Configurator.setLevel(LOGGER.getName(), Level.DEBUG);
        }

        AutoConfig.getGuiRegistry(FallingLeavesConfig.class).registerPredicateProvider(
            new LeafSettingsGuiProvider(),
            field -> field.getName().equals("leafSettings")
        );

        for (var guiProvider : List.of(
            new StringSetGuiProvider<>(Identifier.class, Identifier::of),
            new StringSetGuiProvider<>(String.class, s -> s)
        )) {
            AutoConfig.getGuiRegistry(FallingLeavesConfig.class).registerPredicateProvider(guiProvider, guiProvider.getPredicate());
        }

        AutoConfig.getGuiRegistry(FallingLeavesConfig.class).registerTypeProvider(
            new IdentifierGuiProvider(),
            Identifier.class
        );

        LOGGER.debug("Loaded configuration.");
    }

    public static void save() {
        configHolder.save();
    }

    /** Migrates the old config v0 (1.0 to 1.4) to v1 (1.5+) */
    private static void migrateOldConfig() {
        GsonConfigHelper gsonHelper = new GsonConfigHelper(FallingLeavesClient.MOD_ID, GSON);
        if (!gsonHelper.exists()) return; // nothing to migrate

        FallingLeavesConfigV0 oldConfig;
        try {
            oldConfig = gsonHelper.load(FallingLeavesConfigV0.class);
        } catch (IOException | JsonParseException e) {
            // If loading failed, we assume the config is already migrated
            LOGGER.debug("Couldn't load config as v0, assuming it is v1");
            return;
        }

        // v1 can successfully load as v0, so we test the added version field
        if (oldConfig.version != 0) {
            return;
        }

        LOGGER.info("Migrating old v0 config");

        FallingLeavesConfig newConfig = new FallingLeavesConfig();

        // In 1.4 leafSize was a double with default 0.1, which matches newConfig.getLeafSize()
        newConfig.setLeafSize(oldConfig.leafSize);

        // leafLifespan was untouched
        newConfig.leafLifespan = oldConfig.leafLifespan;

        // Actual spawn rates did not change
        newConfig.setLeafSpawnRate(oldConfig.leafRate);
        newConfig.setConiferLeafSpawnRate(oldConfig.coniferLeafRate);

        // Conifer Leaves were moved to Leaf Settings
        for (String leafId : oldConfig.coniferLeafIds) {
            newConfig.updateLeafSettings(Identifier.of(leafId), (entry) -> entry.isConiferBlock= true);
        }

        // Rate Overrides were replaced by Spawn Rate Factors/Multipliers
        for (var oldEntry : oldConfig.rateOverrides.entrySet()) {
            newConfig.updateLeafSettings(Identifier.of(oldEntry.getKey()), (newEntry) -> {
                double oldRateOverride = oldEntry.getValue();

                // Set the new factor according the override and base rate
                if (newEntry.isConiferBlock) {
                    // Effective change: leaf rate 0 now means no leaves will spawn at all
                    if (oldConfig.coniferLeafRate != 0) {
                        newEntry.spawnRateFactor = (oldRateOverride / oldConfig.coniferLeafRate);
                    }
                } else {
                    if (oldConfig.leafRate != 0) {
                        newEntry.spawnRateFactor = (oldRateOverride / oldConfig.leafRate);
                    }
                }
            });
        }

        try {
            gsonHelper.save(newConfig);
            LOGGER.info("Migrated successfully");
        } catch (IOException | JsonIOException e) {
            LOGGER.error("Couldn't save migrated config!", e);
        }
    }

}
