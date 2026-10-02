package com.craftworld3d.core.item;

/** Base item definition. Block items are generated automatically from the block registry. */
public class Item {
    public final int id;
    public final String name;
    /** Icon tile name in the texture atlas. */
    public final String icon;
    public final int maxStack;
    /** Block placed by this item, or -1 if not placeable. */
    public final int placesBlock;

    public Item(int id, String name, String icon, int maxStack, int placesBlock) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.maxStack = maxStack;
        this.placesBlock = placesBlock;
    }

    public Item(int id, String name, String icon) {
        this(id, name, icon, 64, -1);
    }

    public boolean isTool() { return this instanceof ToolItem; }
    public boolean isFood() { return this instanceof FoodItem; }
    public boolean isPlaceable() { return placesBlock > 0; }

    /** Max durability; 0 = not damageable. */
    public int maxDurability() { return 0; }
}
