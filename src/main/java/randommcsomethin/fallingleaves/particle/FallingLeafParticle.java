package randommcsomethin.fallingleaves.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import randommcsomethin.fallingleaves.init.Leaves;
import randommcsomethin.fallingleaves.util.LeafUtil;
import randommcsomethin.fallingleaves.util.Wind;

import java.util.List;

import static randommcsomethin.fallingleaves.FallingLeavesClient.id;
import static randommcsomethin.fallingleaves.init.Config.CONFIG;
import static randommcsomethin.fallingleaves.util.LeafUtil.CHERRY_LEAVES_PARTICLE_ID;
import static randommcsomethin.fallingleaves.util.LeafUtil.TINTED_LEAVES_PARTICLE_ID;

public class FallingLeafParticle extends SingleQuadParticle {

    public static final float TAU = (float)(2 * Math.PI); // 1 rotation

    public static final int FADE_DURATION = 16; // ticks
    // public static final double FRICTION       = 0.30;
    public static final double WATER_FRICTION = 0.075;

    protected final float windCoefficient; // to emulate drag/lift

    protected final float maxRotateSpeed; // rotations / tick
    protected final int maxRotateTime;
    protected int rotateTime = 0;
    protected boolean inWater = false;
    protected boolean stuckInGround = false;

    public FallingLeafParticle(ParticleType<BlockParticleOption> particleType, ClientLevel level, double x, double y, double z, double r, double g, double b, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprite);

        if (particleType == Leaves.FALLING_CHERRY) {
            this.gravity = 0.0175f + random.nextFloat() * 0.0050f;
            this.windCoefficient = 0.12f + random.nextFloat() * 0.8f;
        } else if (particleType == Leaves.FALLING_SNOW) {
            this.gravity = 0.0125f + random.nextFloat() * 0.0125f;
            this.windCoefficient = 0.1f + random.nextFloat() * 0.1f;
        } else {
            this.gravity = 0.08f + random.nextFloat() * 0.04f;
            this.windCoefficient = 0.6f + random.nextFloat() * 0.4f;
        }

        // the Particle constructor adds random noise to the velocity which we don't want
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;

        this.lifetime = CONFIG.leafLifespan;

        this.rCol   = (float) r;
        this.gCol = (float) g;
        this.bCol  = (float) b;

        if (particleType == Leaves.FALLING_CHERRY || particleType == Leaves.FALLING_SNOW) {
            // accelerate over 3-7 seconds to at most 1 rotation per second
            this.maxRotateTime = (3 + random.nextInt(4 + 1)) * 20;
            this.maxRotateSpeed = (random.nextBoolean() ? -1 : 1) * (0.1f + 0.4f * random.nextFloat()) * TAU / 20f;
        } else {
            // accelerate over 3-7 seconds to at most 2.5 rotations per second
            this.maxRotateTime = (3 + random.nextInt(4 + 1)) * 20;
            this.maxRotateSpeed = (random.nextBoolean() ? -1 : 1) * (0.1f + 2.4f * random.nextFloat()) * TAU / 20f;
        }

        this.roll = this.oRoll = random.nextFloat() * TAU;

        if (particleType == Leaves.FALLING_CHERRY) {
            this.quadSize = CONFIG.getLeafSize() / 2.0f;
        } else {
            this.quadSize = CONFIG.getLeafSize();
        }

        if (random.nextBoolean())
            this.quadSize *= 1.5f;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;

        age++;

        // fade-out animation
        if (age >= lifetime + 1 - FADE_DURATION) {
            alpha -= 1F / FADE_DURATION;
        }

        if (age >= lifetime) {
            remove();
            return;
        }

        BlockPos blockPos = BlockPos.containing(x, y, z);
        FluidState fluidState = level.getFluidState(blockPos);

        if (fluidState.is(FluidTags.LAVA)) {
            double waterY = blockPos.getY() + fluidState.getHeight(level, blockPos);
            if (waterY >= y) {
                level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
                remove();
                return;
            }
        }

        // apply gravity and weight of rain (note: hasRain() is probably too expensive)
        yd -= 0.04 * (gravity + (level.isRaining() && level.canSeeSky(blockPos) ? 0.04 : 0));

