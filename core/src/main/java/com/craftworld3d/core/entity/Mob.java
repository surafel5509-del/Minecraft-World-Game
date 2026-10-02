package com.craftworld3d.core.entity;

import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.World;

import java.util.Map;
import java.util.Random;

/**
 * Mob with a compact state-machine AI: wander, chase, attack, flee.
 * AI only runs when inside the world's activation range (handled by World.tick).
 */
public class Mob extends LivingEntity {
    public MobType type;

    protected int aiTimer;
    protected double wanderX, wanderZ;
    protected boolean wandering;
    protected int attackCooldown;
    /** Creeper fuse ticks; -1 = not fusing. */
    public int fuse = -1;
    /** Lifetime for despawn checks. */
    public int ageTicks;
    /** True if spawned naturally (may despawn), false for persistent mobs. */
    public boolean naturalSpawn = true;

    public Mob(MobType type) {
        setType(type);
    }

    protected void setType(MobType type) {
        this.type = type;
        this.maxHealth = type.maxHealth;
        this.health = type.maxHealth;
        this.width = type.width;
        this.height = type.height;
    }

    @Override
    public String typeKey() { return "mob:" + type.name(); }

    @Override
    public void tick(World world) {
        super.tick(world);
        if (dead) return;
        ageTicks++;
        if (attackCooldown > 0) attackCooldown--;

        Player player = world.player;
        double playerDistSq = player == null ? Double.MAX_VALUE : distSqTo(player);

        // Despawn far natural mobs.
        if (naturalSpawn && playerDistSq > 96 * 96) {
            dead = true;
            return;
        }

        boolean chasing = false;
        if (type.hostile && player != null && !player.dead
            && playerDistSq < type.detectRange * type.detectRange) {
            chasing = true;
            chaseAndAttack(world, player, playerDistSq);
        }

        if (!chasing) {
            fuse = -1;
            wanderAI(world);
        }

        // Physics.
        if (inWater) {
            vy = Math.min(vy + 10f * DT, 2.0);
            fallDistance = 0;
        } else {
            vy -= GRAVITY * DT;
            if (vy < -TERMINAL) vy = -TERMINAL;
        }
        // Auto-jump over obstacles.
        if (collidedHorizontally && onGround) {
            vy = type == MobType.SPIDER ? 5.4f : 7.6f;
        }
        move(world, vx * DT, vy * DT, vz * DT);
        applyFallDamage(world);

        // Ground friction.
        if (onGround) {
            vx *= 0.6;
            vz *= 0.6;
        }
    }

    protected void chaseAndAttack(World world, Player player, double playerDistSq) {
        double dx = player.x - x, dz = player.z - z;
        double dist = Math.sqrt(playerDistSq);
        yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        if (type == MobType.CREEPER) {
            if (dist < 2.8) {
                if (fuse < 0) fuse = 30; // 1.5 s
                fuse--;
                if (fuse <= 0) {
                    explode(world, 2.6f);
                    dead = true;
                }
                vx *= 0.5; vz *= 0.5;
                return;
            }
            fuse = -1;
        }

        if (type == MobType.SKELETON) {
            if (dist < 12 && attackCooldown <= 0) {
                attackCooldown = 45;
                Arrow arrow = new Arrow();
                arrow.setPosition(x, y + height * 0.8, z);
                double len = Math.max(dist, 0.01);
                arrow.vx = dx / len * 22;
                arrow.vy = (player.y + 1 - (y + height * 0.8)) / len * 22 + 2.5;
                arrow.vz = dz / len * 22;
                arrow.fromMob = true;
                world.addEntity(arrow);
            }
            // Keep distance.
            if (dist > 10) approach(dx, dz, dist);
            else if (dist < 6) approach(-dx, -dz, dist);
            else { vx *= 0.6; vz *= 0.6; }
            return;
        }

        approach(dx, dz, dist);
        if (dist < 1.8 && attackCooldown <= 0 && type.damage > 0) {
            attackCooldown = 25;
            player.hurt(type.damage, x, z);
        }
    }

    private void approach(double dx, double dz, double dist) {
        if (dist < 0.01) return;
        vx = dx / dist * type.speed;
        vz = dz / dist * type.speed;
    }

    protected void wanderAI(World world) {
        Random rnd = world.random();
        if (aiTimer-- <= 0) {
            aiTimer = 40 + rnd.nextInt(80);
            if (rnd.nextInt(3) == 0) {
                wandering = true;
                double angle = rnd.nextDouble() * Math.PI * 2;
                double r = 4 + rnd.nextDouble() * 8;
                wanderX = x + Math.cos(angle) * r;
                wanderZ = z + Math.sin(angle) * r;
            } else {
                wandering = false;
            }
        }
        if (wandering) {
            double dx = wanderX - x, dz = wanderZ - z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.8) {
                wandering = false;
                vx *= 0.5; vz *= 0.5;
            } else {
                float walkSpeed = type.speed * 0.5f;
                vx = dx / dist * walkSpeed;
                vz = dz / dist * walkSpeed;
                yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            }
        } else {
            vx *= 0.8;
            vz *= 0.8;
        }
    }

    /** Creeper-style explosion: damages the player and weak blocks around. */
    protected void explode(World world, float radius) {
        Player player = world.player;
        if (player != null) {
            double d = Math.sqrt(distSqTo(player));
            if (d < radius * 2.5) {
                player.hurt((float) Math.max(1, 12 * (1 - d / (radius * 2.5))), x, z);
            }
        }
        int r = (int) Math.ceil(radius);
        int cx = (int) Math.floor(x), cy = (int) Math.floor(y + height / 2), cz = (int) Math.floor(z);
        for (int bx = cx - r; bx <= cx + r; bx++) {
            for (int by = cy - r; by <= cy + r; by++) {
                for (int bz = cz - r; bz <= cz + r; bz++) {
                    double dd = (bx - x + 0.5) * (bx - x + 0.5) + (by - cy) * (by - cy) + (bz - z + 0.5) * (bz - z + 0.5);
                    if (dd > radius * radius) continue;
                    var block = world.getBlock(bx, by, bz);
                    if (!block.isAir() && !block.liquid && block.resistance < 30 && !block.isUnbreakable()) {
                        world.setBlock(bx, by, bz, 0);
                    }
                }
            }
        }
        world.explosionHappened = true;
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        deathDropsPending = true;
    }

    /** Set when the mob dies; the engine spawns drops and plays sounds, then clears it. */
    public boolean deathDropsPending;

    /** Rolls this mob's drops. */
    public ItemStack rollDrops(Random rnd) {
        if (type.dropItem <= 0 || type.dropMax <= 0) return null;
        int n = type.dropMin + (type.dropMax > type.dropMin ? rnd.nextInt(type.dropMax - type.dropMin + 1) : 0);
        if (n <= 0) return null;
        return new ItemStack(type.dropItem, n);
    }

    @Override
    protected void saveExtra(Map<String, Object> m) {
        super.saveExtra(m);
        m.put("mobType", type.name());
        m.put("natural", naturalSpawn);
    }

    @Override
    protected void loadExtra(Map<String, Object> m) {
        super.loadExtra(m);
        try {
            setType(MobType.valueOf(MiniJson.getString(m, "mobType", "PIG")));
        } catch (IllegalArgumentException e) {
            setType(MobType.PIG);
        }
        health = MiniJson.getFloat(m, "health", maxHealth);
        naturalSpawn = MiniJson.getBool(m, "natural", true);
    }
}
