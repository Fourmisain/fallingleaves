package randommcsomethin.fallingleaves.config;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;
import static randommcsomethin.fallingleaves.config.ConfigDefaults.DEFAULT_SPAWN_CHANCE;

@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal", "FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
@Config(name = "fallingleaves2")
public class FallingLeavesConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    @ConfigEntry.Category("fallingleaves.general")
    public int version = 2;

    @ConfigEntry.Gui.Excluded
    @ConfigEntry.Category("fallingleaves.general")
    public boolean displayDebugData = false;

    @ConfigEntry.Category("fallingleaves.general")
    public boolean enabled = true;

    @ConfigEntry.Category("fallingleaves.general")
    public boolean alwaysUseVanillaParticles = false;

    @ConfigEntry.Category("fallingleaves.general")
    public boolean useVanillaTextures = false;

    @ConfigEntry.Category("fallingleaves.general")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int leafSize = 5;

    public float getLeafSize() {
        return leafSize / 50F;
    }

    @ConfigEntry.Category("fallingleaves.general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 100, max = 600)
    public int leafLifespan = 200;

    @ConfigEntry.Category("fallingleaves.general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 40)
    private int leafSpawnRate = 20;

    // 1.4 had a default spawn chance of 1.0 / 75 ~ 1.33% that could be set to 100%
    // 1.5 has the same default but is bounded to ~2.66% except when boosted by a spawn rate factor,
    // the maximum upper bound is currently ~26.6%
    // 2.0 is based on the vanilla default of 1%, which goes up to 10% for cherry
    public float getBaseLeafSpawnChance() {
        float factor = leafSpawnRate / 20.0f;
        return factor * DEFAULT_SPAWN_CHANCE;
    }

    @ConfigEntry.Category("fallingleaves.general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 40)
    private int coniferLeafSpawnRate = 0;

    public float getBaseConiferLeafSpawnChance() {
        float factor = coniferLeafSpawnRate / 20.0f;
        return factor * DEFAULT_SPAWN_CHANCE;
    }

    @ConfigEntry.Category("fallingleaves.general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(max = 40)
    private int snowflakeSpawnRate = 30;

    public float getSnowflakeSpawnChance() {
        float factor = snowflakeSpawnRate / 20.0f;
        return factor * DEFAULT_SPAWN_CHANCE;
    }

    @ConfigEntry.Category("fallingleaves.general")
    public boolean dropFromPlayerPlacedBlocks = true;

    @ConfigEntry.Category("fallingleaves.general")
    public boolean leavesOnBlockHit = true;

    @ConfigEntry.Category("fallingleaves.general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int minimumFreeSpaceBelow = 1;

    @ConfigEntry.Category("fallingleaves.general")
    @ConfigEntry.Gui.Tooltip
    public boolean windEnabled = true;

    @ConfigEntry.Category("fallingleaves.general")
    public Set<Identifier> windlessDimensions = new HashSet<>(Arrays.asList(
        Identifier.ofVanilla("the_nether"), Identifier.ofVanilla("the_end")));

    @ConfigEntry.Category("fallingleaves.leafsettings")
    @ConfigEntry.Gui.TransitiveObject
    public Map<Identifier, LeafSettingsEntry> leafSettings = new HashMap<>();

    @ConfigEntry.Category("fallingleaves.experimental")
    @ConfigEntry.Gui.Tooltip(count = 2)
    public Set<String> leafSpawners = new HashSet<>();  // block ids with properties, e.g. minecraft:bamboo[leaves=large]
    @ConfigEntry.Gui.Excluded @ConfigEntry.Category("fallingleaves.experimental")
    private transient Set<Identifier> leafSpawnerIds = new HashSet<>();
    @ConfigEntry.Gui.Excluded @ConfigEntry.Category("fallingleaves.experimental")
    private transient Map<Identifier, Map<Property<?>, Comparable<?>>> leafSpawnerProperties = new HashMap<>();

    @ConfigEntry.Category("fallingleaves.experimental")
    @ConfigEntry.Gui.Tooltip
    public float fallSpawnRateFactor = 1.8f;

    @ConfigEntry.Category("fallingleaves.experimental")
    @ConfigEntry.Gui.Tooltip
    public float winterSpawnRateFactor = 0.1f;

    @ConfigEntry.Category("fallingleaves.experimental")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 23)
    public int startingSpawnRadius = 0;

    @ConfigEntry.Category("fallingleaves.experimental")
    @ConfigEntry.Gui.Tooltip
    public float decaySpawnRateFactor = 2.6f;

    @ConfigEntry.Category("fallingleaves.experimental")
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 30)
    public volatile int maxDecayLeaves = 9;

    @ConfigEntry.Category("fallingleaves.experimental")
    @ConfigEntry.Gui.Tooltip
    public boolean registerParticles = true;

    public boolean isLeafSpawner(Identifier blockId) {
        return leafSpawnerIds.contains(blockId);
    }

    public Map<Property<?>, Comparable<?>> getLeafSpawnerProperties(Identifier blockId) {
        return leafSpawnerProperties.getOrDefault(blockId, Map.of());
    }

    public void updateLeafSettings(Identifier blockId, Consumer<LeafSettingsEntry> f) {
        leafSettings.compute(blockId, (id, entry) -> {
            if (entry == null)
                entry = new LeafSettingsEntry(id);
            f.accept(entry);
            return entry;
        });
    }

    private void parseLeafSpawners() {
        leafSpawnerIds.clear();
        leafSpawnerProperties.clear();

        for (var spawner : leafSpawners) {
            int a = spawner.indexOf("[");
            if (a != -1) {
                // get id
                Identifier id = Identifier.of(spawner.substring(0, a));
                leafSpawnerIds.add(id);

                // parse properties
                try {
                    var block = BlockArgumentParser.block(Registries.BLOCK, spawner, false);
                    leafSpawnerProperties.put(id, block.properties());
                } catch (CommandSyntaxException e) {
                    LOGGER.error("could not parse block state arguments of {}", spawner);
                }
            } else {
                // regular id
                leafSpawnerIds.add(Identifier.of(spawner));
            }
        }
    }

    @Override
    public void validatePostLoad() throws ConfigData.ValidationException {
        version = 2;
        leafSize = Math.max(leafSize, 1);
        minimumFreeSpaceBelow = Math.max(minimumFreeSpaceBelow, 1);

        parseLeafSpawners();

        for (var spawner : leafSpawnerIds) {
            leafSettings.computeIfAbsent(spawner, LeafSettingsEntry::new);
        }
    }
}