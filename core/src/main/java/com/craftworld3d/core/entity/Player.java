package com.craftworld3d.core.entity;

import com.craftworld3d.core.inventory.Inventory;
import com.craftworld3d.core.item.FoodItem;
import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.util.MathUtil;
import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.World;
import com.craftworld3d.core.world.blockentity.ChestEntity;

import java.util.Map;

/**
 * First-person player: walking, sprinting, crouching, jumping, swimming,
 * ladder climbing, fall damage, hunger and stamina.
 * Movement input is set by the UI layer each frame.
 */
public final class Player extends LivingEntity {
    public static final int GAMEMODE_SURVIVAL = 0;
    public static final int GAMEMODE_CREATIVE = 1;

    public static final float WALK_SPEED = 4.3f;
    public static final float SPRINT_SPEED = 5.8f;
    public static final float CROUCH_SPEED = 1.4f;
    public static final float SWIM_SPEED = 2.2f;
    public static final float JUMP_VELOCITY = 8.6f;
    public static final float EYE_HEIGHT = 1.62f;
    public static final float EYE_HEIGHT_CROUCH = 1.27f;

    public final Inventory inventory = new Inventory();

    public float hunger = 20;
    public float stamina = 20;
    public int gamemode = GAMEMODE_SURVIVAL;
    public int dimension = World.DIM_OVERWORLD;

    // Input state (set by touch controls each tick).
    public float inputForward, inputStrafe;  // -1..1
    public boolean wantJump, wantSprint, wantCrouch;

    public boolean sprinting, crouching;
    private float exhaustion;
    private int regenTimer;
    private int portalCooldown;
    public double distanceTraveled;

    /** Set for one tick when the player dies (engine shows death screen). */
    public boolean justDied;

    public Player() {
        width = 0.6f;
        height = 1.8f;
    }

    @Override
    public String typeKey() { return "player"; }

    public float eyeHeight() {
        return crouching ? EYE_HEIGHT_CROUCH : EYE_HEIGHT;
    }

    public double eyeY() { return y + eyeHeight(); }

