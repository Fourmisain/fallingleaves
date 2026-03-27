package randommcsomethin.fallingleaves.config.gui;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import me.shedaniel.clothconfig2.impl.builders.EnumSelectorBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import randommcsomethin.fallingleaves.config.ConfigDefaults;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;
import randommcsomethin.fallingleaves.particle.ParticleImplementation;
import randommcsomethin.fallingleaves.util.TranslationComparator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;

public class LeafSettingsGuiProvider implements GuiProvider {
    private static final MutableComponent RESET_TEXT = Component.translatable("text.cloth-config.reset_value");

    public static String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(c -> c.getMetadata().getName()).orElse(modId);
    }

    @SuppressWarnings({"rawtypes", "unchecked", "ConstantConditions"})
    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        try {
            Map<Identifier, LeafSettingsEntry> leafSettings = (Map<Identifier, LeafSettingsEntry>) field.get(config);
            List<AbstractConfigListEntry> entries = new ArrayList<>(leafSettings.size());

            // Insert per-leaf settings ordered by translation name
            leafSettings.entrySet().stream()
                .filter(e -> BuiltInRegistries.BLOCK.containsKey(e.getKey())) // Only insert registered blocks
                .sorted((e1, e2) -> TranslationComparator.INST.compare(BuiltInRegistries.BLOCK.getValue(e1.getKey()).getDescriptionId(), BuiltInRegistries.BLOCK.getValue(e2.getKey()).getDescriptionId()))
                .forEachOrdered((e) -> {
                    Identifier blockId = e.getKey();
                    LeafSettingsEntry leafEntry = e.getValue();
                    Block block = BuiltInRegistries.BLOCK.getValue(blockId);

                    MutableComponent text = Component.translatable(block.getDescriptionId());
                    if (!leafEntry.isDefault(blockId)) {
                        text.append("*");
                    }

                    SubCategoryBuilder builder = new SubCategoryBuilder(RESET_TEXT, text)
                        .setTooltip(Component.nullToEmpty(getModName(blockId.getNamespace())));

                    builder.add(buildParticleImplementationSelector(blockId, leafEntry));
                    builder.add(buildSpawnRateFactorSlider(blockId, leafEntry));
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
        int max = 1500; // cherry goes up to 1000
        int stepSize = 10;
        int currentValue = (int)(entry.spawnRateFactor * 100.0);
        int defaultValue = (int)(ConfigDefaults.spawnRateFactor(blockId) * 100.0);

        min /= stepSize;
        max /= stepSize;
        currentValue /= stepSize;
        defaultValue /= stepSize;

        return new IntSliderBuilder(RESET_TEXT, Component.translatable("config.fallingleaves2.spawn_rate_factor"), currentValue, min, max)
            .setDefaultValue(defaultValue)
            .setSaveConsumer((Integer value) -> {
                entry.spawnRateFactor = (value * stepSize) / 100.0f;
            })
            .setTextGetter((Integer value) -> {
                return Component.nullToEmpty((value * stepSize) + "%");
            })
            .setTooltip(Component.translatable("config.fallingleaves2.spawn_rate_factor.@Tooltip"))
            .build();
    }

    private static EnumListEntry<ParticleImplementation> buildParticleImplementationSelector(Identifier blockId, LeafSettingsEntry entry) {
        return new EnumSelectorBuilder<>(RESET_TEXT, Component.translatable("config.fallingleaves2.particle_implementation"), ParticleImplementation.class, entry.particleImplementation)
            .setDefaultValue(ConfigDefaults.getImplementation(blockId))
            .setSaveConsumer((ParticleImplementation value) -> {
                entry.particleImplementation = value;
            })
            .setEnumNameProvider(anEnum -> Component.translatable("config.fallingleaves2.particle_implementation." + anEnum.name()))
            .setTooltip(Component.translatable("config.fallingleaves2.particle_implementation.@Tooltip"))
            .build();
    }

    private static BooleanListEntry buildSpawnBreakingLeaves(Identifier blockId, LeafSettingsEntry entry) {
        return new BooleanToggleBuilder(RESET_TEXT, Component.translatable("config.fallingleaves2.spawn_breaking_leaves"), entry.spawnBreakingLeaves)
            .setDefaultValue(ConfigDefaults.spawnBreakingLeaves(blockId))
            .setSaveConsumer((Boolean value) -> {
                entry.spawnBreakingLeaves = value;
            })
            .build();
    }

}