package com.craftworld3d.core.entity.vehicle;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.entity.Entity;
import com.craftworld3d.core.entity.Player;
import com.craftworld3d.core.util.MathUtil;
import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.World;

import java.util.Map;

/**
 * Drivable vehicle entity covering cars, planes, helicopters, boats and minecarts.
 *
 * Flight / driving models are intentionally arcade-simple but functional:
 *  - Cars: ground acceleration along yaw, steering scaled by speed, fuel burn, crash damage.
 *  - Planes: need ground-roll above a takeoff speed, then pitch for climb; stall under min speed.
 *  - Helicopter: direct collective (up/down) plus tilt translation.
 *  - Boat: water-only drive, no fuel.
 *  - Minecart: constrained to rails, boosted by powered rails.
 */
public class Vehicle extends Entity {
    public VehicleType type;

    public float health;
    public float fuel;
    /** -1..1 controls set by UI while mounted. */
    public float throttleInput, steerInput, pitchInput;
    public boolean mounted;
    public boolean headlightsOn;
    /** Current forward speed (blocks/s), signed. */
    public float speed;
    public boolean airborne;
    /** Set briefly when horn is pressed (audio hook). */
    public boolean hornPressed;

    public Vehicle(VehicleType type) {
        setType(type);
    }

    protected void setType(VehicleType type) {
        this.type = type;
        this.health = type.maxHealth;
        this.width = type.width;
        this.height = type.height;
        this.fuel = type.needsFuel() ? type.fuelCapacity * 0.25f : 0;
    }

    @Override
    public String typeKey() { return "vehicle:" + type.name(); }

    public boolean addFuel(int units) {
        if (!type.needsFuel() || fuel >= type.fuelCapacity) return false;
        fuel = Math.min(type.fuelCapacity, fuel + units);
        return true;
    }

    public float fuelFraction() {
        return type.needsFuel() ? fuel / type.fuelCapacity : 1f;
    }

    @Override
    public void tick(World world) {
        if (health <= 0) { dead = true; return; }

        switch (type.kind) {
            case CAR -> tickCar(world);
            case PLANE -> tickPlane(world);
            case HELICOPTER -> tickHelicopter(world);
            case BOAT -> tickBoat(world);
            case MINECART -> tickMinecart(world);
        }

        // Crash damage on hard impacts.
        float impact = (float) Math.abs(vy);
        move(world, vx * DT, vy * DT, vz * DT);
        if (collidedHorizontally && Math.abs(speed) > 8) {
            health -= Math.abs(speed) * 0.4f;
            speed *= 0.3f;
        }
        if (onGround && impact > 14) {
            health -= (impact - 14) * 1.5f;
        }
        if (!mounted) {
            throttleInput = steerInput = pitchInput = 0;
        }
    }

    private boolean consumeFuel(float amount) {
        if (!type.needsFuel()) return true;
        if (fuel <= 0) return false;
        fuel = Math.max(0, fuel - amount);
        return true;
    }

    private void tickCar(World world) {
        boolean powered = mounted && Math.abs(throttleInput) > 0.05f
            && consumeFuel(Math.abs(throttleInput) * 0.012f * DT * 20);
        if (powered) {
            speed += throttleInput * type.accel * DT;
            speed = MathUtil.clamp(speed, -type.maxSpeed * 0.4f, type.maxSpeed);
        } else {
            speed *= onGround ? 0.95f : 0.995f;
        }
        if (Math.abs(speed) > 0.5f && mounted) {
            yaw += steerInput * 70f * DT * Math.signum(speed) * Math.min(1, 6 / Math.abs(speed) + 0.4);
        }
        double yawRad = Math.toRadians(yaw);
        vx = -Math.sin(yawRad) * speed;
        vz = Math.cos(yawRad) * speed;
        vy -= GRAVITY * DT;
        airborne = !onGround;
    }

    private void tickPlane(World world) {
        float takeoff = type.maxSpeed * 0.45f;
        boolean powered = mounted && throttleInput > 0.05f
            && consumeFuel(throttleInput * 0.02f * DT * 20);
        if (powered) {
            speed += throttleInput * type.accel * DT;
        } else {
            speed *= airborne ? 0.995f : 0.97f;
        }
        speed = MathUtil.clamp(speed, 0, type.maxSpeed);

        if (mounted) yaw += steerInput * 40f * DT;
        double yawRad = Math.toRadians(yaw);
        vx = -Math.sin(yawRad) * speed;
        vz = Math.cos(yawRad) * speed;

        if (speed >= takeoff) {
            airborne = true;
            // Lift balances gravity; pitch input climbs/dives.
            float climb = mounted ? -pitchInput * 10f : 0;
            float lift = GRAVITY * Math.min(1f, speed / takeoff);
            vy += (lift - GRAVITY) * DT + climb * DT;
            vy *= 0.98;
        } else {
            // Below stall speed: sink (gently if some speed remains).
            vy -= GRAVITY * DT * (airborne ? 0.35f : 1f);
            if (onGround) airborne = false;
        }
    }

