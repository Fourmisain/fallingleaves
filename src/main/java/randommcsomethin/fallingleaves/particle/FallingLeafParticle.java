package randommcsomethin.fallingleaves.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import randommcsomethin.fallingleaves.init.Leaves;
import randommcsomethin.fallingleaves.util.LeafUtil;
import randommcsomethin.fallingleaves.util.Wind;

import java.util.List;

import static randommcsomethin.fallingleaves.FallingLeavesClient.id;
import static randommcsomethin.fallingleaves.init.Config.CONFIG;

public class FallingLeafParticle extends SpriteBillboardParticle {

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

    public FallingLeafParticle(ParticleType<BlockStateParticleEffect> particleType, ClientWorld clientWorld, double x, double y, double z, double r, double g, double b, SpriteProvider provider) {
        super(clientWorld, x, y, z, 0.0, 0.0, 0.0);
        this.setSprite(provider);

        if (particleType == Leaves.FALLING_SNOW) {
            this.gravityStrength = 0.0125f + random.nextFloat() * 0.0125f;
            this.windCoefficient = 0.1f + random.nextFloat() * 0.1f;
        } else {
            this.gravityStrength = 0.08f + random.nextFloat() * 0.04f;
            this.windCoefficient = 0.6f + random.nextFloat() * 0.4f;
        }

        // the Particle constructor adds random noise to the velocity which we don't want
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;

        this.maxAge = CONFIG.leafLifespan;

        this.red   = (float) r;
        this.green = (float) g;
        this.blue  = (float) b;

        if (particleType == Leaves.FALLING_SNOW) {
            // accelerate over 3-7 seconds to at most 1 rotation per second
            this.maxRotateTime = (3 + random.nextInt(4 + 1)) * 20;
            this.maxRotateSpeed = (random.nextBoolean() ? -1 : 1) * (0.1f + 0.4f * random.nextFloat()) * TAU / 20f;
        } else {
            // accelerate over 3-7 seconds to at most 2.5 rotations per second
            this.maxRotateTime = (3 + random.nextInt(4 + 1)) * 20;
            this.maxRotateSpeed = (random.nextBoolean() ? -1 : 1) * (0.1f + 2.4f * random.nextFloat()) * TAU / 20f;
        }

        this.angle = this.prevAngle = random.nextFloat() * TAU;

        this.scale = CONFIG.getLeafSize();

        if (random.nextBoolean())
            this.scale *= 1.5f;
    }

    @Override
    public void tick() {
        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
        prevAngle = angle;

        age++;

        // fade-out animation
        if (age >= maxAge + 1 - FADE_DURATION) {
            alpha -= 1F / FADE_DURATION;
        }

        if (age >= maxAge) {
            markDead();
            return;
        }

        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        FluidState fluidState = world.getFluidState(blockPos);

        if (fluidState.isIn(FluidTags.LAVA)) {
            double waterY = blockPos.getY() + fluidState.getHeight(world, blockPos);
            if (waterY >= y) {
                world.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
                markDead();
                return;
            }
        }

        // apply gravity and weight of rain (note: hasRain() is probably too expensive)
        velocityY -= 0.04 * (gravityStrength + (world.isRaining() && world.isSkyVisible(blockPos) ? 0.04 : 0));

        if (fluidState.isIn(FluidTags.WATER)) {
            double waterY;
            if ((waterY = blockPos.getY() + fluidState.getHeight(world, blockPos)) >= y - 0.1) {
                if (!inWater) {
                    // hit water for the first time
                    inWater = true;

                    if (Math.abs(waterY - y) < 0.2)
                        y = waterY;

                    velocityY *= 0.1;
                    velocityX *= 0.5;
                    velocityZ *= 0.5;

                    rotateTime = 0;
                } else {
                    // buoyancy - try to stay on top of the water surface
                    double depth = Math.max(waterY + 0.1 - y, 0);
                    velocityY += depth * windCoefficient / 30.0f;
                }

                if (!fluidState.isStill()) {
                    Vec3d pushVel = fluidState.getVelocity(world, blockPos).multiply(0.4);
                    velocityX += (pushVel.x - velocityX) * windCoefficient / 60.0f;
                    velocityZ += (pushVel.z - velocityZ) * windCoefficient / 60.0f;
                }

                velocityX *= (1 - WATER_FRICTION);
                velocityY *= (1 - WATER_FRICTION);
                velocityZ *= (1 - WATER_FRICTION);
            }
        } else {
            // note: intentionally inaccurate, so the leaves don't constantly switch between being blown by wind and hitting water again
            inWater = false;

            if (!onGround) {
                // spin when in the air
                rotateTime = Math.min(rotateTime + 1, maxRotateTime);
                angle += (rotateTime / (float) maxRotateTime) * maxRotateSpeed;
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
            velocityX += (Wind.windX - velocityX) * windCoefficient / 60.0f;
            velocityZ += (Wind.windZ - velocityZ) * windCoefficient / 60.0f;
        }

        move(velocityX, velocityY, velocityZ);
    }

    @Override
    public void move(double dx, double dy, double dz) {
        if (dx == 0.0 && dy == 0.0 && dz == 0.0) return;

        double oldDx = dx;
        double oldDy = dy;
        double oldDz = dz;

        // TODO: is it possible to turn off collisions with leaf blocks?
        Vec3d vec3d = Entity.adjustMovementForCollisions(null, new Vec3d(dx, dy, dz), getBoundingBox(), world, List.of());
        dx = vec3d.x;
        dy = vec3d.y;
        dz = vec3d.z;

        // lose horizontal velocity on collision
        if (oldDx != dx) velocityX = 0.0;
        if (oldDz != dz) velocityZ = 0.0;

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
            velocityX = 0.0;
            velocityY = 0.0;
            velocityZ = 0.0;

            // don't move
            return;
        }

        if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
            setBoundingBox(getBoundingBox().offset(dx, dy, dz));
            repositionFromBoundingBox();
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public record BlockStateFactory(SpriteProvider spriteProvider) implements ParticleFactory<BlockStateParticleEffect> {
        @Override
        public Particle createParticle(BlockStateParticleEffect parameters, ClientWorld world, double x, double y, double z, double unusedX, double unusedY, double unusedZ) {
            double r, g, b;

            var particleType = parameters.getType();
            SpriteProvider customSpriteProvider = null;

            if (particleType == Leaves.FALLING_SNOW) {
                r = g = b = 1;
            } else {
                var blockId = Registries.BLOCK.getId(parameters.getBlockState().getBlock());
                customSpriteProvider = Leaves.CUSTOM_LEAVES.get(id("block/%s/%s".formatted(blockId.getNamespace(), blockId.getPath())));

                double[] color = LeafUtil.getBlockTextureColor(parameters.getBlockState(), world, BlockPos.ofFloored(x, y, z));

                r = color[0];
                g = color[1];
                b = color[2];
            }

            return new FallingLeafParticle(particleType, world, x, y, z, r, g, b, customSpriteProvider != null ? customSpriteProvider : spriteProvider);
        }
    }

}
