package com.craftworld3d.core.entity;

import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.World;

import java.util.Map;

/** An item stack lying in the world; floats to the player for pickup. */
public final class DroppedItem extends Entity {
    public ItemStack stack;
    public int age;
    /** Short delay so freshly dropped items aren't instantly re-collected. */
    public int pickupDelay = 20;

    public DroppedItem() {
        width = 0.3f;
        height = 0.3f;
    }

    public DroppedItem(ItemStack stack, double x, double y, double z) {
        this();
        this.stack = stack;
        setPosition(x, y, z);
    }

    @Override
    public String typeKey() { return "item"; }

    @Override
    public void tick(World world) {
        if (stack == null || stack.isEmpty()) { dead = true; return; }
        if (++age > 20 * 60 * 5) { dead = true; return; } // 5 min lifetime
        if (pickupDelay > 0) pickupDelay--;

        if (inWater) vy = Math.min(vy + 8f * DT, 1.2);
        else vy -= GRAVITY * 0.8f * DT;
        move(world, vx * DT, vy * DT, vz * DT);
        if (onGround) { vx *= 0.7; vz *= 0.7; }

        Player p = world.player;
        if (p != null && !p.dead && pickupDelay <= 0) {
            double dSq = distSqTo(p.x, p.y + 0.8, p.z);
            if (dSq < 2.2 * 2.2) {
                // Magnet toward player.
                double d = Math.sqrt(dSq);
                if (d > 0.5) {
                    vx += (p.x - x) / d * 2.2;
                    vy += (p.y + 0.8 - y) / d * 2.2;
                    vz += (p.z - z) / d * 2.2;
                }
                if (d < 0.9) {
                    int leftover = p.inventory.add(stack);
                    world.itemPickedUp = true;
                    if (leftover <= 0) {
                        dead = true;
                    } else {
                        stack.count = leftover;
                    }
                }
            }
        }
    }

    @Override
    protected void saveExtra(Map<String, Object> m) {
        if (stack != null && !stack.isEmpty()) {
            m.put("itemId", stack.itemId);
            m.put("n", stack.count);
            m.put("dmg", stack.damage);
        }
    }

    @Override
    protected void loadExtra(Map<String, Object> m) {
        int id = MiniJson.getInt(m, "itemId", 0);
        int n = MiniJson.getInt(m, "n", 0);
        if (id > 0 && n > 0) stack = new ItemStack(id, n, MiniJson.getInt(m, "dmg", 0));
        else dead = true;
    }
}
