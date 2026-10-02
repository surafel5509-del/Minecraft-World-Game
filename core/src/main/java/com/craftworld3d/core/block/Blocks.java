package com.craftworld3d.core.block;

import com.craftworld3d.core.item.ItemIds;

import static com.craftworld3d.core.block.ToolType.*;

/** All built-in block definitions (data-driven; 80+ blocks). */
public final class Blocks {
    private Blocks() {}

    public static final Block AIR = BlockRegistry.register(
        Block.builder(0, "air", "air").shape(RenderShape.NONE).noCollision().transparent()
            .strength(0, 0).sound(SoundType.NONE).dropsNothing().build());

    public static final Block GRASS = reg(Block.builder(1, "grass", "grass_side")
        .tex("grass_top", "grass_side", "dirt").strength(0.6f, 3).sound(SoundType.GRASS)
        .tool(SHOVEL, 0, false).drops(2, 1, 1));
    public static final Block DIRT = reg(Block.builder(2, "dirt", "dirt")
        .strength(0.5f, 2.5f).sound(SoundType.GRAVEL).tool(SHOVEL, 0, false));
    public static final Block STONE = reg(Block.builder(3, "stone", "stone")
        .strength(1.5f, 30).tool(PICKAXE, 0, true).drops(4, 1, 1));
    public static final Block COBBLESTONE = reg(Block.builder(4, "cobblestone", "cobblestone")
        .strength(2.0f, 30).tool(PICKAXE, 0, true));
    public static final Block SAND = reg(Block.builder(5, "sand", "sand")
        .strength(0.5f, 2.5f).sound(SoundType.SAND).tool(SHOVEL, 0, false));
    public static final Block GRAVEL = reg(Block.builder(6, "gravel", "gravel")
        .strength(0.6f, 3).sound(SoundType.GRAVEL).tool(SHOVEL, 0, false));
    public static final Block CLAY = reg(Block.builder(7, "clay", "clay")
        .strength(0.6f, 3).sound(SoundType.GRAVEL).tool(SHOVEL, 0, false));
    public static final Block SNOW_BLOCK = reg(Block.builder(8, "snow_block", "snow")
        .strength(0.2f, 1).sound(SoundType.SNOW).tool(SHOVEL, 0, false));
    public static final Block ICE = reg(Block.builder(9, "ice", "ice")
        .strength(0.5f, 2.5f).transparent().sound(SoundType.GLASS).tool(PICKAXE, 0, false).dropsNothing());
    public static final Block WATER = reg(Block.builder(10, "water", "water")
        .liquid().strength(100, 100).sound(SoundType.LIQUID).dropsNothing());
    public static final Block LAVA = reg(Block.builder(11, "lava", "lava")
        .liquid().light(15).strength(100, 100).sound(SoundType.LIQUID).dropsNothing());
    public static final Block BEDROCK = reg(Block.builder(12, "bedrock", "bedrock")
        .strength(-1, 3_600_000).dropsNothing());

