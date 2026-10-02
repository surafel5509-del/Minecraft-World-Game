package com.craftworld3d.core.item;

/** Edible item restoring hunger points. */
public final class FoodItem extends Item {
    public final int foodValue;

    public FoodItem(int id, String name, String icon, int foodValue) {
        super(id, name, icon, 64, -1);
        this.foodValue = foodValue;
    }
}