        if (fluidState.is(FluidTags.WATER)) {
            double waterY;
            if ((waterY = blockPos.getY() + fluidState.getHeight(level, blockPos)) >= y - 0.1) {
                if (!inWater) {
                    // hit water for the first time
                    inWater = true;

                    if (Math.abs(waterY - y) < 0.2)
                        y = waterY;

                    yd *= 0.1;
                    xd *= 0.5;
                    zd *= 0.5;

                    rotateTime = 0;
                } else {
                    // buoyancy - try to stay on top of the water surface
                    double depth = Math.max(waterY + 0.1 - y, 0);
                    yd += depth * windCoefficient / 30.0f;
                }

                if (!fluidState.isSource()) {
                    Vec3 pushVel = fluidState.getFlow(level, blockPos).scale(0.4);
                    xd += (pushVel.x - xd) * windCoefficient / 60.0f;
                    zd += (pushVel.z - zd) * windCoefficient / 60.0f;
                }

                xd *= (1 - WATER_FRICTION);
                yd *= (1 - WATER_FRICTION);
                zd *= (1 - WATER_FRICTION);
            }
        } else {
            // note: intentionally inaccurate, so the leaves don't constantly switch between being blown by wind and hitting water again
            inWater = false;

            if (!onGround) {
                // spin when in the air
                rotateTime = Math.min(rotateTime + 1, maxRotateTime);
                roll += (rotateTime / (float) maxRotateTime) * maxRotateSpeed;
            } else {
                rotateTime = 0;

                // TODO: leaves get stuck in the ground which is nice sometimes, but some/most leaves should
                //       still get blown by the wind / tumble over the ground
                // velocityX *= (1 - FRICTION);
                // velocityZ *= (1 - FRICTION);
            }

            // approach the target wind velocity over time via vel += (target - vel) * f, where f is in (0, 1)
            // after n ticks, the distance closes to a factor of 1 - (1 - f)^n.
            // for f = 1 / 2, it would only take 4 ticks to close the distance by 90%
            // for f = 1 / 60, it takes ~2 seconds to halve the distance, ~5 seconds to reach 80%
            //
            // the wind coefficient is just another factor in (0, 1) to add some variance between leaves.
            // this implementation lags behind the actual wind speed and will never reach it fully,
            // so wind speeds needs to be adjusted accordingly
            xd += (Wind.windX - xd) * windCoefficient / 60.0f;
            zd += (Wind.windZ - zd) * windCoefficient / 60.0f;
        }

        move(xd, yd, zd);
    }

    @Override
    public void move(double dx, double dy, double dz) {
        if (dx == 0.0 && dy == 0.0 && dz == 0.0) return;

        double oldDx = dx;
        double oldDy = dy;
        double oldDz = dz;

        // TODO: is it possible to turn off collisions with leaf blocks?
        Vec3 vec3d = Entity.collideBoundingBox(null, new Vec3(dx, dy, dz), getBoundingBox(), level, List.of());
        dx = vec3d.x;
        dy = vec3d.y;
        dz = vec3d.z;

        // lose horizontal velocity on collision
        if (oldDx != dx) xd = 0.0;
        if (oldDz != dz) zd = 0.0;

        onGround = oldDy != dy && oldDy < 0.0;

        if (!onGround) {
            stuckInGround = false;
        } else {
            // get stuck if slow enough
            if (!stuckInGround && Math.abs(dy) < 1E-5) {
                stuckInGround = true;
            }
        }

        if (stuckInGround) {
            // don't accumulate speed over time
            xd = 0.0;
            yd = 0.0;
            zd = 0.0;

            // don't move
            return;
        }

        if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
            setBoundingBox(getBoundingBox().move(dx, dy, dz));
            setLocationFromBoundingbox();
        }
    }

    @Override
    protected Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public record BlockStateFactory(SpriteSet spriteProvider) implements ParticleProvider<BlockParticleOption> {
        @Override
        public Particle createParticle(BlockParticleOption parameters, ClientLevel level, double x, double y, double z, double unusedX, double unusedY, double unusedZ, RandomSource random) {
            double r, g, b;

            var particleType = parameters.getType();
            SpriteSet customSpriteProvider = null;

            if (particleType == Leaves.FALLING_CHERRY) {
                // use vanilla textures
                customSpriteProvider = LeafUtil.getSpriteProvider(CHERRY_LEAVES_PARTICLE_ID);
                r = g = b = 1;
            } else if (particleType == Leaves.FALLING_SNOW) {
                r = g = b = 1;
            } else {
                var blockId = BuiltInRegistries.BLOCK.getKey(parameters.getState().getBlock());
                customSpriteProvider = Leaves.CUSTOM_LEAVES.get(id("block/%s/%s".formatted(blockId.getNamespace(), blockId.getPath())));

                if (CONFIG.useVanillaTextures && particleType == Leaves.FALLING_LEAF && customSpriteProvider == null) {
                    customSpriteProvider = LeafUtil.getSpriteProvider(TINTED_LEAVES_PARTICLE_ID);
                }

                double[] color = LeafUtil.getBlockTextureColor(parameters.getState(), level, BlockPos.containing(x, y, z));

                r = color[0];
                g = color[1];
                b = color[2];
            }

            TextureAtlasSprite sprite = (customSpriteProvider != null ? customSpriteProvider : spriteProvider).get(random);

            return new FallingLeafParticle(particleType, level, x, y, z, r, g, b, sprite);
        }
    }

}
