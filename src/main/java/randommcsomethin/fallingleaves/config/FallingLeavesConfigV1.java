package randommcsomethin.fallingleaves.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.minecraft.util.Identifier;
import randommcsomethin.fallingleaves.FallingLeavesClient;

import java.util.*;
import java.util.function.Consumer;

/** v1 config format used in versions 1.5 to 1.17 */
@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal", "FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection", "unused"})
@Config(name = FallingLeavesClient.MOD_ID)
public class FallingLeavesConfigV1 {
    public int version = 1;
    public boolean displayDebugData = false;
    public boolean enabled = true;
    public int leafSize = 5;
    public int leafLifespan = 200;

    // won't be migrated
    public int leafSpawnRate = 10;
    public int coniferLeafSpawnRate = 0;
    public int cherrySpawnRate = 10;
    public int paleOakSpawnRate = 10;
    public int snowflakeSpawnRate = 15;

    public boolean dropFromPlayerPlacedBlocks = true;
    public boolean leavesOnBlockHit = true;
    public int minimumFreeSpaceBelow = 1;
    public boolean windEnabled = true;
    public Set<Identifier> windlessDimensions = new HashSet<>(Arrays.asList(Identifier.ofVanilla("the_nether"), Identifier.ofVanilla("the_end")));
    public Map<Identifier, LeafSettingsEntryV1> leafSettings = new HashMap<>();
    public Set<String> leafSpawners = new HashSet<>();  // block ids with properties, e.g. minecraft:bamboo[leaves=large]
    public double fallSpawnRateFactor = 1.8;
    public double winterSpawnRateFactor = 0.1;
    public int startingSpawnRadius = 0;
    public double decaySpawnRateFactor = 2.6;
    public volatile int maxDecayLeaves = 9;
    public boolean registerParticles = true;

    public void updateLeafSettings(Identifier blockId, Consumer<LeafSettingsEntryV1> f) {
        leafSettings.compute(blockId, (id, entry) -> {
            if (entry == null)
                entry = new LeafSettingsEntryV1(id);
            f.accept(entry);
            return entry;
        });
    }

    public void setLeafSize(double leafSize) {
        this.leafSize = (int)(leafSize * 50.0);
    }

    public void setLeafSpawnRate(double leafRate) {
        leafSpawnRate = (int)(leafRate * 10.0);
    }

    public void setConiferLeafSpawnRate(double coniferLeafRate) {
        coniferLeafSpawnRate = (int)(coniferLeafRate * 10.0);
    }

    public void validatePostLoad() throws ConfigData.ValidationException {
        version = 1;
        leafSize = Math.max(leafSize, 1);
        minimumFreeSpaceBelow = Math.max(minimumFreeSpaceBelow, 1);
    }
}