    private void tickHelicopter(World world) {
        boolean powered = mounted && Math.abs(throttleInput) > 0.05f
            && consumeFuel(Math.abs(throttleInput) * 0.02f * DT * 20);
        if (powered) {
            vy += throttleInput * 14f * DT - GRAVITY * DT * 0.0f;
            vy = MathUtil.clamp((float) vy, -8, 7);
            airborne = true;
        } else {
            vy -= GRAVITY * DT * 0.5f;
        }
        if (mounted) {
            yaw += steerInput * 60f * DT;
            // Pitch input slides forward/back.
            speed += -pitchInput * type.accel * DT;
            speed *= 0.97f;
            speed = MathUtil.clamp(speed, -type.maxSpeed * 0.4f, type.maxSpeed);
        } else {
            speed *= 0.95f;
        }
        double yawRad = Math.toRadians(yaw);
        vx = -Math.sin(yawRad) * speed;
        vz = Math.cos(yawRad) * speed;
        if (onGround) airborne = false;
    }

    private void tickBoat(World world) {
        boolean onWater = world.isLiquidAt(x, y + 0.1, z) || world.isLiquidAt(x, y - 0.3, z);
        if (onWater) {
            // Float on the surface.
            vy = Math.min(vy + 20f * DT, 1.5);
            if (mounted) {
                speed += throttleInput * type.accel * DT;
                speed = MathUtil.clamp(speed, -type.maxSpeed * 0.4f, type.maxSpeed);
                yaw += steerInput * 60f * DT;
            }
            speed *= 0.98f;
        } else {
            vy -= GRAVITY * DT;
            speed *= 0.9f;
        }
        double yawRad = Math.toRadians(yaw);
        vx = -Math.sin(yawRad) * speed;
        vz = Math.cos(yawRad) * speed;
    }

    private void tickMinecart(World world) {
        int bx = MathUtil.floor(x), by = MathUtil.floor(y + 0.1), bz = MathUtil.floor(z);
        int below = world.getBlockId(bx, by, bz);
        boolean onRail = below == Blocks.RAIL.id || below == Blocks.POWERED_RAIL.id;
        if (!onRail) {
            below = world.getBlockId(bx, by - 1, bz);
            onRail = below == Blocks.RAIL.id || below == Blocks.POWERED_RAIL.id;
        }
        if (onRail) {
            // Align to the dominant axis of the rail line.
            boolean xAxis = world.getBlockId(bx + 1, by, bz) == below || world.getBlockId(bx - 1, by, bz) == below
                || world.getBlockId(bx + 1, by - 1, bz) == below || world.getBlockId(bx - 1, by - 1, bz) == below;
            if (mounted && Math.abs(throttleInput) > 0.05f && Math.abs(speed) < 4) {
                speed += throttleInput * 3f * DT;
            }
            if (below == Blocks.POWERED_RAIL.id) {
                speed = MathUtil.clamp(speed * 1.06f + Math.signum(speed) * 2f * DT, -type.maxSpeed, type.maxSpeed);
            } else {
                speed *= 0.995f;
            }
            if (xAxis) {
                vx = speed; vz = 0;
                z = bz + 0.5;
                yaw = speed >= 0 ? -90 : 90;
            } else {
                vz = speed; vx = 0;
                x = bx + 0.5;
                yaw = speed >= 0 ? 0 : 180;
            }
        } else {
            speed *= 0.9f;
            vx *= 0.9; vz *= 0.9;
        }
        vy -= GRAVITY * DT;
    }

    /** Seat position for the mounted player. */
    public double seatY() { return y + height * 0.4; }

    public void damage(float amount) {
        health -= amount;
        if (health <= 0) dead = true;
    }

    @Override
    protected void saveExtra(Map<String, Object> m) {
        m.put("vehicleType", type.name());
        m.put("health", (double) health);
        m.put("fuel", (double) fuel);
    }

    @Override
    protected void loadExtra(Map<String, Object> m) {
        try {
            setType(VehicleType.valueOf(MiniJson.getString(m, "vehicleType", "BOAT")));
        } catch (IllegalArgumentException e) {
            setType(VehicleType.BOAT);
        }
        health = MiniJson.getFloat(m, "health", type.maxHealth);
        fuel = MiniJson.getFloat(m, "fuel", 0);
    }

    public static Vehicle create(VehicleType type) {
        return new Vehicle(type);
    }
}
