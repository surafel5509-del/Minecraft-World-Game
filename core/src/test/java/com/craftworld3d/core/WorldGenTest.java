package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.item.Items;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.World;
import com.craftworld3d.core.world.gen.BiomeType;
import com.craftworld3d.core.world.gen.DepthsGenerator;
import com.craftworld3d.core.world.gen.OverworldGenerator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WorldGenTest {

    @Before
    public void setUp() {
        Items.init();
    }

    @Test
    public void chunkHasBedrockFloorAndSaneSurface() {
        OverworldGenerator gen = new OverworldGenerator(42L);
        Chunk chunk = new Chunk(0, 0);
        gen.generate(chunk);
        chunk.recalcHeightMap();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(Blocks.BEDROCK.id, chunk.getBlockId(x, 0, z));
                int h = chunk.getHeight(x, z);
                assertTrue("height " + h, h > 5 && h < 250);
            }
        }
    }

    @Test
    public void seedDeterminism() {
        long seed = 987654321L;
        OverworldGenerator a = new OverworldGenerator(seed);
        OverworldGenerator b = new OverworldGenerator(seed);
        int[][] coords = {{0, 0}, {3, -7}, {-12, 25}, {100, 100}};
        for (int[] c : coords) {
            Chunk ca = new Chunk(c[0], c[1]);
            Chunk cb = new Chunk(c[0], c[1]);
            a.generate(ca);
            b.generate(cb);
            assertArrayEquals("chunk " + c[0] + "," + c[1],
                ca.rawBlocks(), cb.rawBlocks());
        }
    }

    @Test
    public void differentSeedsDiffer() {
        OverworldGenerator a = new OverworldGenerator(1L);
        OverworldGenerator b = new OverworldGenerator(2L);
        Chunk ca = new Chunk(0, 0);
        Chunk cb = new Chunk(0, 0);
        a.generate(ca);
        b.generate(cb);
        assertFalse(java.util.Arrays.equals(ca.rawBlocks(), cb.rawBlocks()));
    }

    @Test
    public void biomesCoverExpectedVariety() {
        OverworldGenerator gen = new OverworldGenerator(2024L);
        java.util.Set<BiomeType> seen = java.util.EnumSet.noneOf(BiomeType.class);
        for (int x = -2000; x <= 2000; x += 16) {
            for (int z = -2000; z <= 2000; z += 16) {
                seen.add(gen.biomeAt(x, z));
            }
        }
        // A large sample should expose most biome types.
        assertTrue("saw only " + seen, seen.size() >= 7);
        assertTrue(seen.contains(BiomeType.OCEAN));
        assertTrue(seen.contains(BiomeType.PLAINS) || seen.contains(BiomeType.FOREST));
    }

    @Test
    public void oresExistUnderground() {
        OverworldGenerator gen = new OverworldGenerator(7L);
        boolean foundOre = false;
        for (int cx = 0; cx < 6 && !foundOre; cx++) {
            for (int cz = 0; cz < 6 && !foundOre; cz++) {
                Chunk chunk = new Chunk(cx, cz);
                gen.generate(chunk);
                for (int i = 0; i < Chunk.BLOCK_COUNT; i++) {
                    short id = chunk.rawBlocks()[i];
                    if (id == Blocks.COAL_ORE.id || id == Blocks.IRON_ORE.id) {
                        foundOre = true;
                        break;
                    }
                }
            }
        }
        assertTrue("expected coal/iron ore in 36 chunks", foundOre);
    }

    @Test
    public void treesGenerateInForests() {
        OverworldGenerator gen = new OverworldGenerator(33L);
        boolean foundLog = false;
        outer:
        for (int cx = -20; cx < 20; cx += 2) {
            for (int cz = -20; cz < 20; cz += 2) {
                if (gen.biomeAt((cx << 4) + 8, (cz << 4) + 8) != BiomeType.FOREST) continue;
                Chunk chunk = new Chunk(cx, cz);
                gen.generate(chunk);
                for (int i = 0; i < Chunk.BLOCK_COUNT; i++) {
                    short id = chunk.rawBlocks()[i];
                    if (id == Blocks.OAK_LOG.id || id == Blocks.BIRCH_LOG.id || id == Blocks.RUBBER_LOG.id) {
                        foundLog = true;
                        break outer;
                    }
                }
            }
        }
        assertTrue("expected trees in forest chunks", foundLog);
    }

    @Test
    public void streamingGeneratesOnDemandAndUnloads() {
        World world = new World(5L, World.DIM_OVERWORLD, new OverworldGenerator(5L));
        assertEquals(0, world.loadedChunkCount());
        world.getOrGenerateChunk(0, 0);
        world.getOrGenerateChunk(1, 0);
        assertEquals(2, world.loadedChunkCount());
        assertNotNull(world.getLoadedChunk(0, 0));
        world.removeChunk(0, 0);
        assertEquals(1, world.loadedChunkCount());
        assertNull(world.getLoadedChunk(0, 0));
    }

    @Test
    public void depthsDimensionIsDeterministicAndDerived() {
        assertEquals(DepthsGenerator.deriveSeed(10L), DepthsGenerator.deriveSeed(10L));
        assertNotEquals(DepthsGenerator.deriveSeed(10L), DepthsGenerator.deriveSeed(11L));

        DepthsGenerator a = new DepthsGenerator(10L);
        DepthsGenerator b = new DepthsGenerator(10L);
        Chunk ca = new Chunk(2, 2);
        Chunk cb = new Chunk(2, 2);
        a.generate(ca);
        b.generate(cb);
        assertArrayEquals(ca.rawBlocks(), cb.rawBlocks());

        // Bedrock caps and some lava/netherrack.
        boolean lava = false, rack = false;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(Blocks.BEDROCK.id, ca.getBlockId(x, 0, z));
                assertEquals(Blocks.BEDROCK.id, ca.getBlockId(x, DepthsGenerator.HEIGHT - 1, z));
                for (int y = 1; y < DepthsGenerator.HEIGHT; y++) {
                    int id = ca.getBlockId(x, y, z);
                    if (id == Blocks.LAVA.id) lava = true;
                    if (id == Blocks.NETHERRACK.id) rack = true;
                }
            }
        }
        assertTrue(rack);
        assertTrue(lava);
    }

    @Test
    public void villagesAppearSomewhere() {
        OverworldGenerator gen = new OverworldGenerator(2026L);
        // Search regions for a village plan.
        boolean found = false;
        for (int rx = -6; rx <= 6 && !found; rx++) {
            for (int rz = -6; rz <= 6 && !found; rz++) {
                if (gen.villagePlanner().planFor(rx, rz, gen) != null) found = true;
            }
        }
        assertTrue("expected at least one village in 169 regions", found);
    }

    @Test
    public void daylightCycle() {
        World world = new World(1L, World.DIM_OVERWORLD, new TestWorlds.FlatGenerator());
        world.time = 6000; // midday
        assertEquals(1f, world.daylight(), 0.001f);
        assertFalse(world.isNight());
        world.time = 18000; // midnight
        assertEquals(0.15f, world.daylight(), 0.01f);
        assertTrue(world.isNight());
    }
}