    @Override
    public void tick(World world) {
        super.tick(world);
        justDied = false;
        if (portalCooldown > 0) portalCooldown--;
        if (dead) return;

        boolean climbing = world.isClimbableAt(x, y + 0.5, z);
        crouching = wantCrouch && !inWater;
        sprinting = wantSprint && !crouching && inputForward > 0.1f && stamina > 1 && !inWater;

        // Stamina.
        if (sprinting) {
            stamina = Math.max(0, stamina - 2.5f * DT);
            exhaustion += 0.08f * DT;
            if (stamina <= 0) sprinting = false;
        } else {
            stamina = Math.min(20, stamina + 1.5f * DT);
        }

        float speed = inWater ? SWIM_SPEED
            : sprinting ? SPRINT_SPEED
            : crouching ? CROUCH_SPEED
            : WALK_SPEED;

        // Direction from yaw.
        double yawRad = Math.toRadians(yaw);
        double sin = Math.sin(yawRad), cos = Math.cos(yawRad);
        double ax = (inputStrafe * cos - inputForward * sin) * speed;
        double az = (inputForward * cos + inputStrafe * sin) * speed;

        // Horizontal velocity: responsive ground control, drifting in air.
        float control = onGround || inWater || climbing ? 0.6f : 0.08f;
        vx += (ax - vx) * control;
        vz += (az - vz) * control;

        // Vertical.
        if (inWater) {
            vy -= GRAVITY * 0.18f * DT;
            vy *= 0.9;
            if (wantJump) vy = Math.min(vy + 16f * DT, 3.2);
            fallDistance = 0;
        } else if (climbing) {
            vy = wantJump ? 2.4 : (inputForward != 0 || inputStrafe != 0 ? 1.6 : (crouching ? 0 : -1.2));
            fallDistance = 0;
        } else {
            vy -= GRAVITY * DT;
            if (vy < -TERMINAL) vy = -TERMINAL;
            if (wantJump && onGround) {
                vy = JUMP_VELOCITY;
                exhaustion += 0.05f;
            }
        }

        double px = x, pz = z;
        move(world, vx * DT, vy * DT, vz * DT);
        distanceTraveled += Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));

        if (gamemode == GAMEMODE_SURVIVAL) {
            applyFallDamage(world);
            tickHunger();
            tickEnvironment(world);
        } else {
            fallDistance = 0;
        }

        // Pressure plates.
        int bx = MathUtil.floor(x), by = MathUtil.floor(y), bz = MathUtil.floor(z);
        world.redstone.standOnPlate(world, bx, by, bz);
    }

    private void tickHunger() {
        exhaustion += 0.004f * DT * (Math.abs(inputForward) + Math.abs(inputStrafe) > 0 ? 1 : 0.2f);
        if (exhaustion > 4f) {
            exhaustion = 0;
            hunger = Math.max(0, hunger - 1);
        }
        if (hunger >= 18 && health < maxHealth) {
            if (++regenTimer >= 80) {
                regenTimer = 0;
                health = Math.min(maxHealth, health + 1);
                exhaustion += 1.5f;
            }
        } else if (hunger <= 0) {
            if (++regenTimer >= 80) {
                regenTimer = 0;
                health -= 1;
                justHurt = true;
                if (health <= 0) { health = 0; onDeath(); }
            }
        } else {
            regenTimer = 0;
        }
    }

    private void tickEnvironment(World world) {
        // Lava damage.
        if (world.getBlock(MathUtil.floor(x), MathUtil.floor(y), MathUtil.floor(z)).name.equals("lava")) {
            if (hurtTicks <= 0) {
                hurt(4, x + 0.1, z);
            }
        }
        // Drowning.
        if (world.isLiquidAt(x, eyeY(), z)) {
            if (++regenTimer > 300) { // ~15 s of air
                regenTimer = 0;
                hurt(2, x + 0.1, z);
            }
        }
    }

    /** Eats the selected item if it is food. Returns true when consumed. */
    public boolean eatSelected() {
        ItemStack sel = inventory.selected();
        if (sel == null || !(sel.item() instanceof FoodItem food)) return false;
        if (hunger >= 20) return false;
        hunger = Math.min(20, hunger + food.foodValue);
        inventory.shrink(inventory.selectedSlot);
        return true;
    }

    public boolean canEnterPortal() { return portalCooldown <= 0; }

    public void notePortalUsed() { portalCooldown = 80; }

    @Override
    protected void onDeath() {
        dead = true;
        justDied = true;
    }

    /** Respawn at the given location with reset vitals (inventory is kept). */
    public void respawn(double sx, double sy, double sz) {
        dead = false;
        health = maxHealth;
        hunger = 20;
        stamina = 20;
        fallDistance = 0;
        vx = vy = vz = 0;
        setPosition(sx, sy, sz);
    }

    @Override
    protected void saveExtra(Map<String, Object> m) {
        super.saveExtra(m);
        m.put("hunger", (double) hunger);
        m.put("stamina", (double) stamina);
        m.put("gamemode", gamemode);
        m.put("dimension", dimension);
        m.put("selected", inventory.selectedSlot);
        m.put("distance", distanceTraveled);
        m.put("inventory", ChestEntity.saveInventory(inventory));
    }

    @Override
    protected void loadExtra(Map<String, Object> m) {
        super.loadExtra(m);
        hunger = MiniJson.getFloat(m, "hunger", 20);
        stamina = MiniJson.getFloat(m, "stamina", 20);
        gamemode = MiniJson.getInt(m, "gamemode", GAMEMODE_SURVIVAL);
        dimension = MiniJson.getInt(m, "dimension", World.DIM_OVERWORLD);
        inventory.selectedSlot = MathUtil.clamp(MiniJson.getInt(m, "selected", 0), 0, 8);
        distanceTraveled = MiniJson.getDouble(m, "distance", 0);
        ChestEntity.loadInventory(inventory, m.get("inventory"));
    }
}
