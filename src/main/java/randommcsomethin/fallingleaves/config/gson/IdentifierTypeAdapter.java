package randommcsomethin.fallingleaves.config.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.Identifier;

import java.io.IOException;

/** Allows Gson to de/serialize Identifiers */
public class IdentifierTypeAdapter extends TypeAdapter<Identifier> {
    public static final IdentifierTypeAdapter INST = new IdentifierTypeAdapter();

    private IdentifierTypeAdapter() { }

    @Override
    public void write(JsonWriter out, Identifier value) throws IOException {
        out.value(value.toString());
    }

    @Override
    public Identifier read(JsonReader reader) throws IOException {
        return Identifier.parse(reader.nextString());
    }
}