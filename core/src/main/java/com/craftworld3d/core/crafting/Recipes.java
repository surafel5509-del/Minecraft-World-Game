package com.craftworld3d.core.crafting;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.item.Items;

import static com.craftworld3d.core.item.ItemIds.*;

/** All built-in recipes (shaped, shapeless, smelting, fuels). */
public final class Recipes {
    private Recipes() {}

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        Items.init();

        int planks = Blocks.OAK_PLANKS.id;
        int cobble = Blocks.COBBLESTONE.id;

        // ---- Basic shapeless ----
        shapeless(stack(planks, 4), Blocks.OAK_LOG.id);
        shapeless(stack(Blocks.BIRCH_PLANKS.id, 4), Blocks.BIRCH_LOG.id);
        shapeless(stack(Blocks.SPRUCE_PLANKS.id, 4), Blocks.SPRUCE_LOG.id);
        shapeless(stack(planks, 4), Blocks.RUBBER_LOG.id);

        // ---- Shaped basics ----
        shaped(stack(STICK, 4), 1, 2, planks, planks);
        shaped(stack(Blocks.CRAFTING_TABLE.id, 1), 2, 2, planks, planks, planks, planks);
        shaped(stack(Blocks.TORCH.id, 4), 1, 2, COAL, STICK);
        shaped(stack(Blocks.FURNACE.id, 1), 3, 3,
            cobble, cobble, cobble, cobble, 0, cobble, cobble, cobble, cobble);
        shaped(stack(Blocks.CHEST.id, 1), 3, 3,
            planks, planks, planks, planks, 0, planks, planks, planks, planks);
        shaped(stack(Blocks.DOOR_CLOSED.id, 1), 2, 3,
            planks, planks, planks, planks, planks, planks);
        shaped(stack(Blocks.LADDER.id, 3), 3, 3,
            STICK, 0, STICK, STICK, STICK, STICK, STICK, 0, STICK);
        shaped(stack(Blocks.STONE_BRICKS.id, 4), 2, 2,
            Blocks.STONE.id, Blocks.STONE.id, Blocks.STONE.id, Blocks.STONE.id);
        shaped(stack(Blocks.SANDSTONE.id, 1), 2, 2,
            Blocks.SAND.id, Blocks.SAND.id, Blocks.SAND.id, Blocks.SAND.id);
        shaped(stack(Blocks.BRICKS.id, 1), 2, 2,
            Blocks.CLAY.id, Blocks.CLAY.id, Blocks.CLAY.id, Blocks.CLAY.id);
        shaped(stack(Blocks.BOOKSHELF.id, 1), 3, 2,
            planks, planks, planks, planks, planks, planks);
        shapeless(stack(Blocks.TNT.id, 1), GUNPOWDER, GUNPOWDER, Blocks.SAND.id, Blocks.SAND.id);
        shapeless(stack(BREAD, 1), WHEAT, WHEAT, WHEAT);
        shapeless(stack(Blocks.WOOL.id, 1), STRING, STRING, STRING, STRING);

        // ---- Material blocks (3x3 <-> 9) ----
        nineBlock(COAL, Blocks.COAL_BLOCK.id);
        nineBlock(IRON_INGOT, Blocks.IRON_BLOCK.id);
        nineBlock(GOLD_INGOT, Blocks.GOLD_BLOCK.id);
        nineBlock(COPPER_INGOT, Blocks.COPPER_BLOCK.id);
        nineBlock(REDSTONE_DUST, Blocks.REDSTONE_BLOCK.id);
        nineBlock(DIAMOND, Blocks.DIAMOND_BLOCK.id);
        nineBlock(EMERALD, Blocks.EMERALD_BLOCK.id);
        nineBlock(ALUMINUM_INGOT, Blocks.ALUMINUM_BLOCK.id);

        // ---- Tools (material-major) ----
        toolSet(WOODEN_PICKAXE, planks);
        toolSet(STONE_PICKAXE, cobble);
        toolSet(IRON_PICKAXE, IRON_INGOT);
        toolSet(GOLD_PICKAXE, GOLD_INGOT);
        toolSet(DIAMOND_PICKAXE, DIAMOND);

