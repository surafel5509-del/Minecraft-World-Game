package com.craftworld3d.core.world.gen;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.noise.PerlinNoise;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.ChunkGenerator;

import java.util.Random;

/**
 * "The Depths" - the second dimension, reached through an obsidian portal.
 * A 128-block-high cavern world of netherrack, ash sand, lava seas,
 * glowstone clusters and glowing crystal ore. Its seed is derived
 * deterministically from the main world seed.
 */
public final class DepthsGenerator implements ChunkGenerator {
    public static final int HEIGHT = 128;
    public static final int LAVA_LEVEL = 32;

    private final long seed;
    private final PerlinNoise density, floor;

    /** Derives the dimension seed from the overworld seed. */
    public static long deriveSeed(long worldSeed) {
        return worldSeed * 31L + 1_000_003L;
    }

    public DepthsGenerator(long worldSeed) {
        this.seed = deriveSeed(worldSeed);
        density = new PerlinNoise(seed);
        floor = new PerlinNoise(seed + 7);
    }

    @Override
    public int surfaceHeight(int x, int z) {
        // Floor height of the big cavern (parts dip below the lava level).
        return (int) (28 + floor.fbm2(x * 0.01, z * 0.01, 3, 2.0, 0.5) * 9);
    }

    @Override
    public void generate(Chunk chunk) {
        int baseX = chunk.chunkX << 4, baseZ = chunk.chunkZ << 4;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int wx = baseX + lx, wz = baseZ + lz;
                int floorH = surfaceHeight(wx, wz);
                for (int y = 0; y < HEIGHT; y++) {
                    int id;
                    if (y == 0 || y == HEIGHT - 1) {
                        id = Blocks.BEDROCK.id;
                    } else if (y <= floorH || y >= HEIGHT - 10) {
                        id = Blocks.NETHERRACK.id;
                    } else {
                        // 3D noise carves pillars/islands in the cavern.
                        double n = density.fbm3(wx * 0.035, y * 0.05, wz * 0.035, 3, 2.0, 0.5);
                        id = n > 0.32 ? Blocks.NETHERRACK.id : 0;
                    }
                    // Lava sea fills open space below the lava level.
                    if (id == 0 && y <= LAVA_LEVEL && y > 0) id = Blocks.LAVA.id;
                    if (id != 0) chunk.setBlockId(lx, y, lz, id);
                }
            }
        }

        Random rnd = new Random(seed ^ (chunk.chunkX * 341873128712L + chunk.chunkZ * 132897987541L));
        // Ash sand patches on the cavern floor.
        for (int i = 0; i < 24; i++) {
            int lx = rnd.nextInt(16), lz = rnd.nextInt(16);
            int y = topOfFloor(chunk, lx, lz);
            if (y > LAVA_LEVEL && chunk.getBlockId(lx, y, lz) == Blocks.NETHERRACK.id) {
                chunk.setBlockId(lx, y, lz, Blocks.ASH_SAND.id);
            }
        }
        // Glowstone clusters on ceilings.
        for (int i = 0; i < 6; i++) {
            int lx = rnd.nextInt(16), lz = rnd.nextInt(16);
            for (int y = HEIGHT - 12; y > LAVA_LEVEL; y--) {
                if (chunk.getBlockId(lx, y, lz) != 0 && chunk.getBlockId(lx, y - 1, lz) == 0) {
                    chunk.setBlockId(lx, y - 1, lz, Blocks.GLOWSTONE.id);
                    if (rnd.nextBoolean() && y - 2 > LAVA_LEVEL) {
                        chunk.setBlockId(lx, y - 2, lz, Blocks.GLOWSTONE.id);
                    }
                    break;
                }
            }
        }
        // Crystal ore veins.
        for (int i = 0; i < 8; i++) {
            int lx = rnd.nextInt(16), lz = rnd.nextInt(16);
            int y = 8 + rnd.nextInt(HEIGHT - 24);
            for (int j = 0; j < 4; j++) {
                int ox = lx + rnd.nextInt(3) - 1, oy = y + rnd.nextInt(3) - 1, oz = lz + rnd.nextInt(3) - 1;
                if (Chunk.inBounds(ox, oy, oz) && chunk.getBlockId(ox, oy, oz) == Blocks.NETHERRACK.id) {
                    chunk.setBlockId(ox, oy, oz, Blocks.NETHER_CRYSTAL_ORE.id);
                }
            }
        }
        // Nether brick ruins, rarely.
        if (rnd.nextInt(40) == 0) {
            int lx = 4 + rnd.nextInt(8), lz = 4 + rnd.nextInt(8);
            int y = topOfFloor(chunk, lx, lz);
            if (y > LAVA_LEVEL) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) != 2 && Math.abs(dz) != 2) continue;
                        for (int dy = 1; dy <= 1 + rnd.nextInt(3); dy++) {
                            if (Chunk.inBounds(lx + dx, y + dy, lz + dz)) {
                                chunk.setBlockId(lx + dx, y + dy, lz + dz, Blocks.NETHER_BRICK.id);
                            }
                        }
                    }
                }
            }
        }
    }

    private int topOfFloor(Chunk chunk, int lx, int lz) {
        for (int y = HEIGHT - 12; y > 1; y--) {
            if (chunk.getBlockId(lx, y, lz) != 0 && chunk.getBlockId(lx, y + 1, lz) == 0) return y;
        }
        return -1;
    }
}
