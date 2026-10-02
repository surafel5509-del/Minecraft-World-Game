package com.craftworld3d.core.item;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.block.ToolType;

import static com.craftworld3d.core.item.ItemIds.*;
import static com.craftworld3d.core.item.ToolMaterial.*;

/** All built-in standalone items (materials, food, tools, vehicle items). */
public final class Items {
    private Items() {}

    private static boolean initialized = false;

    /** Registers blocks' items plus all standalone items. Idempotent. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        Blocks.init();

        reg(new Item(STICK, "stick", "item_stick"));
        reg(new Item(COAL, "coal", "item_coal"));
        reg(new Item(IRON_INGOT, "iron_ingot", "item_iron_ingot"));
        reg(new Item(GOLD_INGOT, "gold_ingot", "item_gold_ingot"));
        reg(new Item(COPPER_INGOT, "copper_ingot", "item_copper_ingot"));
        reg(new Item(ALUMINUM_INGOT, "aluminum_ingot", "item_aluminum_ingot"));
        reg(new Item(REDSTONE_DUST, "redstone_dust", "item_redstone", 64, Blocks.REDSTONE_WIRE.id));
        reg(new Item(ItemIds.DIAMOND, "diamond", "item_diamond"));
        reg(new Item(EMERALD, "emerald", "item_emerald"));
        reg(new Item(RUBBER, "rubber", "item_rubber"));
        reg(new Item(WHEAT, "wheat", "item_wheat"));
        reg(new Item(SEEDS, "seeds", "item_seeds", 64, Blocks.WHEAT_CROP.id));
        reg(new FoodItem(BREAD, "bread", "item_bread", 5));
        reg(new FoodItem(PORKCHOP_RAW, "porkchop_raw", "item_porkchop_raw", 3));
        reg(new FoodItem(PORKCHOP_COOKED, "porkchop_cooked", "item_porkchop_cooked", 8));
        reg(new FoodItem(BEEF_RAW, "beef_raw", "item_beef_raw", 3));
        reg(new FoodItem(BEEF_COOKED, "beef_cooked", "item_beef_cooked", 8));
        reg(new FoodItem(MUTTON_RAW, "mutton_raw", "item_mutton_raw", 2));
        reg(new FoodItem(MUTTON_COOKED, "mutton_cooked", "item_mutton_cooked", 6));
        reg(new FoodItem(CHICKEN_RAW, "chicken_raw", "item_chicken_raw", 2));
        reg(new FoodItem(CHICKEN_COOKED, "chicken_cooked", "item_chicken_cooked", 6));
        reg(new Item(FEATHER, "feather", "item_feather"));
        reg(new Item(LEATHER, "leather", "item_leather"));
        reg(new Item(STRING, "string", "item_string"));
        reg(new Item(BONE, "bone", "item_bone"));
        reg(new Item(ARROW, "arrow", "item_arrow"));
        reg(new Item(GUNPOWDER, "gunpowder", "item_gunpowder"));
        reg(new FoodItem(ROTTEN_FLESH, "rotten_flesh", "item_rotten_flesh", 2));
        reg(new Item(NETHER_CRYSTAL, "nether_crystal", "item_nether_crystal"));
        reg(new Item(ENGINE, "engine", "item_engine", 16, -1));
        reg(new Item(WHEEL, "wheel", "item_wheel", 16, -1));
        reg(new Item(PARACHUTE, "parachute", "item_parachute", 1, -1));
        reg(new Item(BOAT, "boat", "item_boat", 1, -1));
        reg(new Item(MINECART, "minecart", "item_minecart", 1, -1));

        tools(WOODEN_PICKAXE, "wooden", WOOD);
        tools(STONE_PICKAXE, "stone", STONE);
        tools(IRON_PICKAXE, "iron", IRON);
        tools(GOLD_PICKAXE, "gold", GOLD);
        tools(DIAMOND_PICKAXE, "diamond", ToolMaterial.DIAMOND);

        reg(new Item(CAR_SEDAN, "car_sedan", "item_car_sedan", 1, -1));
        reg(new Item(CAR_SUV, "car_suv", "item_car_suv", 1, -1));
        reg(new Item(CAR_TRUCK, "car_truck", "item_car_truck", 1, -1));
        reg(new Item(CAR_SPORTS, "car_sports", "item_car_sports", 1, -1));
        reg(new Item(CAR_JEEP, "car_jeep", "item_car_jeep", 1, -1));
        reg(new Item(PLANE_SMALL, "plane_small", "item_plane_small", 1, -1));
        reg(new Item(PLANE_JET, "plane_jet", "item_plane_jet", 1, -1));
        reg(new Item(HELICOPTER, "helicopter", "item_helicopter", 1, -1));
        reg(new Item(BIPLANE, "biplane", "item_biplane", 1, -1));

        // Block items last so explicit items (e.g. redstone dust places wire) win.
        ItemRegistry.registerBlockItems();
    }

    private static void tools(int baseId, String prefix, ToolMaterial mat) {
        reg(new ToolItem(baseId, prefix + "_pickaxe", "item_" + prefix + "_pickaxe", ToolType.PICKAXE, mat));
        reg(new ToolItem(baseId + 1, prefix + "_axe", "item_" + prefix + "_axe", ToolType.AXE, mat));
        reg(new ToolItem(baseId + 2, prefix + "_shovel", "item_" + prefix + "_shovel", ToolType.SHOVEL, mat));
        reg(new ToolItem(baseId + 3, prefix + "_sword", "item_" + prefix + "_sword", ToolType.SWORD, mat));
        reg(new ToolItem(baseId + 4, prefix + "_hoe", "item_" + prefix + "_hoe", ToolType.HOE, mat));
    }

    private static void reg(Item i) {
        ItemRegistry.register(i);
    }
}
