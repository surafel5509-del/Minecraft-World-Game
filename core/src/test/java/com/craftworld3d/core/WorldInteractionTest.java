package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.noise.PerlinNoise;
import com.craftworld3d.core.util.MathUtil;
import com.craftworld3d.core.world.RaycastHit;
import com.craftworld3d.core.world.World;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WorldInteractionTest {
    private World world;

    @Before
    public void setUp() {
        world = TestWorlds.flatWorld();
    }

    @Test
    public void setAndGetBlocksAcrossChunkBorders() {
        world.setBlock(15, 70, 15, Blocks.GLASS.id);
        world.setBlock(16, 70, 15, Blocks.BRICKS.id);
        world.setBlock(-1, 70, -1, Blocks.SANDSTONE.id);
        assertEquals(Blocks.GLASS.id, world.getBlockId(15, 70, 15));
        assertEquals(Blocks.BRICKS.id, world.getBlockId(16, 70, 15));
        assertEquals(Blocks.SANDSTONE.id, world.getBlockId(-1, 70, -1));
        // Neighbour chunks got remesh-flagged.
        assertTrue(world.getLoadedChunk(1, 0).dirty);
    }

    @Test
    public void raycastHitsBlockAndReportsFace() {
        world.setBlock(8, 66, 12, Blocks.STONE.id);
        RaycastHit hit = new RaycastHit();
        // Look straight at it from -Z.
        world.raycast(8.5, 66.5, 8.5, 0, 0, 1, 10, hit);
        assertTrue(hit.hit);
        assertEquals(8, hit.x);
        assertEquals(66, hit.y);
        assertEquals(12, hit.z);
        assertEquals(-1, hit.faceZ); // hit the -Z face
        assertEquals(0, hit.faceX);
    }

    @Test
    public void raycastMissReturnsNoHit() {
        RaycastHit hit = new RaycastHit();
        world.raycast(8.5, 80, 8.5, 0, 1, 0, 30, hit); // straight up into the sky
        assertFalse(hit.hit);
    }

    @Test
    public void posKeyRoundTripsNegativeCoords() {
        int[][] coords = {{0, 0, 0}, {-1, 5, -1}, {123456, 255, -654321}, {-30000000 / 16, 0, 30000000 / 16}};
        for (int[] c : coords) {
            long key = World.posKey(c[0], c[1], c[2]);
            int x = (int) (key >> 38);
            int y = (int) (key & 0xFFF);
            int z = (int) ((key << 26) >> 38);
            assertEquals(c[0], x);
            assertEquals(c[1], y);
            assertEquals(c[2], z);
        }
    }

    @Test
    public void packXZRoundTrips() {
        int[][] coords = {{0, 0}, {-1, 1}, {100000, -100000}};
        for (int[] c : coords) {
            long key = MathUtil.packXZ(c[0], c[1]);
            assertEquals(c[0], MathUtil.unpackX(key));
            assertEquals(c[1], MathUtil.unpackZ(key));
        }
    }

    @Test
    public void lightingAroundTorch() {
        world.setBlock(8, 64, 8, Blocks.TORCH.id);
        var chunk = world.getLoadedChunk(0, 0);
        assertEquals(14, chunk.getBlockLight(8, 64, 8));
        assertEquals(13, chunk.getBlockLight(9, 64, 8));
        assertEquals(12, chunk.getBlockLight(9, 65, 8));
    }

    @Test
    public void skyLightZeroUnderground() {
        var chunk = world.getLoadedChunk(0, 0);
        assertEquals(15, chunk.getSkyLight(8, 100, 8));
        assertEquals(0, chunk.getSkyLight(8, 30, 8)); // under stone
    }

    @Test
    public void noiseIsDeterministicAndBounded() {
        PerlinNoise a = new PerlinNoise(99);
        PerlinNoise b = new PerlinNoise(99);
        PerlinNoise c = new PerlinNoise(100);
        boolean anyDiff = false;
        for (int i = 0; i < 500; i++) {
            double x = i * 0.137, z = i * 0.731;
            assertEquals(a.noise2(x, z), b.noise2(x, z), 1e-12);
            double v = a.fbm2(x, z, 4, 2.0, 0.5);
            assertTrue(Math.abs(v) <= 1.01);
            if (Math.abs(a.noise2(x, z) - c.noise2(x, z)) > 1e-9) anyDiff = true;
        }
        assertTrue(anyDiff);
    }

    @Test
    public void weatherChangesOverTime() {
        world.weatherTicksLeft = 1;
        world.tick(0, 64, 0, 64);
        assertTrue(world.weather.isPrecipitating());
        world.weatherTicksLeft = 1;
        world.tick(0, 64, 0, 64);
        assertFalse(world.weather.isPrecipitating());
    }

    @Test
    public void furnaceSmeltsOre() {
        world.setBlock(5, 64, 5, Blocks.FURNACE.id);
        var furnace = new com.craftworld3d.core.world.blockentity.FurnaceEntity(5, 64, 5);
        furnace.input = new com.craftworld3d.core.item.ItemStack(Blocks.IRON_ORE.id, 1);
        furnace.fuel = new com.craftworld3d.core.item.ItemStack(com.craftworld3d.core.item.ItemIds.COAL, 1);
        world.setBlockEntity(furnace);
        com.craftworld3d.core.crafting.Recipes.init();
        for (int i = 0; i < 210; i++) furnace.tick(world);
        assertNotNull(furnace.output);
        assertEquals(com.craftworld3d.core.item.ItemIds.IRON_INGOT, furnace.output.itemId);
        assertNull(furnace.input);
        // Furnace block swapped to lit while burning.
        assertEquals(Blocks.FURNACE_LIT.id, world.getBlockId(5, 64, 5));
    }
}
