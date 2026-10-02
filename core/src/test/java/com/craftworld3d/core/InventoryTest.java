package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.inventory.Inventory;
import com.craftworld3d.core.item.ItemIds;
import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.item.Items;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InventoryTest {
    private Inventory inv;

    @Before
    public void setUp() {
        Items.init();
        inv = new Inventory();
    }

    @Test
    public void has36SlotsAnd9HotbarSlots() {
        assertEquals(36, inv.size());
        assertEquals(9, Inventory.HOTBAR_SIZE);
    }

    @Test
    public void addMergesIntoExistingStacks() {
        inv.set(3, new ItemStack(Blocks.DIRT.id, 30));
        int leftover = inv.add(new ItemStack(Blocks.DIRT.id, 40));
        assertEquals(0, leftover);
        assertEquals(64, inv.get(3).count);
        assertEquals(6, inv.get(0).count); // remainder in first free slot
    }

    @Test
    public void addRespectsMaxStack() {
        int leftover = inv.add(new ItemStack(Blocks.STONE.id, 64 * 36 + 5));
        assertEquals(5, leftover);
        for (int i = 0; i < 36; i++) {
            assertEquals(64, inv.get(i).count);
        }
    }

    @Test
    public void toolsDoNotStack() {
        inv.add(new ItemStack(ItemIds.IRON_PICKAXE, 1));
        inv.add(new ItemStack(ItemIds.IRON_PICKAXE, 1));
        assertEquals(1, inv.get(0).count);
        assertEquals(1, inv.get(1).count);
    }

    @Test
    public void countAndRemove() {
        inv.add(new ItemStack(ItemIds.COAL, 80));
        assertEquals(80, inv.count(ItemIds.COAL));
        assertEquals(50, inv.remove(ItemIds.COAL, 50));
        assertEquals(30, inv.count(ItemIds.COAL));
        assertEquals(30, inv.remove(ItemIds.COAL, 99));
        assertEquals(0, inv.count(ItemIds.COAL));
    }

    @Test
    public void clickSlotPicksUpAndPlaces() {
        inv.set(0, new ItemStack(Blocks.SAND.id, 10));
        ItemStack cursor = inv.clickSlot(0, null);
        assertNotNull(cursor);
        assertEquals(10, cursor.count);
        assertNull(inv.get(0));
        cursor = inv.clickSlot(5, cursor);
        assertNull(cursor);
        assertEquals(10, inv.get(5).count);
    }

    @Test
    public void clickSlotMergesAndSwaps() {
        inv.set(0, new ItemStack(Blocks.SAND.id, 60));
        ItemStack cursor = inv.clickSlot(0, new ItemStack(Blocks.SAND.id, 10));
        assertNotNull(cursor);
        assertEquals(6, cursor.count);          // 4 moved in
        assertEquals(64, inv.get(0).count);
        // Swap different items.
        cursor = inv.clickSlot(0, new ItemStack(Blocks.DIRT.id, 3));
        assertEquals(Blocks.SAND.id, cursor.itemId);
        assertEquals(Blocks.DIRT.id, inv.get(0).itemId);
    }

    @Test
    public void splitSlotTakesHalfRoundedUp() {
        inv.set(2, new ItemStack(Blocks.DIRT.id, 9));
        ItemStack half = inv.splitSlot(2);
        assertEquals(5, half.count);
        assertEquals(4, inv.get(2).count);
    }

    @Test
    public void stackSplitAndDamage() {
        ItemStack s = new ItemStack(Blocks.DIRT.id, 10);
        ItemStack out = s.split(4);
        assertEquals(4, out.count);
        assertEquals(6, s.count);

        ItemStack tool = new ItemStack(ItemIds.WOODEN_PICKAXE, 1);
        boolean broke = false;
        for (int i = 0; i < 60; i++) broke = tool.damageItem();
        assertTrue(broke);
        assertTrue(tool.isEmpty());
    }
}
