package com.craftworld3d.core;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.BlockRegistry;
import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.item.ItemRegistry;
import com.craftworld3d.core.item.Items;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BlockRegistryTest {

    @Before
    public void setUp() {
        Items.init();
    }

    @Test
    public void hasAtLeast60Blocks() {
        assertTrue("Expected >= 60 blocks, got " + BlockRegistry.count(),
            BlockRegistry.count() >= 60);
    }

    @Test
    public void unknownIdResolvesToAir() {
        assertSame(Blocks.AIR, BlockRegistry.get(9999));
        assertSame(Blocks.AIR, BlockRegistry.get(-5));
        assertSame(Blocks.AIR, BlockRegistry.get(200));
    }

    @Test
    public void namesAreUniqueAndResolvable() {
        for (Block b : BlockRegistry.all()) {
            assertSame("byName roundtrip for " + b.name, b, BlockRegistry.byName(b.name));
        }
    }

    @Test
    public void everyBlockHasTextures() {
        for (Block b : BlockRegistry.all()) {
            assertNotNull(b.texTop);
            assertNotNull(b.texSide);
            assertNotNull(b.texBottom);
        }
    }

    @Test
    public void dropsReferToRealItems() {
        for (Block b : BlockRegistry.all()) {
            if (b.dropItem >= 0) {
                assertNotNull("Block " + b.name + " drops unknown item " + b.dropItem,
                    ItemRegistry.get(b.dropItem));
            }
        }
    }

    @Test
    public void lightEmittersAreMarked() {
        assertEquals(15, Blocks.LAVA.lightEmission);
        assertEquals(14, Blocks.TORCH.lightEmission);
        assertEquals(15, Blocks.GLOWSTONE.lightEmission);
        assertEquals(0, Blocks.STONE.lightEmission);
    }

    @Test
    public void physicalFlags() {
        assertFalse(Blocks.WATER.collidable);
        assertTrue(Blocks.WATER.liquid);
        assertTrue(Blocks.LADDER.climbable);
        assertTrue(Blocks.BEDROCK.isUnbreakable());
        assertTrue(Blocks.GLASS.transparent);
        assertTrue(Blocks.STONE.isOpaqueCube());
        assertFalse(Blocks.GLASS.isOpaqueCube());
    }

    @Test
    public void stoneRequiresPickaxeAndDropsCobble() {
        assertTrue(Blocks.STONE.toolRequired);
        assertEquals(Blocks.COBBLESTONE.id, Blocks.STONE.dropItem);
    }
}
