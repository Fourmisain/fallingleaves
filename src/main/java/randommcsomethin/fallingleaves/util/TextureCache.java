package randommcsomethin.fallingleaves.util;

import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.HashMap;

public class TextureCache {
    public record Data(double[] color) {
        public Data(double[] color) {
            if (color.length != 3)
                throw new IllegalArgumentException("texture color should have 3 components");

            this.color = Arrays.copyOf(color, color.length);
        }

        public double[] getColor() {
            return Arrays.copyOf(color, color.length);
        }
    }

    public static final HashMap<Identifier, Data> INST = new HashMap<>();

    private TextureCache() {}

}
