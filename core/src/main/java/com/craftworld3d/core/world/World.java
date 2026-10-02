package com.craftworld3d.core.world;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.BlockRegistry;
import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.entity.Entity;
import com.craftworld3d.core.entity.Player;
import com.craftworld3d.core.redstone.RedstoneEngine;
import com.craftworld3d.core.util.AABB;
import com.craftworld3d.core.util.MathUtil;
import com.craftworld3d.core.world.blockentity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single dimension's block + entity container.
 * Chunks are generated on demand by the deterministic generator.
 */
public final class World {
    public static final int SEA_LEVEL = 62;
    public static final long DAY_LENGTH = 24000; // ticks; 20 tps -> 20 min days

    public static final int DIM_OVERWORLD = 0;
    public static final int DIM_DEPTHS = -1;

    public final long seed;
    public final int dimension;
    public final ChunkGenerator generator;

    private final ConcurrentHashMap<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, BlockEntity> blockEntities = new ConcurrentHashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> pendingAdd = new ArrayList<>();

    /** World time in ticks. */
    public long time = 1000;
    public Weather weather = Weather.CLEAR;
    public int weatherTicksLeft = 12000;

    /** The player currently in this dimension (set by the engine; may be null in tests). */
    public Player player;
    public final RedstoneEngine redstone = new RedstoneEngine();

    /** One-tick event flags polled by the app layer for sound/particles. */
    public boolean explosionHappened;
    public boolean itemPickedUp;

    private final Random rand = new Random();
    private BlockChangeListener listener;

    public interface BlockChangeListener {
        void onBlockChanged(int x, int y, int z, int oldId, int newId);
    }

    public World(long seed, int dimension, ChunkGenerator generator) {
        this.seed = seed;
        this.dimension = dimension;
        this.generator = generator;
    }

    public void setListener(BlockChangeListener l) { this.listener = l; }

    // ---------------- Chunk access ----------------

    public Chunk getLoadedChunk(int cx, int cz) {
        return chunks.get(MathUtil.packXZ(cx, cz));
    }

    /** Returns the chunk, generating it synchronously if missing. */
    public Chunk getOrGenerateChunk(int cx, int cz) {
        return chunks.computeIfAbsent(MathUtil.packXZ(cx, cz), key -> {
            Chunk c = new Chunk(cx, cz);
            generator.generate(c);
            c.recalcHeightMap();
            c.recalcLight();
            c.ready = true;
            return c;
        });
    }

    /** Inserts an externally loaded or generated chunk. */
    public void putChunk(Chunk chunk) {
        chunks.put(MathUtil.packXZ(chunk.chunkX, chunk.chunkZ), chunk);
    }

    public Chunk removeChunk(int cx, int cz) {
        return chunks.remove(MathUtil.packXZ(cx, cz));
    }

    public Collection<Chunk> loadedChunks() { return chunks.values(); }

    public int loadedChunkCount() { return chunks.size(); }

    // ---------------- Block access ----------------

