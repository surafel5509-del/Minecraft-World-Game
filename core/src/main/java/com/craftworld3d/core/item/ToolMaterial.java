package com.craftworld3d.core.item;

/** Tool tiers: mining level, speed multiplier, durability and base damage bonus. */
public enum ToolMaterial {
    WOOD(0, 2.0f, 60, 0),
    STONE(1, 4.0f, 132, 1),
    IRON(2, 6.0f, 251, 2),
    GOLD(0, 9.0f, 33, 0),
    DIAMOND(3, 8.0f, 1562, 3);

    public final int miningLevel;
    public final float speed;
    public final int durability;
    public final int damageBonus;

    ToolMaterial(int miningLevel, float speed, int durability, int damageBonus) {
        this.miningLevel = miningLevel;
        this.speed = speed;
        this.durability = durability;
        this.damageBonus = damageBonus;
    }
}
