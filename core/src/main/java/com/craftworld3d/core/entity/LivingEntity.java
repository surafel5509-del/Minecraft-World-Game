package com.craftworld3d.core.entity;

import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.World;

import java.util.Map;

/** Entity with health, damage, knockback and fall damage. */
public abstract class LivingEntity extends Entity {
    public float health = 20;
    public float maxHealth = 20;
    /** Invulnerability frames after a hit (ticks). */
    public int hurtTicks;
    /** Set for one tick when damaged (renderer flash / sound hooks). */
    public boolean justHurt;

    @Override
    public void tick(World world) {
        justHurt = false;
        if (hurtTicks > 0) hurtTicks--;
    }

    /** Applies damage with knockback away from (fromX, fromZ). Returns true if it connected. */
    public boolean hurt(float amount, double fromX, double fromZ) {
        if (hurtTicks > 0 || dead) return false;
        health -= amount;
        hurtTicks = 10;
        justHurt = true;
        double dx = x - fromX, dz = z - fromZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len > 0.01) {
            vx += dx / len * 6.0;
            vz += dz / len * 6.0;
            vy = Math.max(vy, 5.0);
        }
        if (health <= 0) {
            health = 0;
            onDeath();
        }
        return true;
    }

    protected void onDeath() {
        dead = true;
    }

    /** Standard fall damage: 1 heart per block over 3. Resets fall distance on landing. */
    protected void applyFallDamage(World world) {
        if (!onGround) return;
        float dist = fallDistance;
        fallDistance = 0;
        if (dist > 3.5f && !inWater) {
            float dmg = dist - 3.0f;
            if (dmg > 0.5f) {
                health -= dmg;
                justHurt = true;
                hurtTicks = 10;
                if (health <= 0) {
                    health = 0;
                    onDeath();
                }
            }
        }
    }

    @Override
    protected void saveExtra(Map<String, Object> m) {
        m.put("health", (double) health);
    }

    @Override
    protected void loadExtra(Map<String, Object> m) {
        health = MiniJson.getFloat(m, "health", maxHealth);
    }
}
