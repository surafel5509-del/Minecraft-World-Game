package com.craftworld3d.core.entity;

import com.craftworld3d.core.util.AABB;
import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.World;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base entity: position (feet, centered), velocity, orientation and
 * swept AABB collision against the voxel world. Tick rate is fixed 20/s.
 */
public abstract class Entity {
    public static final float DT = 1f / 20f;
    public static final float GRAVITY = 28f;      // blocks/s^2
    public static final float TERMINAL = 60f;

    public double x, y, z;
    public double vx, vy, vz;
    public float yaw, pitch;
    public float width = 0.6f, height = 1.8f;

    public boolean onGround;
    public boolean inWater;
    public boolean collidedHorizontally;
    public boolean dead;

    /** Distance fallen while airborne, for fall damage. */
    public float fallDistance;

    private final AABB box = new AABB();
    private final AABB sweep = new AABB();

    public abstract String typeKey();

    public abstract void tick(World world);

    public void setPosition(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
    }

    public AABB boundingBox() {
        double hw = width / 2.0;
        return box.set(x - hw, y, z - hw, x + hw, y + height, z + hw);
    }

    /** Moves with voxel collision; updates onGround / collided flags and fall distance. */
    public void move(World world, double dx, double dy, double dz) {
        AABB bb = boundingBox();
        sweep.set(bb).expand(dx, dy, dz);
        List<AABB> boxes = world.getCollisions(sweep);

        // Each obstacle box clips the movement of the entity's box, axis by axis.
        double origDy = dy, origDx = dx, origDz = dz;
        for (int i = 0; i < boxes.size(); i++) dy = boxes.get(i).clipY(bb, dy);
        bb.move(0, dy, 0);
        for (int i = 0; i < boxes.size(); i++) dx = boxes.get(i).clipX(bb, dx);
        bb.move(dx, 0, 0);
        for (int i = 0; i < boxes.size(); i++) dz = boxes.get(i).clipZ(bb, dz);
        bb.move(0, 0, dz);

        onGround = origDy < 0 && dy != origDy;
        collidedHorizontally = dx != origDx || dz != origDz;
        if (dx != origDx) vx = 0;
        if (dz != origDz) vz = 0;
        if (dy != origDy) vy = 0;

        x = (bb.minX + bb.maxX) / 2.0;
        y = bb.minY;
        z = (bb.minZ + bb.maxZ) / 2.0;

        // Accumulate fall distance; consumers (applyFallDamage) reset it on landing.
        if (origDy < 0) {
            fallDistance -= (float) origDy;
        }
        inWater = world.isLiquidAt(x, y + height * 0.5, z);
    }

    public double distSqTo(Entity o) {
        double dx = o.x - x, dy = o.y - y, dz = o.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distSqTo(double px, double py, double pz) {
        double dx = px - x, dy = py - y, dz = pz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    // ---------------- Persistence ----------------

    public Map<String, Object> save() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", typeKey());
        m.put("x", x); m.put("y", y); m.put("z", z);
        m.put("vx", vx); m.put("vy", vy); m.put("vz", vz);
        m.put("yaw", (double) yaw); m.put("pitch", (double) pitch);
        saveExtra(m);
        return m;
    }

    public void load(Map<String, Object> m) {
        x = MiniJson.getDouble(m, "x", 0);
        y = MiniJson.getDouble(m, "y", 80);
        z = MiniJson.getDouble(m, "z", 0);
        vx = MiniJson.getDouble(m, "vx", 0);
        vy = MiniJson.getDouble(m, "vy", 0);
        vz = MiniJson.getDouble(m, "vz", 0);
        yaw = MiniJson.getFloat(m, "yaw", 0);
        pitch = MiniJson.getFloat(m, "pitch", 0);
        loadExtra(m);
    }

    protected void saveExtra(Map<String, Object> m) {}

    protected void loadExtra(Map<String, Object> m) {}
}
