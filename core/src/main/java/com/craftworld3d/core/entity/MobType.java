package com.craftworld3d.core.entity;

import com.craftworld3d.core.item.ItemIds;

/** Data-driven mob stats and drops. */
public enum MobType {
    //            hostile hp  speed dmg  w     h     detect  drops (item, min, max)
    PIG(          false,  10, 1.8f, 0,   0.9f, 0.9f, 0,      ItemIds.PORKCHOP_RAW, 1, 3),
    COW(          false,  10, 1.6f, 0,   0.9f, 1.4f, 0,      ItemIds.BEEF_RAW, 1, 3),
    SHEEP(        false,  8,  1.6f, 0,   0.9f, 1.3f, 0,      ItemIds.MUTTON_RAW, 1, 2),
    CHICKEN(      false,  4,  1.4f, 0,   0.4f, 0.7f, 0,      ItemIds.CHICKEN_RAW, 1, 1),
    ZOMBIE(       true,   20, 2.2f, 3,   0.6f, 1.9f, 16,     ItemIds.ROTTEN_FLESH, 0, 2),
    SKELETON(     true,   20, 2.0f, 2,   0.6f, 1.9f, 16,     ItemIds.BONE, 0, 2),
    CREEPER(      true,   20, 2.4f, 0,   0.6f, 1.7f, 14,     ItemIds.GUNPOWDER, 0, 2),
    SPIDER(       true,   16, 2.8f, 2,   1.4f, 0.9f, 14,     ItemIds.STRING, 0, 2),
    DEPTH_FIEND(  true,   24, 2.6f, 4,   0.7f, 1.9f, 18,     ItemIds.NETHER_CRYSTAL, 0, 1),
    VILLAGER(     false,  20, 1.6f, 0,   0.6f, 1.9f, 0,      0, 0, 0);

    public final boolean hostile;
    public final float maxHealth;
    public final float speed;
    public final int damage;
    public final float width, height;
    public final float detectRange;
    public final int dropItem, dropMin, dropMax;

    MobType(boolean hostile, float hp, float speed, int damage, float w, float h,
            float detect, int dropItem, int dropMin, int dropMax) {
        this.hostile = hostile;
        this.maxHealth = hp;
        this.speed = speed;
        this.damage = damage;
        this.width = w;
        this.height = h;
        this.detectRange = detect;
        this.dropItem = dropItem;
        this.dropMin = dropMin;
        this.dropMax = dropMax;
    }
}
