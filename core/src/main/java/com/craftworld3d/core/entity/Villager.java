package com.craftworld3d.core.entity;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.item.ItemIds;
import com.craftworld3d.core.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Village NPC with a small set of barter trades (UI opens on tap). */
public final class Villager extends Mob {

    /** cost item/count -> result item/count */
    public record Trade(int costItem, int costCount, int resultItem, int resultCount) {}

    public static final List<Trade> TRADES = buildTrades();

    public Villager() {
        super(MobType.VILLAGER);
        naturalSpawn = false; // villagers never despawn
    }

    private static List<Trade> buildTrades() {
        List<Trade> t = new ArrayList<>();
        t.add(new Trade(ItemIds.WHEAT, 12, ItemIds.EMERALD, 1));
        t.add(new Trade(ItemIds.COAL, 10, ItemIds.EMERALD, 1));
        t.add(new Trade(ItemIds.LEATHER, 6, ItemIds.EMERALD, 1));
        t.add(new Trade(ItemIds.EMERALD, 1, ItemIds.BREAD, 4));
        t.add(new Trade(ItemIds.EMERALD, 2, ItemIds.IRON_INGOT, 3));
        t.add(new Trade(ItemIds.EMERALD, 3, Blocks.GLASS.id, 8));
        t.add(new Trade(ItemIds.EMERALD, 4, ItemIds.RUBBER, 4));
        t.add(new Trade(ItemIds.EMERALD, 8, ItemIds.DIAMOND, 1));
        return t;
    }

    @Override
    public String typeKey() { return "villager"; }

    /** Attempts the trade against the player's inventory. */
    public static boolean doTrade(Player player, Trade trade) {
        if (player.inventory.count(trade.costItem()) < trade.costCount()) return false;
        player.inventory.remove(trade.costItem(), trade.costCount());
        int left = player.inventory.add(new ItemStack(trade.resultItem(), trade.resultCount()));
        return left == 0;
    }
}