        // ---- Redstone ----
        shaped(stack(Blocks.REDSTONE_TORCH.id, 1), 1, 2, REDSTONE_DUST, STICK);
        shaped(stack(Blocks.LEVER.id, 1), 1, 2, STICK, cobble);
        shapeless(stack(Blocks.BUTTON.id, 1), Blocks.STONE.id);
        shaped(stack(Blocks.PRESSURE_PLATE.id, 1), 2, 1, planks, planks);
        shaped(stack(Blocks.REDSTONE_LAMP.id, 1), 3, 3,
            0, REDSTONE_DUST, 0, REDSTONE_DUST, Blocks.GLOWSTONE.id, REDSTONE_DUST, 0, REDSTONE_DUST, 0);
        shaped(stack(Blocks.REPEATER.id, 1), 3, 3,
            0, 0, 0, Blocks.REDSTONE_TORCH.id, REDSTONE_DUST, Blocks.REDSTONE_TORCH.id,
            Blocks.STONE.id, Blocks.STONE.id, Blocks.STONE.id);
        shaped(stack(Blocks.PISTON.id, 1), 3, 3,
            planks, planks, planks, cobble, IRON_INGOT, cobble, cobble, REDSTONE_DUST, cobble);

        // ---- Rails / transport ----
        shaped(stack(Blocks.RAIL.id, 16), 3, 3,
            IRON_INGOT, 0, IRON_INGOT, IRON_INGOT, STICK, IRON_INGOT, IRON_INGOT, 0, IRON_INGOT);
        shaped(stack(Blocks.POWERED_RAIL.id, 6), 3, 3,
            GOLD_INGOT, 0, GOLD_INGOT, GOLD_INGOT, STICK, GOLD_INGOT, GOLD_INGOT, REDSTONE_DUST, GOLD_INGOT);
        shaped(stack(MINECART, 1), 3, 2,
            IRON_INGOT, 0, IRON_INGOT, IRON_INGOT, IRON_INGOT, IRON_INGOT);
        shaped(stack(BOAT, 1), 3, 2,
            planks, 0, planks, planks, planks, planks);

        // ---- Vehicles ----
        // Engine = Iron + Redstone + Furnace (per design doc).
        shapeless(stack(ENGINE, 1), IRON_INGOT, IRON_INGOT, REDSTONE_DUST, Blocks.FURNACE.id);
        // Wheel = Rubber + Iron.
        shapeless(stack(WHEEL, 1), RUBBER, RUBBER, IRON_INGOT);
        shapeless(stack(PARACHUTE, 1), Blocks.WOOL.id, Blocks.WOOL.id, STRING, STRING);

        shaped(stack(CAR_SEDAN, 1), 3, 2,
            IRON_INGOT, IRON_INGOT, IRON_INGOT, WHEEL, ENGINE, WHEEL);
        shaped(stack(CAR_SUV, 1), 3, 2,
            IRON_INGOT, Blocks.IRON_BLOCK.id, IRON_INGOT, WHEEL, ENGINE, WHEEL);
        shaped(stack(CAR_TRUCK, 1), 3, 2,
            Blocks.IRON_BLOCK.id, Blocks.IRON_BLOCK.id, Blocks.CHEST.id, WHEEL, ENGINE, WHEEL);
        shaped(stack(CAR_SPORTS, 1), 3, 2,
            ALUMINUM_INGOT, ALUMINUM_INGOT, ALUMINUM_INGOT, WHEEL, ENGINE, WHEEL);
        shaped(stack(CAR_JEEP, 1), 3, 2,
            IRON_INGOT, ALUMINUM_INGOT, IRON_INGOT, WHEEL, ENGINE, WHEEL);
        shaped(stack(PLANE_SMALL, 1), 3, 3,
            0, ALUMINUM_INGOT, 0, ALUMINUM_INGOT, ENGINE, ALUMINUM_INGOT, 0, WHEEL, WHEEL);
        shaped(stack(PLANE_JET, 1), 3, 3,
            0, ALUMINUM_INGOT, 0, ENGINE, Blocks.ALUMINUM_BLOCK.id, ENGINE, 0, WHEEL, WHEEL);
        shaped(stack(HELICOPTER, 1), 3, 3,
            ALUMINUM_INGOT, ALUMINUM_INGOT, ALUMINUM_INGOT, 0, ENGINE, 0, 0, WHEEL, 0);
        shaped(stack(BIPLANE, 1), 3, 3,
            Blocks.OAK_PLANKS.id, Blocks.OAK_PLANKS.id, Blocks.OAK_PLANKS.id,
            ENGINE, Blocks.OAK_PLANKS.id, ENGINE, 0, WHEEL, WHEEL);

