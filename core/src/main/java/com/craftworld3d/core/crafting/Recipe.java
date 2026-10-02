package com.craftworld3d.core.crafting;

import com.craftworld3d.core.item.ItemStack;

/** A crafting recipe matched against a square grid of item ids (0 = empty). */
public interface Recipe {
    /** @param grid item ids, row-major; size*size entries. */
    boolean matches(int[] grid, int size);

    ItemStack result();
}
