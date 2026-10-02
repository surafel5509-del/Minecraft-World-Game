package com.craftworld3d.core.crafting;

import com.craftworld3d.core.item.ItemStack;

import java.util.Arrays;

/** Order-independent recipe: the grid must contain exactly these ingredients. */
public final class ShapelessRecipe implements Recipe {
    private final int[] ingredients; // sorted item ids
    private final ItemStack result;

    public ShapelessRecipe(int[] ingredients, ItemStack result) {
        this.ingredients = ingredients.clone();
        Arrays.sort(this.ingredients);
        this.result = result;
    }

    @Override
    public boolean matches(int[] grid, int size) {
        int n = 0;
        int[] present = new int[grid.length];
        for (int id : grid) {
            if (id != 0) present[n++] = id;
        }
        if (n != ingredients.length) return false;
        int[] sorted = Arrays.copyOf(present, n);
        Arrays.sort(sorted);
        return Arrays.equals(sorted, ingredients);
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }
}