        // ---- Smelting ----
        smelt(Blocks.IRON_ORE.id, stack(IRON_INGOT, 1));
        smelt(Blocks.GOLD_ORE.id, stack(GOLD_INGOT, 1));
        smelt(Blocks.COPPER_ORE.id, stack(COPPER_INGOT, 1));
        smelt(Blocks.ALUMINUM_ORE.id, stack(ALUMINUM_INGOT, 1));
        smelt(Blocks.SAND.id, stack(Blocks.GLASS.id, 1));
        smelt(Blocks.COBBLESTONE.id, stack(Blocks.STONE.id, 1));
        smelt(Blocks.RUBBER_LOG.id, stack(RUBBER, 2));
        smelt(Blocks.CLAY.id, stack(Blocks.BRICKS.id, 1));
        smelt(PORKCHOP_RAW, stack(PORKCHOP_COOKED, 1));
        smelt(BEEF_RAW, stack(BEEF_COOKED, 1));
        smelt(MUTTON_RAW, stack(MUTTON_COOKED, 1));
        smelt(CHICKEN_RAW, stack(CHICKEN_COOKED, 1));

        // ---- Fuels (ticks at 20 tps; one smelt = 200 ticks) ----
        RecipeRegistry.registerFuel(COAL, 1600);
        RecipeRegistry.registerFuel(Blocks.COAL_BLOCK.id, 16000);
        RecipeRegistry.registerFuel(planks, 300);
        RecipeRegistry.registerFuel(Blocks.BIRCH_PLANKS.id, 300);
        RecipeRegistry.registerFuel(Blocks.SPRUCE_PLANKS.id, 300);
        RecipeRegistry.registerFuel(Blocks.OAK_LOG.id, 300);
        RecipeRegistry.registerFuel(Blocks.BIRCH_LOG.id, 300);
        RecipeRegistry.registerFuel(Blocks.SPRUCE_LOG.id, 300);
        RecipeRegistry.registerFuel(Blocks.RUBBER_LOG.id, 300);
        RecipeRegistry.registerFuel(STICK, 100);
    }

    /** Pickaxe/axe/shovel/sword/hoe from one head material. */
    private static void toolSet(int basePickaxeId, int mat) {
        int s = STICK;
        shaped(stack(basePickaxeId, 1), 3, 3, mat, mat, mat, 0, s, 0, 0, s, 0);          // pickaxe
        shaped(stack(basePickaxeId + 1, 1), 2, 3, mat, mat, mat, s, 0, s);               // axe
        shaped(stack(basePickaxeId + 2, 1), 1, 3, mat, s, s);                            // shovel
        shaped(stack(basePickaxeId + 3, 1), 1, 3, mat, mat, s);                          // sword
        shaped(stack(basePickaxeId + 4, 1), 2, 3, mat, mat, 0, s, 0, s);                 // hoe
    }

    private static void nineBlock(int item, int block) {
        shaped(stack(block, 1), 3, 3, item, item, item, item, item, item, item, item, item);
        shapeless(stack(item, 9), block);
    }

    private static ItemStack stack(int id, int count) {
        return new ItemStack(id, count);
    }

    private static void shaped(ItemStack result, int w, int h, int... pattern) {
        RecipeRegistry.register(new ShapedRecipe(w, h, pattern, result));
    }

    private static void shapeless(ItemStack result, int... ingredients) {
        RecipeRegistry.register(new ShapelessRecipe(ingredients, result));
    }

    private static void smelt(int input, ItemStack output) {
        RecipeRegistry.registerSmelting(input, output);
    }
}
