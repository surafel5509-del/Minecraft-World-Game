package com.craftworld3d.core.world.blockentity;

import com.craftworld3d.core.world.World;

import java.util.Map;

/** Extra data attached to a block position (chest contents, furnace state...). */
public abstract class BlockEntity {
    public final int x, y, z;

    protected BlockEntity(int x, int y, int z) {
        this.x = x; this.y = y; this.z = z;
    }

    /** Type key used by the save system. */
    public abstract String type();

    public void tick(World world) {}

    public abstract Map<String, Object> saveData();

    public abstract void loadData(Map<String, Object> data);
}