    public static final Block OAK_LOG = reg(Block.builder(13, "oak_log", "oak_log")
        .tex("log_top", "oak_log", "log_top").strength(2.0f, 10).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block BIRCH_LOG = reg(Block.builder(14, "birch_log", "birch_log")
        .tex("log_top", "birch_log", "log_top").strength(2.0f, 10).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block SPRUCE_LOG = reg(Block.builder(15, "spruce_log", "spruce_log")
        .tex("log_top", "spruce_log", "log_top").strength(2.0f, 10).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block RUBBER_LOG = reg(Block.builder(16, "rubber_log", "rubber_log")
        .tex("log_top", "rubber_log", "log_top").strength(2.0f, 10).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block OAK_PLANKS = reg(Block.builder(17, "oak_planks", "oak_planks")
        .strength(2.0f, 15).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block BIRCH_PLANKS = reg(Block.builder(18, "birch_planks", "birch_planks")
        .strength(2.0f, 15).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block SPRUCE_PLANKS = reg(Block.builder(19, "spruce_planks", "spruce_planks")
        .strength(2.0f, 15).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block OAK_LEAVES = reg(Block.builder(20, "oak_leaves", "oak_leaves")
        .strength(0.2f, 1).transparent().sound(SoundType.GRASS).dropsNothing());
    public static final Block BIRCH_LEAVES = reg(Block.builder(21, "birch_leaves", "birch_leaves")
        .strength(0.2f, 1).transparent().sound(SoundType.GRASS).dropsNothing());
    public static final Block SPRUCE_LEAVES = reg(Block.builder(22, "spruce_leaves", "spruce_leaves")
        .strength(0.2f, 1).transparent().sound(SoundType.GRASS).dropsNothing());
    public static final Block RUBBER_LEAVES = reg(Block.builder(23, "rubber_leaves", "rubber_leaves")
        .strength(0.2f, 1).transparent().sound(SoundType.GRASS).dropsNothing());

    public static final Block GLASS = reg(Block.builder(24, "glass", "glass")
        .strength(0.3f, 1.5f).transparent().sound(SoundType.GLASS).dropsNothing());
    public static final Block BRICKS = reg(Block.builder(25, "bricks", "bricks")
        .strength(2.0f, 30).tool(PICKAXE, 0, true));
    public static final Block STONE_BRICKS = reg(Block.builder(26, "stone_bricks", "stone_bricks")
        .strength(1.5f, 30).tool(PICKAXE, 0, true));
    public static final Block MOSSY_STONE_BRICKS = reg(Block.builder(27, "mossy_stone_bricks", "mossy_stone_bricks")
        .strength(1.5f, 30).tool(PICKAXE, 0, true));
    public static final Block SANDSTONE = reg(Block.builder(28, "sandstone", "sandstone")
        .strength(0.8f, 4).tool(PICKAXE, 0, true));
    public static final Block OBSIDIAN = reg(Block.builder(29, "obsidian", "obsidian")
        .strength(50, 1200).tool(PICKAXE, 3, true));

    public static final Block COAL_ORE = reg(Block.builder(30, "coal_ore", "coal_ore")
        .strength(3, 15).tool(PICKAXE, 0, true).drops(ItemIds.COAL, 1, 2));
    public static final Block IRON_ORE = reg(Block.builder(31, "iron_ore", "iron_ore")
        .strength(3, 15).tool(PICKAXE, 1, true));
    public static final Block GOLD_ORE = reg(Block.builder(32, "gold_ore", "gold_ore")
        .strength(3, 15).tool(PICKAXE, 2, true));
    public static final Block COPPER_ORE = reg(Block.builder(33, "copper_ore", "copper_ore")
        .strength(3, 15).tool(PICKAXE, 1, true));
    public static final Block REDSTONE_ORE = reg(Block.builder(34, "redstone_ore", "redstone_ore")
        .strength(3, 15).tool(PICKAXE, 2, true).drops(ItemIds.REDSTONE_DUST, 2, 4));
    public static final Block DIAMOND_ORE = reg(Block.builder(35, "diamond_ore", "diamond_ore")
        .strength(3, 15).tool(PICKAXE, 2, true).drops(ItemIds.DIAMOND, 1, 1));
    public static final Block EMERALD_ORE = reg(Block.builder(36, "emerald_ore", "emerald_ore")
        .strength(3, 15).tool(PICKAXE, 2, true).drops(ItemIds.EMERALD, 1, 1));
    public static final Block ALUMINUM_ORE = reg(Block.builder(37, "aluminum_ore", "aluminum_ore")
        .strength(3, 15).tool(PICKAXE, 1, true));

    public static final Block COAL_BLOCK = reg(Block.builder(38, "coal_block", "coal_block")
        .strength(5, 30).tool(PICKAXE, 0, true));
    public static final Block IRON_BLOCK = reg(Block.builder(39, "iron_block", "iron_block")
        .strength(5, 30).sound(SoundType.METAL).tool(PICKAXE, 1, true));
    public static final Block GOLD_BLOCK = reg(Block.builder(40, "gold_block", "gold_block")
        .strength(3, 30).sound(SoundType.METAL).tool(PICKAXE, 2, true));
    public static final Block COPPER_BLOCK = reg(Block.builder(41, "copper_block", "copper_block")
        .strength(3, 30).sound(SoundType.METAL).tool(PICKAXE, 1, true));
    public static final Block REDSTONE_BLOCK = reg(Block.builder(42, "redstone_block", "redstone_block")
        .strength(5, 30).tool(PICKAXE, 0, true));
    public static final Block DIAMOND_BLOCK = reg(Block.builder(43, "diamond_block", "diamond_block")
        .strength(5, 30).sound(SoundType.METAL).tool(PICKAXE, 2, true));
    public static final Block EMERALD_BLOCK = reg(Block.builder(44, "emerald_block", "emerald_block")
        .strength(5, 30).sound(SoundType.METAL).tool(PICKAXE, 2, true));
    public static final Block ALUMINUM_BLOCK = reg(Block.builder(45, "aluminum_block", "aluminum_block")
        .strength(4, 25).sound(SoundType.METAL).tool(PICKAXE, 1, true));

    public static final Block CRAFTING_TABLE = reg(Block.builder(46, "crafting_table", "crafting_table_side")
        .tex("crafting_table_top", "crafting_table_side", "oak_planks")
        .strength(2.5f, 12).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block FURNACE = reg(Block.builder(47, "furnace", "furnace_front")
        .tex("furnace_top", "furnace_front", "furnace_top")
        .strength(3.5f, 17).tool(PICKAXE, 0, true));
    public static final Block FURNACE_LIT = reg(Block.builder(48, "furnace_lit", "furnace_front_lit")
        .tex("furnace_top", "furnace_front_lit", "furnace_top")
        .strength(3.5f, 17).light(13).tool(PICKAXE, 0, true).drops(47, 1, 1));
    public static final Block CHEST = reg(Block.builder(49, "chest", "chest_front")
        .tex("chest_top", "chest_front", "chest_top")
        .strength(2.5f, 12).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block DOOR_CLOSED = reg(Block.builder(50, "door", "door")
        .shape(RenderShape.SLAB).transparent().strength(3, 15).sound(SoundType.WOOD).tool(AXE, 0, false));
    public static final Block DOOR_OPEN = reg(Block.builder(51, "door_open", "door")
        .shape(RenderShape.SLAB).transparent().noCollision().strength(3, 15).sound(SoundType.WOOD)
        .tool(AXE, 0, false).drops(50, 1, 1));
    public static final Block LADDER = reg(Block.builder(52, "ladder", "ladder")
        .shape(RenderShape.CROSS).transparent().noCollision().climbable().strength(0.4f, 2).sound(SoundType.WOOD));
    public static final Block TORCH = reg(Block.builder(53, "torch", "torch")
        .shape(RenderShape.CROSS).transparent().noCollision().light(14).strength(0.05f, 0).sound(SoundType.WOOD));

    public static final Block REDSTONE_WIRE = reg(Block.builder(54, "redstone_wire", "redstone_wire")
        .shape(RenderShape.FLAT).transparent().noCollision().strength(0.05f, 0)
        .drops(ItemIds.REDSTONE_DUST, 1, 1));
    public static final Block REDSTONE_TORCH = reg(Block.builder(55, "redstone_torch", "redstone_torch")
        .shape(RenderShape.CROSS).transparent().noCollision().light(7).strength(0.05f, 0));
    public static final Block LEVER = reg(Block.builder(56, "lever", "lever")
        .shape(RenderShape.CROSS).transparent().noCollision().strength(0.5f, 2.5f));
    public static final Block LEVER_ON = reg(Block.builder(57, "lever_on", "lever_on")
        .shape(RenderShape.CROSS).transparent().noCollision().strength(0.5f, 2.5f).drops(56, 1, 1));
    public static final Block BUTTON = reg(Block.builder(58, "button", "button")
        .shape(RenderShape.FLAT).transparent().noCollision().strength(0.5f, 2.5f));
    public static final Block PRESSURE_PLATE = reg(Block.builder(59, "pressure_plate", "pressure_plate")
        .shape(RenderShape.FLAT).transparent().noCollision().strength(0.5f, 2.5f).sound(SoundType.WOOD));
    public static final Block REDSTONE_LAMP = reg(Block.builder(60, "redstone_lamp", "redstone_lamp")
        .strength(0.3f, 1.5f).sound(SoundType.GLASS));
    public static final Block REDSTONE_LAMP_LIT = reg(Block.builder(61, "redstone_lamp_lit", "redstone_lamp_lit")
        .strength(0.3f, 1.5f).light(15).sound(SoundType.GLASS).drops(60, 1, 1));
    public static final Block REPEATER = reg(Block.builder(62, "repeater", "repeater")
        .shape(RenderShape.FLAT).transparent().noCollision().strength(0.5f, 2.5f));
    public static final Block PISTON = reg(Block.builder(63, "piston", "piston_side")
        .tex("piston_top", "piston_side", "piston_side").strength(1.5f, 7));

    public static final Block RAIL = reg(Block.builder(64, "rail", "rail")
        .shape(RenderShape.FLAT).transparent().noCollision().strength(0.7f, 3.5f).sound(SoundType.METAL)
        .tool(PICKAXE, 0, false));
    public static final Block POWERED_RAIL = reg(Block.builder(65, "powered_rail", "powered_rail")
        .shape(RenderShape.FLAT).transparent().noCollision().strength(0.7f, 3.5f).sound(SoundType.METAL)
        .tool(PICKAXE, 0, false));

    public static final Block RUBBER_BLOCK = reg(Block.builder(66, "rubber_block", "rubber_block")
        .strength(0.8f, 4).sound(SoundType.CLOTH));
    public static final Block RUNWAY_BLOCK = reg(Block.builder(67, "runway_block", "runway_block")
        .tex("runway_top", "runway_side", "runway_side")
        .strength(2, 30).tool(PICKAXE, 0, true));

    public static final Block FARMLAND = reg(Block.builder(68, "farmland", "farmland")
        .tex("farmland", "dirt", "dirt").strength(0.6f, 3).sound(SoundType.GRAVEL)
        .tool(SHOVEL, 0, false).drops(2, 1, 1));
    public static final Block WHEAT_CROP = reg(Block.builder(69, "wheat_crop", "wheat_crop")
        .shape(RenderShape.CROSS).transparent().noCollision().strength(0.05f, 0).sound(SoundType.GRASS)
        .drops(ItemIds.WHEAT, 1, 1));
    public static final Block TALL_GRASS = reg(Block.builder(70, "tall_grass", "tall_grass")
        .shape(RenderShape.CROSS).transparent().noCollision().strength(0.05f, 0).sound(SoundType.GRASS)
        .drops(ItemIds.SEEDS, 0, 1));
    public static final Block FLOWER_YELLOW = reg(Block.builder(71, "flower_yellow", "flower_yellow")
        .shape(RenderShape.CROSS).transparent().noCollision().strength(0.05f, 0).sound(SoundType.GRASS));
    public static final Block FLOWER_RED = reg(Block.builder(72, "flower_red", "flower_red")
        .shape(RenderShape.CROSS).transparent().noCollision().strength(0.05f, 0).sound(SoundType.GRASS));
    public static final Block CACTUS = reg(Block.builder(73, "cactus", "cactus_side")
        .tex("cactus_top", "cactus_side", "cactus_top").transparent()
        .strength(0.4f, 2).sound(SoundType.CLOTH));
    public static final Block DEAD_BUSH = reg(Block.builder(74, "dead_bush", "dead_bush")
        .shape(RenderShape.CROSS).transparent().noCollision().strength(0.05f, 0).sound(SoundType.GRASS)
        .drops(ItemIds.STICK, 0, 2));

    // ---- Second dimension ("The Depths") ----
    public static final Block NETHERRACK = reg(Block.builder(75, "netherrack", "netherrack")
        .strength(0.4f, 2).tool(PICKAXE, 0, true));
    public static final Block ASH_SAND = reg(Block.builder(76, "ash_sand", "ash_sand")
        .strength(0.5f, 2.5f).sound(SoundType.SAND).tool(SHOVEL, 0, false));
    public static final Block NETHER_BRICK = reg(Block.builder(77, "nether_brick", "nether_brick")
        .strength(2, 30).tool(PICKAXE, 0, true));
    public static final Block GLOWSTONE = reg(Block.builder(78, "glowstone", "glowstone")
        .strength(0.3f, 1.5f).light(15).sound(SoundType.GLASS));
    public static final Block NETHER_CRYSTAL_ORE = reg(Block.builder(79, "nether_crystal_ore", "nether_crystal_ore")
        .strength(3, 15).light(5).tool(PICKAXE, 2, true).drops(ItemIds.NETHER_CRYSTAL, 1, 2));
    public static final Block PORTAL = reg(Block.builder(80, "portal", "portal")
        .shape(RenderShape.SLAB).transparent().noCollision().light(11).strength(-1, 0)
        .sound(SoundType.GLASS).dropsNothing());

    public static final Block WOOL = reg(Block.builder(81, "wool", "wool")
        .strength(0.8f, 4).sound(SoundType.CLOTH));
    public static final Block TNT = reg(Block.builder(82, "tnt", "tnt_side")
        .tex("tnt_top", "tnt_side", "tnt_top").strength(0.1f, 0).sound(SoundType.GRASS));
    public static final Block BOOKSHELF = reg(Block.builder(83, "bookshelf", "bookshelf")
        .tex("oak_planks", "bookshelf", "oak_planks").strength(1.5f, 7.5f)
        .sound(SoundType.WOOD).tool(AXE, 0, false));

    private static Block reg(Block.Builder b) {
        return BlockRegistry.register(b.build());
    }

    /** Forces class initialisation (registration). Call once at startup. */
    public static void init() {
        // Static initialisers above do the work.
    }
}
