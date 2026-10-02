package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.item.Items;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.ChunkGenerator;
import com.craftworld3d.core.world.World;

/** Shared helpers for tests. */
public final class TestWorlds {
    private TestWorlds() {}

    /** Superflat generator: bedrock, stone to y=59, dirt, grass at y=63. */
    public static final class FlatGenerator implements ChunkGenerator {
        @Override
        public void generate(Chunk chunk) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockId(x, 0, z, Blocks.BEDROCK.id);
                    for (int y = 1; y < 60; y++) chunk.setBlockId(x, y, z, Blocks.STONE.id);
                    for (int y = 60; y < 63; y++) chunk.setBlockId(x, y, z, Blocks.DIRT.id);
                    chunk.setBlockId(x, 63, z, Blocks.GRASS.id);
                }
            }
        }

        @Override
        public int surfaceHeight(int worldX, int worldZ) {
            return 63;
        }
    }

    /** Flat world with the registries initialised and a 5x5 chunk area pre-generated. */
    public static World flatWorld() {
        Items.init();
        World world = new World(1234L, World.DIM_OVERWORLD, new FlatGenerator());
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                world.getOrGenerateChunk(cx, cz);
            }
        }
        return world;
    }
}
