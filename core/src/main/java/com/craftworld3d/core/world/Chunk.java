package com.craftworld3d.core.world;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.BlockRegistry;

/**
 * A 16 x 256 x 16 column of blocks.
 * Block storage is a flat short array indexed (y << 8) | (z << 4) | x.
 * Light is stored as one byte per cell: high nibble sky light, low nibble block light.
 */
public final class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 256;
    public static final int SIZE_Z = 16;
    public static final int BLOCK_COUNT = SIZE_X * SIZE_Y * SIZE_Z;

    public final int chunkX, chunkZ;
    private final short[] blocks = new short[BLOCK_COUNT];
    private final byte[] light = new byte[BLOCK_COUNT];
    private final int[] heightMap = new int[SIZE_X * SIZE_Z];

    /** Needs remeshing by the renderer. */
    public volatile boolean dirty = true;
    /** Has unsaved modifications. */
    public volatile boolean modified = false;
    /** Fully generated and lit. */
    public volatile boolean ready = false;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    public static boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    public int getBlockId(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return blocks[index(x, y, z)];
    }

    public Block getBlock(int x, int y, int z) {
        return BlockRegistry.get(getBlockId(x, y, z));
    }

    public void setBlockId(int x, int y, int z, int id) {
        if (!inBounds(x, y, z)) return;
        blocks[index(x, y, z)] = (short) id;
        int hi = heightIndex(x, z);
        if (id != 0 && y >= heightMap[hi]) {
            heightMap[hi] = y + 1;
        } else if (id == 0 && y == heightMap[hi] - 1) {
            int yy = y;
            while (yy > 0 && blocks[index(x, yy, z)] == 0) yy--;
            heightMap[hi] = blocks[index(x, yy, z)] == 0 ? 0 : yy + 1;
        }
    }

    private static int heightIndex(int x, int z) { return (z << 4) | x; }

    /** First free Y above the highest non-air block in this column. */
    public int getHeight(int x, int z) {
        return heightMap[heightIndex(x, z)];
    }

    public int getSkyLight(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 15;
        return (light[index(x, y, z)] >> 4) & 0xF;
    }

    public int getBlockLight(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return light[index(x, y, z)] & 0xF;
    }

    public short[] rawBlocks() { return blocks; }

    public void recalcHeightMap() {
        for (int z = 0; z < SIZE_Z; z++) {
            for (int x = 0; x < SIZE_X; x++) {
                int y = SIZE_Y - 1;
                while (y >= 0 && blocks[index(x, y, z)] == 0) y--;
                heightMap[heightIndex(x, z)] = y + 1;
            }
        }
    }

    /**
     * Recomputes chunk-local lighting:
     * sky light flows straight down (full above the heightmap, attenuated under cover),
     * block light spreads with BFS from emitting blocks within this chunk.
     */
    public void recalcLight() {
        // Sky light.
        for (int z = 0; z < SIZE_Z; z++) {
            for (int x = 0; x < SIZE_X; x++) {
                int sky = 15;
                for (int y = SIZE_Y - 1; y >= 0; y--) {
                    int idx = index(x, y, z);
                    Block b = BlockRegistry.get(blocks[idx]);
                    if (!b.isAir()) {
                        if (b.isOpaqueCube()) sky = 0;
                        else if (sky > 0) sky = Math.max(0, sky - 2);
                    }
                    light[idx] = (byte) ((sky << 4) & 0xF0);
                }
            }
        }
        // Block light BFS.
        int[] queue = new int[4096];
        int head = 0, tail = 0;
        for (int i = 0; i < BLOCK_COUNT; i++) {
            Block b = BlockRegistry.get(blocks[i]);
            if (b.lightEmission > 0) {
                light[i] = (byte) ((light[i] & 0xF0) | b.lightEmission);
                if (tail < queue.length) queue[tail++] = i;
            }
        }
        while (head < tail) {
            int idx = queue[head++];
            if (head == queue.length) head = 0;
            int level = light[idx] & 0xF;
            if (level <= 1) continue;
            int x = idx & 0xF, z = (idx >> 4) & 0xF, y = idx >> 8;
            for (int d = 0; d < 6; d++) {
                int nx = x + DX[d], ny = y + DY[d], nz = z + DZ[d];
                if (!inBounds(nx, ny, nz)) continue;
                int nIdx = index(nx, ny, nz);
                Block nb = BlockRegistry.get(blocks[nIdx]);
                if (nb.isOpaqueCube()) continue;
                int nLevel = light[nIdx] & 0xF;
                if (nLevel < level - 1) {
                    light[nIdx] = (byte) ((light[nIdx] & 0xF0) | (level - 1));
                    int next = (tail + 1) % queue.length;
                    if (next != head) {
                        queue[tail] = nIdx;
                        tail = next;
                    }
                }
            }
        }
    }

    private static final int[] DX = {1, -1, 0, 0, 0, 0};
    private static final int[] DY = {0, 0, 1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 0, 0, 1, -1};
}
