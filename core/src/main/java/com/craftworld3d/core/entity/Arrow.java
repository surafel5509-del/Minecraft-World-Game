package com.craftworld3d.core.entity;

import com.craftworld3d.core.world.World;

/** Simple projectile used by skeletons (and later, bows). */
public final class Arrow extends Entity {
    public boolean fromMob;
    private int life;

    public Arrow() {
        width = 0.2f;
        height = 0.2f;
    }

    @Override
    public String typeKey() { return "arrow"; }

    @Override
    public void tick(World world) {
        if (++life > 200) { dead = true; return; }
        vy -= GRAVITY * 0.6f * DT;
        move(world, vx * DT, vy * DT, vz * DT);
        if (onGround || collidedHorizontally) {
            dead = true;
            return;
        }
        Player p = world.player;
        if (fromMob && p != null && !p.dead && distSqTo(p.x, p.y + 0.9, p.z) < 0.8) {
            p.hurt(3, x, z);
            dead = true;
        }
    }
}
