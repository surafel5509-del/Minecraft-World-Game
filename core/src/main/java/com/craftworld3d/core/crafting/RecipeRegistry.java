package com.craftworld3d.core.crafting;

import com.craftworld3d.core.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Data-driven registry of crafting and smelting recipes. */
public final class RecipeRegistry {
    private static final List<Recipe> RECIPES = new ArrayList<>();
    private static final Map<Integer, ItemStack> SMELTING = new HashMap<>();
    private static final Map<Integer, Integer> FUEL_TICKS = new HashMap<>();

    private RecipeRegistry() {}

    public static void register(Recipe r) {
        RECIPES.add(r);
    }

    public static void registerSmelting(int inputItem, ItemStack output) {
        SMELTING.put(inputItem, output);
    }

    public static void registerFuel(int itemId, int burnTicks) {
        FUEL_TICKS.put(itemId, burnTicks);
    }

    /** Finds the first recipe matching the grid, or null. */
    public static Recipe match(int[] grid, int size) {
        for (Recipe r : RECIPES) {
            if (r.matches(grid, size)) return r;
        }
        return null;
    }

    public static ItemStack smeltResult(int inputItem) {
        ItemStack out = SMELTING.get(inputItem);
        return out == null ? null : out.copy();
    }

    public static int fuelTicks(int itemId) {
        Integer t = FUEL_TICKS.get(itemId);
        return t == null ? 0 : t;
    }

    public static int recipeCount() { return RECIPES.size(); }

    public static List<Recipe> all() { return RECIPES; }

    /** Test support: clears all registered recipes. */
    public static void clearAll() {
        RECIPES.clear();
        SMELTING.clear();
        FUEL_TICKS.clear();
    }
}
