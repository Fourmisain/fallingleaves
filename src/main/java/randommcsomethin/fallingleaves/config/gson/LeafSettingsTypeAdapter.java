package randommcsomethin.fallingleaves.config.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.Identifier;
import randommcsomethin.fallingleaves.config.LeafSettingsEntry;

import java.io.IOException;
import java.util.Map;

/** Serialize leaf settings without default entries */
public class LeafSettingsTypeAdapter implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!TypeToken.getParameterized(Map.class, Identifier.class, LeafSettingsEntry.class).equals(type)) {
            return null;
        }

        var delegate = gson.getDelegateAdapter(this, type);
        var settingsDelegate = gson.getDelegateAdapter(this, TypeToken.get(LeafSettingsEntry.class));

        return new TypeAdapter<>() {
            @SuppressWarnings("unchecked")
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                var map = (Map<Identifier, LeafSettingsEntry>) value;

                out.beginObject();

                for (var entry : map.entrySet()) {
                    var id = entry.getKey();
                    var settings = entry.getValue();

                    if (!settings.isDefault(id)) {
                        out.name(id.toString());
                        settingsDelegate.write(out, settings);
                    }
                }

                out.endObject();
            }

            @Override
            public T read(JsonReader in) throws IOException {
                return delegate.read(in);
            }
        };
    }
}