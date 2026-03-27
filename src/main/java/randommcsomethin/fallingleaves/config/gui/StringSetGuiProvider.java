package randommcsomethin.fallingleaves.config.gui;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.api.GuiProvider;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class StringSetGuiProvider<T> implements GuiProvider {

    public static boolean predicate(Class<?> clazz, Field field) {
        if (field.getAnnotation(ConfigEntry.Gui.Excluded.class) != null) // since Cloth Config doesn't do this
            return false;

        if (field.getGenericType() instanceof ParameterizedType type) {
            Class<?> rawType = (Class<?>) type.getRawType();
            Type paramType = type.getActualTypeArguments()[0];

            // will work with e.g. Set<T> and HashSet<T> but not with Set<? extends T>
            return Set.class.isAssignableFrom(rawType) && paramType.equals(clazz);
        }

        return false;
    }

    private final Class<T> clazz;
    private final Function<String, T> constructor;

    public StringSetGuiProvider(Class<T> clazz, Function<String, T> constructor) {
        this.clazz = clazz;
        this.constructor = constructor;
    }

    public Predicate<Field> getPredicate() {
        return (field) -> predicate(clazz, field);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<AbstractConfigListEntry> get(String i13n, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        return Collections.singletonList(
            ConfigEntryBuilder.create()
                .startStrList(
                    Component.translatable(i13n),
                    Utils.<Set<T>>getUnsafely(field, config).stream()
                        .map(Object::toString)
                        .collect(toList())
                )
                .setDefaultValue(() -> Utils.<Set<?>>getUnsafely(field, defaults).stream()
                    .map(Object::toString)
                    .collect(toList())
                )
                .setSaveConsumer(newValue -> Utils.setUnsafely(field, config, newValue.stream()
                    .map(constructor)
                    .collect(toSet()))
                )
                .build()
        );
    }
}
