package com.craftworld3d.core.entity.vehicle;

import com.craftworld3d.core.item.ItemIds;

/** Data-driven vehicle stats. Fuel unit = 1 coal; capacity in units. */
public enum VehicleType {
    //            kind              item                  maxSpd accel  fuel  hp   w     h
    SEDAN(        Kind.CAR,         ItemIds.CAR_SEDAN,    14f,   6f,    40,   40,  1.6f, 1.4f),
    SUV(          Kind.CAR,         ItemIds.CAR_SUV,      12f,   5f,    50,   60,  1.8f, 1.7f),
    TRUCK(        Kind.CAR,         ItemIds.CAR_TRUCK,    10f,   4f,    80,   80,  2.0f, 2.0f),
    SPORTS_CAR(   Kind.CAR,         ItemIds.CAR_SPORTS,   22f,   9f,    35,   30,  1.6f, 1.1f),
    JEEP(         Kind.CAR,         ItemIds.CAR_JEEP,     13f,   6f,    45,   50,  1.8f, 1.6f),
    SMALL_PLANE(  Kind.PLANE,       ItemIds.PLANE_SMALL,  26f,   5f,    80,   40,  2.2f, 1.6f),
    JET(          Kind.PLANE,       ItemIds.PLANE_JET,    48f,   9f,    120,  50,  2.4f, 1.6f),
    HELICOPTER(   Kind.HELICOPTER,  ItemIds.HELICOPTER,   18f,   6f,    90,   40,  2.2f, 2.0f),
    BIPLANE(      Kind.PLANE,       ItemIds.BIPLANE,      20f,   4f,    60,   30,  2.2f, 1.8f),
    BOAT(         Kind.BOAT,        ItemIds.BOAT,         8f,    4f,    0,    10,  1.4f, 0.6f),
    MINECART(     Kind.MINECART,    ItemIds.MINECART,     12f,   0f,    0,    10,  1.0f, 0.7f);

    public enum Kind { CAR, PLANE, HELICOPTER, BOAT, MINECART }

    public final Kind kind;
    public final int itemId;
    public final float maxSpeed;
    public final float accel;
    public final int fuelCapacity;
    public final float maxHealth;
    public final float width, height;

    VehicleType(Kind kind, int itemId, float maxSpeed, float accel, int fuelCapacity,
                float maxHealth, float w, float h) {
        this.kind = kind;
        this.itemId = itemId;
        this.maxSpeed = maxSpeed;
        this.accel = accel;
        this.fuelCapacity = fuelCapacity;
        this.maxHealth = maxHealth;
        this.width = w;
        this.height = h;
    }

    public boolean needsFuel() { return fuelCapacity > 0; }

    public static VehicleType byItem(int itemId) {
        for (VehicleType t : values()) if (t.itemId == itemId) return t;
        return null;
    }
}
