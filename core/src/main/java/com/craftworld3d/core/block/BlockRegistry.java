package com.craftworld3d.core.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Id- and name-indexed registry of all block definitions. */
public final class BlockRegistry {
    public static final int MAX_BLOCKS = 256;

    private static final Block[] BY_ID = new Block[MAX_BLOCKS];
    private static final Map<String, Block> BY_NAME = new HashMap<>();
    private static final List<Block> ALL = new ArrayList<>();

    private BlockRegistry() {}

    public static Block register(Block block) {
        if (block.id < 0 || block.id >= MAX_BLOCKS)
            throw new IllegalArgumentException("Block id out of range: " + block.id);
        if (BY_ID[block.id] != null)
            throw new IllegalStateException("Duplicate block id " + block.id + " (" + block.name + ")");
        if (BY_NAME.containsKey(block.name))
            throw new IllegalStateException("Duplicate block name " + block.name);
        BY_ID[block.id] = block;
        BY_NAME.put(block.name, block);
        ALL.add(block);
        return block;
    }

    /** Never returns null: unknown ids resolve to air so corrupted data can't crash the game. */
    public static Block get(int id) {
        Block b = (id >= 0 && id < MAX_BLOCKS) ? BY_ID[id] : null;
        return b != null ? b : Blocks.AIR;
    }

    public static Block byName(String name) { return BY_NAME.get(name); }

    public static List<Block> all() { return ALL; }

    public static int count() { return ALL.size(); }
}
