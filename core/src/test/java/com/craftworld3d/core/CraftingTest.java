package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.crafting.Recipe;
import com.craftworld3d.core.crafting.RecipeRegistry;
import com.craftworld3d.core.crafting.Recipes;
import com.craftworld3d.core.item.ItemIds;
import com.craftworld3d.core.item.ItemStack;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CraftingTest {

    @Before
    public void setUp() {
        Recipes.init();
    }

    private static int[] grid3(int... ids) {
        assertEquals(9, ids.length);
        return ids;
    }

    @Test
    public void registryHasManyRecipes() {
        assertTrue(RecipeRegistry.recipeCount() >= 50);
    }

    @Test
    public void logsToPlanksShapeless() {
        int[] grid = {Blocks.OAK_LOG.id, 0, 0, 0};
        Recipe r = RecipeRegistry.match(grid, 2);
        assertNotNull(r);
        assertEquals(Blocks.OAK_PLANKS.id, r.result().itemId);
        assertEquals(4, r.result().count);
        // Position independent.
        int[] grid2 = {0, 0, 0, Blocks.OAK_LOG.id};
        assertNotNull(RecipeRegistry.match(grid2, 2));
    }

    @Test
    public void sticksShapedWorksAtAnyOffset() {
        int p = Blocks.OAK_PLANKS.id;
        // Column of two planks in a 2x2 grid, left column.
        assertNotNull(RecipeRegistry.match(new int[]{p, 0, p, 0}, 2));
        // Right column.
        assertNotNull(RecipeRegistry.match(new int[]{0, p, 0, p}, 2));
        // Row (wrong shape) must NOT match sticks.
        Recipe r = RecipeRegistry.match(new int[]{p, p, 0, 0}, 2);
        if (r != null) {
            assertNotEquals(ItemIds.STICK, r.result().itemId);
        }
    }

    @Test
    public void pickaxeShapedRecipe() {
        int c = Blocks.COBBLESTONE.id, s = ItemIds.STICK;
        Recipe r = RecipeRegistry.match(grid3(
            c, c, c,
            0, s, 0,
            0, s, 0), 3);
        assertNotNull(r);
        assertEquals(ItemIds.STONE_PICKAXE, r.result().itemId);
    }

    @Test
    public void swordVerticalRecipe() {
        int i = ItemIds.IRON_INGOT, s = ItemIds.STICK;
        Recipe r = RecipeRegistry.match(grid3(
            0, i, 0,
            0, i, 0,
            0, s, 0), 3);
        assertNotNull(r);
        assertEquals(ItemIds.IRON_SWORD, r.result().itemId);
    }

    @Test
    public void engineRecipeIronRedstoneFurnace() {
        Recipe r = RecipeRegistry.match(grid3(
            ItemIds.IRON_INGOT, ItemIds.IRON_INGOT, 0,
            ItemIds.REDSTONE_DUST, Blocks.FURNACE.id, 0,
            0, 0, 0), 3);
        assertNotNull(r);
        assertEquals(ItemIds.ENGINE, r.result().itemId);
    }

    @Test
    public void wheelRecipeRubberIron() {
        Recipe r = RecipeRegistry.match(grid3(
            ItemIds.RUBBER, ItemIds.RUBBER, ItemIds.IRON_INGOT,
            0, 0, 0,
            0, 0, 0), 3);
        assertNotNull(r);
        assertEquals(ItemIds.WHEEL, r.result().itemId);
    }

    @Test
    public void shapelessOrderIndependent() {
        Recipe a = RecipeRegistry.match(grid3(
            ItemIds.GUNPOWDER, Blocks.SAND.id, ItemIds.GUNPOWDER,
            Blocks.SAND.id, 0, 0,
            0, 0, 0), 3);
        Recipe b = RecipeRegistry.match(grid3(
            Blocks.SAND.id, ItemIds.GUNPOWDER, 0,
            ItemIds.GUNPOWDER, 0, 0,
            0, 0, Blocks.SAND.id), 3);
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(Blocks.TNT.id, a.result().itemId);
        assertEquals(Blocks.TNT.id, b.result().itemId);
    }

    @Test
    public void craftingTableFitsIn2x2ButFurnaceNeeds3x3() {
        int p = Blocks.OAK_PLANKS.id;
        assertNotNull(RecipeRegistry.match(new int[]{p, p, p, p}, 2));
        int c = Blocks.COBBLESTONE.id;
        Recipe furnace = RecipeRegistry.match(grid3(
            c, c, c,
            c, 0, c,
            c, c, c), 3);
        assertNotNull(furnace);
        assertEquals(Blocks.FURNACE.id, furnace.result().itemId);
    }

    @Test
    public void smeltingAndFuel() {
        ItemStack iron = RecipeRegistry.smeltResult(Blocks.IRON_ORE.id);
        assertNotNull(iron);
        assertEquals(ItemIds.IRON_INGOT, iron.itemId);
        ItemStack rubber = RecipeRegistry.smeltResult(Blocks.RUBBER_LOG.id);
        assertNotNull("rubber trees must yield rubber", rubber);
        assertEquals(ItemIds.RUBBER, rubber.itemId);
        assertTrue(RecipeRegistry.fuelTicks(ItemIds.COAL) > 0);
        assertEquals(0, RecipeRegistry.fuelTicks(ItemIds.DIAMOND));
        assertNull(RecipeRegistry.smeltResult(ItemIds.DIAMOND));
    }

    @Test
    public void emptyGridMatchesNothing() {
        assertNull(RecipeRegistry.match(new int[]{0, 0, 0, 0}, 2));
        assertNull(RecipeRegistry.match(new int[9], 3));
    }
}