    public int getBlockId(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) return 0;
        Chunk c = getLoadedChunk(x >> 4, z >> 4);
        if (c == null) return 0;
        return c.getBlockId(x & 15, y, z & 15);
    }

    public Block getBlock(int x, int y, int z) {
        return BlockRegistry.get(getBlockId(x, y, z));
    }

    public boolean setBlock(int x, int y, int z, int id) {
        return setBlockInternal(x, y, z, id, true, true);
    }

    /** Set block without touching its block entity (furnace lit/unlit swap). */
    public boolean setBlockKeepEntity(int x, int y, int z, int id) {
        return setBlockInternal(x, y, z, id, false, true);
    }

    private boolean setBlockInternal(int x, int y, int z, int id, boolean manageEntity, boolean notify) {
        if (y < 0 || y >= Chunk.SIZE_Y) return false;
        Chunk c = getLoadedChunk(x >> 4, z >> 4);
        if (c == null) return false;
        int lx = x & 15, lz = z & 15;
        int old = c.getBlockId(lx, y, lz);
        if (old == id) return false;
        c.setBlockId(lx, y, lz, id);
        c.modified = true;
        c.recalcLight();
        markDirtyWithNeighbors(c, lx, lz);

        if (manageEntity) {
            if (old == Blocks.CHEST.id || old == Blocks.FURNACE.id || old == Blocks.FURNACE_LIT.id) {
                blockEntities.remove(posKey(x, y, z));
            }
        }
        if (notify) {
            redstone.onBlockChanged(this, x, y, z);
            if (listener != null) listener.onBlockChanged(x, y, z, old, id);
        }
        return true;
    }

    private void markDirtyWithNeighbors(Chunk c, int lx, int lz) {
        c.dirty = true;
        if (lx == 0) markDirty(c.chunkX - 1, c.chunkZ);
        if (lx == 15) markDirty(c.chunkX + 1, c.chunkZ);
        if (lz == 0) markDirty(c.chunkX, c.chunkZ - 1);
        if (lz == 15) markDirty(c.chunkX, c.chunkZ + 1);
    }

    private void markDirty(int cx, int cz) {
        Chunk c = getLoadedChunk(cx, cz);
        if (c != null) c.dirty = true;
    }

    /** Combined light (0..15): max of block light and sky light scaled by daylight. */
    public int getLight(int x, int y, int z, float daylight) {
        if (y < 0) return 0;
        if (y >= Chunk.SIZE_Y) return (int) (15 * daylight);
        Chunk c = getLoadedChunk(x >> 4, z >> 4);
        if (c == null) return (int) (15 * daylight);
        int sky = (int) (c.getSkyLight(x & 15, y, z & 15) * daylight);
        int block = c.getBlockLight(x & 15, y, z & 15);
        return Math.max(sky, block);
    }

    public int surfaceHeight(int x, int z) {
        Chunk c = getLoadedChunk(x >> 4, z >> 4);
        if (c != null && c.ready) return c.getHeight(x & 15, z & 15);
        return generator.surfaceHeight(x, z);
    }

    // ---------------- Block entities ----------------

    public static long posKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return blockEntities.get(posKey(x, y, z));
    }

    public void setBlockEntity(BlockEntity be) {
        blockEntities.put(posKey(be.x, be.y, be.z), be);
    }

    public void removeBlockEntity(int x, int y, int z) {
        blockEntities.remove(posKey(x, y, z));
    }

    public Collection<BlockEntity> blockEntities() { return blockEntities.values(); }

    // ---------------- Entities ----------------

    public void addEntity(Entity e) {
        synchronized (pendingAdd) {
            pendingAdd.add(e);
        }
    }

    /** Snapshot-safe list; only touch from the tick thread. */
    public List<Entity> entities() { return entities; }

    // ---------------- Physics helpers ----------------

    private final List<AABB> collisionScratch = new ArrayList<>(32);
    private final AABB scratchBox = new AABB();

    /** Collects AABBs of collidable blocks overlapping the region (tick thread only). */
    public List<AABB> getCollisions(AABB region) {
        collisionScratch.clear();
        int x0 = MathUtil.floor(region.minX) - 1, x1 = MathUtil.floor(region.maxX) + 1;
        int y0 = Math.max(0, MathUtil.floor(region.minY) - 1), y1 = Math.min(Chunk.SIZE_Y - 1, MathUtil.floor(region.maxY) + 1);
        int z0 = MathUtil.floor(region.minZ) - 1, z1 = MathUtil.floor(region.maxZ) + 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    Block b = getBlock(x, y, z);
                    if (!b.collidable || b.isAir()) continue;
                    AABB box = new AABB(x, y, z, x + 1, y + 1, z + 1);
                    if (box.intersects(region)) collisionScratch.add(box);
                }
            }
        }
        return collisionScratch;
    }

    public boolean isLiquidAt(double x, double y, double z) {
        return getBlock(MathUtil.floor(x), MathUtil.floor(y), MathUtil.floor(z)).liquid;
    }

    public boolean isClimbableAt(double x, double y, double z) {
        return getBlock(MathUtil.floor(x), MathUtil.floor(y), MathUtil.floor(z)).climbable;
    }

    // ---------------- Raycast (Amanatides & Woo DDA) ----------------

    public void raycast(double ox, double oy, double oz,
                        double dx, double dy, double dz,
                        double maxDist, RaycastHit out) {
        out.clear();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-8) return;
        dx /= len; dy /= len; dz /= len;

        int x = MathUtil.floor(ox), y = MathUtil.floor(oy), z = MathUtil.floor(oz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tDeltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dx);
        double tDeltaY = dy == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dy);
        double tDeltaZ = dz == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dz);
        double tMaxX = dx == 0 ? Double.MAX_VALUE : ((dx > 0 ? (x + 1 - ox) : (ox - x)) * tDeltaX);
        double tMaxY = dy == 0 ? Double.MAX_VALUE : ((dy > 0 ? (y + 1 - oy) : (oy - y)) * tDeltaY);
        double tMaxZ = dz == 0 ? Double.MAX_VALUE : ((dz > 0 ? (z + 1 - oz) : (oz - z)) * tDeltaZ);

        int faceX = 0, faceY = 0, faceZ = 0;
        double t = 0;
        while (t <= maxDist) {
            Block b = getBlock(x, y, z);
            if (!b.isAir() && !b.liquid) {
                out.hit = true;
                out.x = x; out.y = y; out.z = z;
                out.faceX = faceX; out.faceY = faceY; out.faceZ = faceZ;
                out.distance = t;
                return;
            }
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX; t = tMaxX; tMaxX += tDeltaX;
                faceX = -stepX; faceY = 0; faceZ = 0;
            } else if (tMaxY < tMaxZ) {
                y += stepY; t = tMaxY; tMaxY += tDeltaY;
                faceX = 0; faceY = -stepY; faceZ = 0;
            } else {
                z += stepZ; t = tMaxZ; tMaxZ += tDeltaZ;
                faceX = 0; faceY = 0; faceZ = -stepZ;
            }
        }
    }

    // ---------------- Ticking ----------------

    /** Advances time, weather, block entities and entities by one tick (1/20 s). */
    public void tick(double playerX, double playerY, double playerZ, float activationRange) {
        time++;
        explosionHappened = false;
        itemPickedUp = false;
        redstone.tick(this);

        // Weather only changes in the overworld.
        if (dimension == DIM_OVERWORLD) {
            weatherTicksLeft--;
            if (weatherTicksLeft <= 0) {
                int roll = rand.nextInt(100);
                if (weather == Weather.CLEAR) {
                    weather = roll < 70 ? Weather.RAIN : Weather.THUNDER;
                    weatherTicksLeft = 2400 + rand.nextInt(7200);
                } else {
                    weather = Weather.CLEAR;
                    weatherTicksLeft = 12000 + rand.nextInt(24000);
                }
            }
        }

        for (BlockEntity be : blockEntities.values()) {
            be.tick(this);
        }

        synchronized (pendingAdd) {
            entities.addAll(pendingAdd);
            pendingAdd.clear();
        }
        float actSq = activationRange * activationRange;
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity e = entities.get(i);
            double dSq = MathUtil.distSq(e.x, e.y, e.z, playerX, playerY, playerZ);
            if (dSq <= actSq) {
                e.tick(this);
            }
            if (e.dead) entities.remove(i);
        }
    }

    /** 0..1 daylight factor from world time. */
    public float daylight() {
        long t = time % DAY_LENGTH;
        // 0..12000 day, 12000..13500 dusk, 13500..22500 night, 22500..24000 dawn
        if (t < 12000) return 1f;
        if (t < 13500) return 1f - (t - 12000) / 1500f * 0.85f;
        if (t < 22500) return 0.15f;
        return 0.15f + (t - 22500) / 1500f * 0.85f;
    }

    public boolean isNight() {
        long t = time % DAY_LENGTH;
        return t >= 13000 && t < 23000;
    }

    public Random random() { return rand; }
}
