package randommcsomethin.fallingleaves.config.gui;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.block.Block;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import randommcsomethin.fallingleaves.config.ConfigDefaults;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;
import randommcsomethin.fallingleaves.util.ModUtil;
import randommcsomethin.fallingleaves.util.TranslationComparator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;
import static randommcsomethin.fallingleaves.util.RegistryUtil.getBlock;

public class LeafSettingsGuiProvider implements GuiProvider {
    private static final MutableText RESET_TEXT = Text.translatable("text.cloth-config.reset_value");

    @SuppressWarnings({"rawtypes", "unchecked", "ConstantConditions"})
    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        try {
            Map<Identifier, LeafSettingsEntry> leafSettings = (Map<Identifier, LeafSettingsEntry>) field.get(config);
            List<AbstractConfigListEntry> entries = new ArrayList<>(leafSettings.size());

            // Insert per-leaf settings ordered by translation name
            leafSettings.entrySet().stream()
                .filter((e) -> getBlock(e.getKey()) != null) // Only insert registered blocks
                .sorted((e1, e2) -> TranslationComparator.INST.compare(getBlock(e1.getKey()).getTranslationKey(), getBlock(e2.getKey()).getTranslationKey()))
                .forEachOrdered((e) -> {
                    Identifier blockId = e.getKey();
                    LeafSettingsEntry leafEntry = e.getValue();
                    Block block = getBlock(blockId);

                    MutableText text = Text.translatable(block.getTranslationKey());
                    if (!leafEntry.isDefault(blockId)) {
                        text.append("*");
                    }

                    SubCategoryBuilder builder = new SubCategoryBuilder(RESET_TEXT, text)
                        .setTooltip(Text.of(ModUtil.getModName(block)));

                    builder.add(buildSpawnRateFactorSlider(blockId, leafEntry));
                    builder.add(buildIsConiferLeavesToggle(blockId, leafEntry));
                    builder.add(buildSpawnBreakingLeaves(blockId, leafEntry));

                    entries.add(builder.build());
                });

            return entries;
        } catch (IllegalAccessException e) {
            LOGGER.error(e);
            return Collections.emptyList();
        }
    }

    private static IntegerSliderEntry buildSpawnRateFactorSlider(Identifier blockId, LeafSettingsEntry entry) {
        // Percentage values
        int min = 0;
        int max = 1000;
        int stepSize = 10;
        int currentValue = (int)(entry.spawnRateFactor * 100.0);
        int defaultValue = (int)(ConfigDefaults.spawnRateFactor(blockId) * 100.0);

        min /= stepSize;
        max /= stepSize;
        currentValue /= stepSize;
        defaultValue /= stepSize;

        return new IntSliderBuilder(RESET_TEXT, Text.translatable("config.fallingleaves.spawn_rate_factor"), currentValue, min, max)
            .setDefaultValue(defaultValue)
            .setSaveConsumer((Integer value) -> {
                entry.spawnRateFactor = (value * stepSize) / 100.0;
            })
            .setTextGetter((Integer value) -> {
                return Text.of((value * stepSize) + "%");
            })
            .setTooltip(Text.translatable("config.fallingleaves.spawn_rate_factor.@Tooltip"))
            .build();
    }

    private static BooleanListEntry buildIsConiferLeavesToggle(Identifier blockId, LeafSettingsEntry entry) {
        return new BooleanToggleBuilder(RESET_TEXT, Text.translatable("config.fallingleaves.is_conifer"), entry.isConiferBlock)
            .setDefaultValue(ConfigDefaults.isConifer(blockId))
            .setSaveConsumer((Boolean value) -> {
                entry.isConiferBlock = value;
            })
            .build();
    }

    private static BooleanListEntry buildSpawnBreakingLeaves(Identifier blockId, LeafSettingsEntry entry) {
        return new BooleanToggleBuilder(RESET_TEXT, Text.translatable("config.fallingleaves.spawn_breaking_leaves"), entry.spawnBreakingLeaves)
            .setDefaultValue(ConfigDefaults.spawnBreakingLeaves(blockId))
            .setSaveConsumer((Boolean value) -> {
                entry.spawnBreakingLeaves = value;
            })
            .build();
    }

}