package com.craftworld3d.core.world;

/** Deterministic chunk generator. Must only depend on the seed and chunk coordinates. */
public interface ChunkGenerator {
    void generate(Chunk chunk);

    /** Terrain height (pure function of world x/z) used for spawning and structures. */
    int surfaceHeight(int worldX, int worldZ);
}
