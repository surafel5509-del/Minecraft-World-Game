package com.craftworld3d.core.entity;

import com.craftworld3d.core.entity.vehicle.Vehicle;
import com.craftworld3d.core.entity.vehicle.VehicleType;
import com.craftworld3d.core.util.MiniJson;

import java.util.Map;

/** Creates entities from their saved type keys. */
public final class EntityFactory {
    private EntityFactory() {}

    /** Returns a loaded entity or null for unknown/player types. */
    public static Entity fromSave(Map<String, Object> data) {
        String type = MiniJson.getString(data, "type", "");
        Entity e = null;
        if (type.startsWith("mob:")) {
            e = new Mob(MobType.PIG);
        } else if (type.equals("villager")) {
            e = new Villager();
        } else if (type.startsWith("vehicle:")) {
            e = new Vehicle(VehicleType.BOAT);
        } else if (type.equals("item")) {
            e = new DroppedItem();
        } else if (type.equals("arrow")) {
            e = new Arrow();
        }
        if (e != null) {
            e.load(data);
            if (e.dead) return null;
        }
        return e;
    }
}
