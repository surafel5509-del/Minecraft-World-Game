package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.item.ItemIds;
import com.craftworld3d.core.item.ItemRegistry;
import com.craftworld3d.core.item.Items;
import com.craftworld3d.core.item.ToolItem;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ToolsTest {

    @Before
    public void setUp() {
        Items.init();
    }

    private ToolItem tool(int id) {
        return (ToolItem) ItemRegistry.get(id);
    }

    @Test
    public void allTwentyFiveToolsExist() {
        for (int id = ItemIds.WOODEN_PICKAXE; id <= ItemIds.DIAMOND_HOE; id++) {
            assertNotNull("tool id " + id, ItemRegistry.get(id));
            assertTrue(ItemRegistry.get(id) instanceof ToolItem);
        }
    }

    @Test
    public void pickaxeSpeedsUpStoneButNotDirt() {
        ToolItem pick = tool(ItemIds.IRON_PICKAXE);
        assertTrue(pick.speedAgainst(Blocks.STONE) > 1);
        assertEquals(1.0f, pick.speedAgainst(Blocks.DIRT), 0.001f);
    }

    @Test
    public void harvestLevelGating() {
        assertFalse(tool(ItemIds.WOODEN_PICKAXE).canHarvest(Blocks.DIAMOND_ORE));
        assertFalse(tool(ItemIds.STONE_PICKAXE).canHarvest(Blocks.DIAMOND_ORE));
        assertTrue(tool(ItemIds.IRON_PICKAXE).canHarvest(Blocks.DIAMOND_ORE));
        assertTrue(tool(ItemIds.DIAMOND_PICKAXE).canHarvest(Blocks.OBSIDIAN));
        assertFalse(tool(ItemIds.IRON_PICKAXE).canHarvest(Blocks.OBSIDIAN));
        // Wrong tool class never harvests tool-required blocks.
        assertFalse(tool(ItemIds.DIAMOND_AXE).canHarvest(Blocks.STONE));
        // Blocks without tool requirement are always harvestable.
        assertTrue(tool(ItemIds.WOODEN_HOE).canHarvest(Blocks.DIRT));
    }

    @Test
    public void durabilityIncreasesWithTier() {
        assertTrue(tool(ItemIds.DIAMOND_PICKAXE).maxDurability()
            > tool(ItemIds.IRON_PICKAXE).maxDurability());
        assertTrue(tool(ItemIds.IRON_PICKAXE).maxDurability()
            > tool(ItemIds.WOODEN_PICKAXE).maxDurability());
    }

    @Test
    public void swordsDealMostDamage() {
        assertTrue(tool(ItemIds.DIAMOND_SWORD).attackDamage > tool(ItemIds.DIAMOND_PICKAXE).attackDamage);
        assertTrue(tool(ItemIds.DIAMOND_SWORD).attackDamage > tool(ItemIds.WOODEN_SWORD).attackDamage);
    }

    @Test
    public void goldIsFastButWeak() {
        assertTrue(tool(ItemIds.GOLD_PICKAXE).material.speed > tool(ItemIds.DIAMOND_PICKAXE).material.speed);
        assertTrue(tool(ItemIds.GOLD_PICKAXE).maxDurability() < tool(ItemIds.WOODEN_PICKAXE).maxDurability());
    }
}
