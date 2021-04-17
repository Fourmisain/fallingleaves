package randommcsomethin.fallingleaves.config.gui;

import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class IdentifierGuiProvider implements GuiProvider {

    @SuppressWarnings("rawtypes")
    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        return Collections.singletonList(
            ConfigEntryBuilder.create()
                .startStrField(
                    new TranslatableText(i13n),
                    Utils.<Identifier>getUnsafely(field, config).toString()
                )
                .setDefaultValue(() -> Utils.<Identifier>getUnsafely(field, defaults).toString())
                .setSaveConsumer(newValue -> Utils.setUnsafely(field, config, new Identifier(newValue)))
                .build()
        );
    }
}
