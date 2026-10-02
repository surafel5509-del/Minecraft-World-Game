package com.craftworld3d.core.item;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.BlockRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Id- and name-indexed registry of items. Block items are derived automatically. */
public final class ItemRegistry {
    public static final int MAX_ITEMS = 512;

    private static final Item[] BY_ID = new Item[MAX_ITEMS];
    private static final Map<String, Item> BY_NAME = new HashMap<>();
    private static final List<Item> ALL = new ArrayList<>();

    private ItemRegistry() {}

    public static Item register(Item item) {
        if (item.id < 0 || item.id >= MAX_ITEMS)
            throw new IllegalArgumentException("Item id out of range: " + item.id);
        if (BY_ID[item.id] != null)
            throw new IllegalStateException("Duplicate item id " + item.id + " (" + item.name + ")");
        BY_ID[item.id] = item;
        BY_NAME.put(item.name, item);
        ALL.add(item);
        return item;
    }

    /** Registers a placeable item for every block that doesn't already have one. */
    public static void registerBlockItems() {
        for (Block b : BlockRegistry.all()) {
            if (b.isAir() || BY_ID[b.id] != null) continue;
            register(new Item(b.id, b.name, b.texSide.equals("air") ? b.texTop : blockIcon(b), 64, b.id));
        }
    }

    private static String blockIcon(Block b) {
        // Cross/flat blocks look best using their actual texture; cubes use the side.
        return switch (b.shape) {
            case CROSS, FLAT, SLAB -> b.texSide;
            default -> b.texSide;
        };
    }

    public static Item get(int id) {
        return (id >= 0 && id < MAX_ITEMS) ? BY_ID[id] : null;
    }

    public static Item byName(String name) { return BY_NAME.get(name); }

    public static List<Item> all() { return ALL; }
}
