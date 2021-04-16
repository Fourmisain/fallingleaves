package randommcsomethin.fallingleaves.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.Level;
import randommcsomethin.fallingleaves.math.SmoothNoise;
import randommcsomethin.fallingleaves.math.TriangularDistribution;

import java.util.Random;

import static randommcsomethin.fallingleaves.FallingLeavesClient.LOGGER;

public class Wind {
    public static void debug() {
        state = State.values()[(state.ordinal() + 1) % State.values().length];
        ChatHud chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
        chatHud.addMessage(new LiteralText("set wind state to " + state));
    }

    protected static Random rng = new Random();

    protected enum State {
        CALM(  0.025f, 0.025f, 0.1f),
        WINDY( 0.025f,   0.15f,   0.35f),
        STORMY(0.025f,   0.3f,   0.55f); // TODO: only when raining/thundering

        public TriangularDistribution strengthDistribution;

        State(float minStrength, float likelyStrength, float maxStrength) {
            this.strengthDistribution = new TriangularDistribution(minStrength, maxStrength, likelyStrength, rng);
        }
    }

    protected static float TAU = (float)(2 * Math.PI);

    public static float windX;
    public static float windZ;

    protected static SmoothNoise strengthNoise;
    protected static SmoothNoise directionTrendNoise;
    protected static SmoothNoise directionNoise;

    protected static State state;
    protected static int stateDuration; // ticks

    public static void init() {
        LOGGER.debug("Wind.init");
        stateDuration = 0;
        updateState();
        windX = windZ = 0;
        strengthNoise = new SmoothNoise(2 * 20, 0, (old) -> {
            return state.strengthDistribution.sample();
        });
        directionTrendNoise = new SmoothNoise(30 * 60 * 20, rng.nextFloat() * TAU, (old) -> {
            return rng.nextFloat() * TAU;
        });
        directionNoise = new SmoothNoise(10 * 20, 0, (old) -> {
            return (2f * rng.nextFloat() - 1f) * TAU / 8f;
        });
    }

    protected static void updateState() {
        stateDuration--;
        if (stateDuration <= 0) {
            state = State.values()[rng.nextInt(State.values().length)];
            LOGGER.debug("new wind state {}", state);
            stateDuration = 6 * 60 * 20; // change state every 6 minutes
        }
    }

    public static void tick() {
        updateState();

        strengthNoise.tick();
        directionTrendNoise.tick();
        directionNoise.tick();

        float strength = strengthNoise.getNoise();
        float direction = directionTrendNoise.getLerp() + directionNoise.getNoise();

        /**
        LOGGER.printf(Level.DEBUG, "state %s strength %.2f -> %.2f direction var %.2f° -> %.2f°, trend %.2f° -> %.2f°",
            state.toString(),
            strengthNoise.getNoise(),
            strengthNoise.getRightNoise(),
            directionNoise.getNoise() * 360.0 / TAU,
            directionNoise.getRightNoise() * 360.0 / TAU,
            directionTrendNoise.getLerp() * 360.0 / TAU,
            directionTrendNoise.getRightNoise() * 360.0 / TAU);
        /**/

        // calculate wind force (blocks / tick² when multiplied by mass)
        windX = strength * MathHelper.cos(direction);
        windZ = strength * MathHelper.sin(direction);
    }
}
