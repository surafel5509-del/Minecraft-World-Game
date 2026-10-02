package com.craftworld3d.core.crafting;

import com.craftworld3d.core.item.ItemStack;

/**
 * Position-sensitive recipe. The pattern may be placed anywhere inside the
 * grid (translation invariant), mirroring is not applied.
 */
public final class ShapedRecipe implements Recipe {
    private final int width, height;
    private final int[] pattern; // item ids, 0 = empty
    private final ItemStack result;

    public ShapedRecipe(int width, int height, int[] pattern, ItemStack result) {
        if (pattern.length != width * height)
            throw new IllegalArgumentException("Pattern size mismatch");
        this.width = width;
        this.height = height;
        this.pattern = pattern.clone();
        this.result = result;
    }

    @Override
    public boolean matches(int[] grid, int size) {
        if (width > size || height > size) return false;
        for (int offY = 0; offY <= size - height; offY++) {
            for (int offX = 0; offX <= size - width; offX++) {
                if (matchesAt(grid, size, offX, offY)) return true;
            }
        }
        return false;
    }

    private boolean matchesAt(int[] grid, int size, int offX, int offY) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int expected = 0;
                int px = x - offX, py = y - offY;
                if (px >= 0 && px < width && py >= 0 && py < height) {
                    expected = pattern[py * width + px];
                }
                if (grid[y * size + x] != expected) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }
}
