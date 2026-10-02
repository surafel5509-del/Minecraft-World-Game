package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.world.World;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RedstoneTest {
    private World world;

    @Before
    public void setUp() {
        world = TestWorlds.flatWorld();
    }

    @Test
    public void leverPowersWireAndLightsLamp() {
        int y = 64;
        world.setBlock(2, y, 2, Blocks.LEVER.id);
        for (int x = 3; x <= 7; x++) {
            world.setBlock(x, y, 2, Blocks.REDSTONE_WIRE.id);
        }
        world.setBlock(8, y, 2, Blocks.REDSTONE_LAMP.id);
        assertEquals(Blocks.REDSTONE_LAMP.id, world.getBlockId(8, y, 2));

        // Flip the lever on.
        assertTrue(world.redstone.interact(world, 2, y, 2));
        assertEquals(Blocks.LEVER_ON.id, world.getBlockId(2, y, 2));
        assertTrue(world.redstone.wirePowerAt(3, y, 2) > 0);
        assertEquals(Blocks.REDSTONE_LAMP_LIT.id, world.getBlockId(8, y, 2));

        // Flip it off.
        assertTrue(world.redstone.interact(world, 2, y, 2));
        assertEquals(Blocks.REDSTONE_LAMP.id, world.getBlockId(8, y, 2));
        assertEquals(0, world.redstone.wirePowerAt(3, y, 2));
    }

    @Test
    public void wireDecaysOver15Blocks() {
        int y = 64;
        world.setBlock(0, y, 5, Blocks.LEVER.id);
        for (int x = 1; x <= 14; x++) {
            world.setBlock(x, y, 5, Blocks.REDSTONE_WIRE.id);
        }
        world.redstone.interact(world, 0, y, 5); // on
        assertEquals(15, world.redstone.wirePowerAt(1, y, 5));
        assertEquals(2, world.redstone.wirePowerAt(14, y, 5));
    }

    @Test
    public void repeaterRefreshesSignal() {
        int y = 64;
        world.setBlock(0, y, 7, Blocks.LEVER.id);
        for (int x = 1; x <= 13; x++) {
            world.setBlock(x, y, 7, Blocks.REDSTONE_WIRE.id);
        }
        world.setBlock(14, y, 7, Blocks.REPEATER.id);
        world.setBlock(15, y, 7, Blocks.REDSTONE_WIRE.id);
        world.redstone.interact(world, 0, y, 7);
        assertEquals(15, world.redstone.wirePowerAt(14, y, 7));
        assertEquals(14, world.redstone.wirePowerAt(15, y, 7));
    }

    @Test
    public void redstoneTorchIsAlwaysOnSource() {
        int y = 64;
        world.setBlock(4, y, 10, Blocks.REDSTONE_TORCH.id);
        world.setBlock(5, y, 10, Blocks.REDSTONE_WIRE.id);
        world.setBlock(6, y, 10, Blocks.REDSTONE_LAMP.id);
        assertEquals(Blocks.REDSTONE_LAMP_LIT.id, world.getBlockId(6, y, 10));
    }

    @Test
    public void doorOpensWithPower() {
        int y = 64;
        world.setBlock(10, y, 10, Blocks.DOOR_CLOSED.id);
        world.setBlock(11, y, 10, Blocks.LEVER.id);
        world.redstone.interact(world, 11, y, 10);
        assertEquals(Blocks.DOOR_OPEN.id, world.getBlockId(10, y, 10));
        world.redstone.interact(world, 11, y, 10);
        assertEquals(Blocks.DOOR_CLOSED.id, world.getBlockId(10, y, 10));
    }

    @Test
    public void buttonPowersTemporarily() {
        int y = 64;
        world.setBlock(2, y, 12, Blocks.BUTTON.id);
        world.setBlock(3, y, 12, Blocks.REDSTONE_LAMP.id);
        world.redstone.interact(world, 2, y, 12);
        assertEquals(Blocks.REDSTONE_LAMP_LIT.id, world.getBlockId(3, y, 12));
        // Expires after ~20 ticks.
        for (int i = 0; i < 25; i++) world.redstone.tick(world);
        assertEquals(Blocks.REDSTONE_LAMP.id, world.getBlockId(3, y, 12));
    }

    @Test
    public void pistonPushesBlockUp() {
        int y = 64;
        world.setBlock(6, y, 14, Blocks.PISTON.id);
        world.setBlock(6, y + 1, 14, Blocks.SAND.id);
        world.setBlock(7, y, 14, Blocks.LEVER.id);
        world.redstone.interact(world, 7, y, 14);
        assertEquals(0, world.getBlockId(6, y + 1, 14));
        assertEquals(Blocks.SAND.id, world.getBlockId(6, y + 2, 14));
    }
}
