package com.craftworld3d.core.world.gen;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.noise.PerlinNoise;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.ChunkGenerator;
import com.craftworld3d.core.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic overworld generator: continents, mountains, rivers, ten biomes,
 * caves, ores, trees (features are seeded per chunk and may cross chunk borders),
 * villages, ruins and surface decoration.
 */
public final class OverworldGenerator implements ChunkGenerator {
    private final long seed;
    private final PerlinNoise heightNoise, mountainNoise, detailNoise;
    private final PerlinNoise tempNoise, humidNoise, caveNoise, riverNoise;
    private final VillagePlanner villages;

    public OverworldGenerator(long seed) {
        this.seed = seed;
        heightNoise = new PerlinNoise(seed);
        mountainNoise = new PerlinNoise(seed + 1);
        detailNoise = new PerlinNoise(seed + 2);
        tempNoise = new PerlinNoise(seed + 3);
        humidNoise = new PerlinNoise(seed + 4);
        caveNoise = new PerlinNoise(seed + 5);
        riverNoise = new PerlinNoise(seed + 6);
        villages = new VillagePlanner(seed);
    }

    // ---------------- Pure column functions ----------------

    @Override
    public int surfaceHeight(int x, int z) {
        double cont = heightNoise.fbm2(x * 0.0016, z * 0.0016, 4, 2.0, 0.5);
        double base = 62 + cont * 24;
        double landness = clamp((base - 58) / 12.0, 0, 1);
        double mount = Math.max(0, mountainNoise.fbm2(x * 0.004, z * 0.004, 4, 2.0, 0.5) - 0.22) * 110 * landness;
        double detail = detailNoise.fbm2(x * 0.02, z * 0.02, 3, 2.0, 0.5) * 5;
        double h = base + mount + detail;

        // Rivers carve a channel through land.
        double r = riverNoise.ridge2(x * 0.0019, z * 0.0019);
        if (r < 0.028 && h > 54 && h < 95) {
            double depth = (0.028 - r) / 0.028; // 0..1
            double target = 58 - depth * 3;
            h = Math.min(h, lerp(h, target, Math.min(1, depth * 2.2)));
        }
        return (int) clamp(h, 8, 240);
    }

    public boolean isRiver(int x, int z) {
        double r = riverNoise.ridge2(x * 0.0019, z * 0.0019);
        return r < 0.028;
    }

    public BiomeType biomeAt(int x, int z) {
        int h = surfaceHeight(x, z);
        double temp = tempNoise.fbm2(x * 0.0011, z * 0.0011, 3, 2.0, 0.5) - Math.max(0, h - 80) * 0.004;
        double humid = humidNoise.fbm2(x * 0.0013, z * 0.0013, 3, 2.0, 0.5);

        if (h < World.SEA_LEVEL - 3) return BiomeType.OCEAN;
        if (h <= World.SEA_LEVEL - 1 && isRiver(x, z)) return BiomeType.RIVER;
        if (h <= World.SEA_LEVEL + 1) return BiomeType.BEACH;
        if (h > 108) return BiomeType.MOUNTAINS;
        if (temp < -0.42) return BiomeType.SNOW;
        if (temp < -0.18) return BiomeType.TAIGA;
        if (temp > 0.42 && humid < 0.05) return BiomeType.DESERT;
        if (humid > 0.42 && temp > 0.05 && h < 70) return BiomeType.SWAMP;
        if (humid > 0.05) return BiomeType.FOREST;
        return BiomeType.PLAINS;
    }

    // ---------------- Chunk generation ----------------

    @Override
    public void generate(Chunk chunk) {
        int cx = chunk.chunkX, cz = chunk.chunkZ;
        int baseX = cx << 4, baseZ = cz << 4;

        int[] heights = new int[256];
        BiomeType[] biomes = new BiomeType[256];

        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int wx = baseX + lx, wz = baseZ + lz;
                int h = surfaceHeight(wx, wz);
                BiomeType biome = biomeAt(wx, wz);
                heights[(lz << 4) | lx] = h;
                biomes[(lz << 4) | lx] = biome;
                fillColumn(chunk, lx, lz, wx, wz, h, biome);
            }
        }

        placeOres(chunk);
        placeFeatures(chunk, baseX, baseZ);
        villages.generate(chunk, this);
        placeRuins(chunk);
        decorate(chunk, heights, biomes);
    }

    private void fillColumn(Chunk chunk, int lx, int lz, int wx, int wz, int h, BiomeType biome) {
        chunk.setBlockId(lx, 0, lz, Blocks.BEDROCK.id);
        for (int y = 1; y <= h; y++) {
            int id;
            if (y > h - 4) {
                id = surfaceBlock(biome, y, h);
            } else {
                id = Blocks.STONE.id;
            }
            // Caves.
            if (y > 5 && y < h - 2 && isCave(wx, y, wz)) {
                id = y < 11 ? Blocks.LAVA.id : 0;
            }
            if (id != 0) chunk.setBlockId(lx, y, lz, id);
        }
        // Water fill.
        if (h < World.SEA_LEVEL) {
            for (int y = h + 1; y <= World.SEA_LEVEL; y++) {
                chunk.setBlockId(lx, y, lz, Blocks.WATER.id);
            }
            if (biome == BiomeType.SNOW || biome == BiomeType.TAIGA) {
                chunk.setBlockId(lx, World.SEA_LEVEL, lz, Blocks.ICE.id);
            }
        }
        // Swamp shallow pools.
        if (biome == BiomeType.SWAMP && h == World.SEA_LEVEL + 1
            && detailNoise.noise2(wx * 0.09, wz * 0.09) > 0.35) {
            chunk.setBlockId(lx, h, lz, Blocks.WATER.id);
        }
    }

    private boolean isCave(int x, int y, int z) {
        double n = caveNoise.fbm3(x * 0.045, y * 0.07, z * 0.045, 2, 2.0, 0.5);
        return n > 0.56;
    }

    private int surfaceBlock(BiomeType biome, int y, int h) {
        boolean top = y == h;
        switch (biome) {
            case DESERT:
                return top || y > h - 3 ? Blocks.SAND.id : Blocks.SANDSTONE.id;
            case BEACH:
                return Blocks.SAND.id;
            case OCEAN:
                return y >= h - 1 ? (((y + h) & 3) == 0 ? Blocks.GRAVEL.id : Blocks.SAND.id) : Blocks.DIRT.id;
            case RIVER:
                return y >= h - 1 ? Blocks.SAND.id : Blocks.DIRT.id;
            case SNOW:
                return top ? Blocks.SNOW_BLOCK.id : Blocks.DIRT.id;
            case MOUNTAINS:
                if (h > 125 && top) return Blocks.SNOW_BLOCK.id;
                return Blocks.STONE.id;
            case SWAMP:
                return top ? Blocks.GRASS.id : (((y * 31) & 7) == 0 ? Blocks.CLAY.id : Blocks.DIRT.id);
            default:
                return top ? Blocks.GRASS.id : Blocks.DIRT.id;
        }
    }

    // ---------------- Ores ----------------

    private long chunkSeed(int cx, int cz, long salt) {
        return seed ^ (cx * 341873128712L + cz * 132897987541L + salt);
    }

    private void placeOres(Chunk chunk) {
        Random rnd = new Random(chunkSeed(chunk.chunkX, chunk.chunkZ, 777));
        vein(chunk, rnd, Blocks.COAL_ORE.id, 18, 10, 120, 8);
        vein(chunk, rnd, Blocks.IRON_ORE.id, 12, 5, 64, 6);
        vein(chunk, rnd, Blocks.COPPER_ORE.id, 9, 20, 90, 6);
        vein(chunk, rnd, Blocks.ALUMINUM_ORE.id, 7, 30, 96, 5);
        vein(chunk, rnd, Blocks.GOLD_ORE.id, 3, 5, 32, 5);
        vein(chunk, rnd, Blocks.REDSTONE_ORE.id, 6, 5, 16, 6);
        vein(chunk, rnd, Blocks.DIAMOND_ORE.id, 2, 4, 16, 5);
        vein(chunk, rnd, Blocks.EMERALD_ORE.id, 1, 4, 32, 3);
    }

    private void vein(Chunk chunk, Random rnd, int blockId, int tries, int minY, int maxY, int size) {
        for (int i = 0; i < tries; i++) {
            int x = rnd.nextInt(16), z = rnd.nextInt(16);
            int y = minY + rnd.nextInt(Math.max(1, maxY - minY));
            int n = 1 + rnd.nextInt(size);
            for (int j = 0; j < n; j++) {
                int ox = x + rnd.nextInt(3) - 1, oy = y + rnd.nextInt(3) - 1, oz = z + rnd.nextInt(3) - 1;
                if (Chunk.inBounds(ox, oy, oz) && chunk.getBlockId(ox, oy, oz) == Blocks.STONE.id) {
                    chunk.setBlockId(ox, oy, oz, blockId);
                }
            }
        }
    }

    // ---------------- Trees & cross-chunk features ----------------

    /** A tree feature: world position, type and trunk height. */
    record Tree(int x, int z, int type, int trunk) {
        static final int OAK = 0, BIRCH = 1, SPRUCE = 2, RUBBER = 3, CACTUS = 4;
    }

    List<Tree> treesFor(int cx, int cz) {
        Random rnd = new Random(chunkSeed(cx, cz, 99));
        List<Tree> list = new ArrayList<>();
        // Sample biome at chunk center to decide density.
        int centerX = (cx << 4) + 8, centerZ = (cz << 4) + 8;
        BiomeType biome = biomeAt(centerX, centerZ);
        int count;
        switch (biome) {
            case FOREST -> count = 5 + rnd.nextInt(3);
            case TAIGA -> count = 4 + rnd.nextInt(3);
            case SWAMP -> count = 2 + rnd.nextInt(2);
            case PLAINS -> count = rnd.nextInt(2);
            case DESERT -> count = 1 + rnd.nextInt(2);
            case SNOW -> count = rnd.nextInt(2);
            default -> count = 0;
        }
        for (int i = 0; i < count; i++) {
            int x = (cx << 4) + rnd.nextInt(16);
            int z = (cz << 4) + rnd.nextInt(16);
            int trunk = 4 + rnd.nextInt(3);
            int type;
            switch (biome) {
                case TAIGA, SNOW -> type = Tree.SPRUCE;
                case DESERT -> type = Tree.CACTUS;
                case SWAMP -> type = rnd.nextInt(3) == 0 ? Tree.RUBBER : Tree.OAK;
                case FOREST -> {
                    int roll = rnd.nextInt(10);
                    type = roll < 5 ? Tree.OAK : roll < 8 ? Tree.BIRCH : Tree.RUBBER;
                }
                default -> type = Tree.OAK;
            }
            list.add(new Tree(x, z, type, trunk));
        }
        return list;
    }

    private void placeFeatures(Chunk chunk, int baseX, int baseZ) {
        for (int dcz = -1; dcz <= 1; dcz++) {
            for (int dcx = -1; dcx <= 1; dcx++) {
                for (Tree t : treesFor(chunk.chunkX + dcx, chunk.chunkZ + dcz)) {
                    placeTree(chunk, baseX, baseZ, t);
                }
            }
        }
    }

    private void placeTree(Chunk chunk, int baseX, int baseZ, Tree t) {
        int ground = surfaceHeight(t.x, t.z);
        if (ground <= World.SEA_LEVEL) return; // no trees in water
        BiomeType biome = biomeAt(t.x, t.z);
        if (biome == BiomeType.OCEAN || biome == BiomeType.RIVER || biome == BiomeType.BEACH) return;

        if (t.type == Tree.CACTUS) {
            for (int y = 1; y <= 3; y++) {
                setWorldBlock(chunk, baseX, baseZ, t.x, ground + y, t.z, Blocks.CACTUS.id);
            }
            return;
        }
        int logId = switch (t.type) {
            case Tree.BIRCH -> Blocks.BIRCH_LOG.id;
            case Tree.SPRUCE -> Blocks.SPRUCE_LOG.id;
            case Tree.RUBBER -> Blocks.RUBBER_LOG.id;
            default -> Blocks.OAK_LOG.id;
        };
        int leafId = switch (t.type) {
            case Tree.BIRCH -> Blocks.BIRCH_LEAVES.id;
            case Tree.SPRUCE -> Blocks.SPRUCE_LEAVES.id;
            case Tree.RUBBER -> Blocks.RUBBER_LEAVES.id;
            default -> Blocks.OAK_LEAVES.id;
        };
        // Canopy.
        int topY = ground + t.trunk;
        for (int dy = -2; dy <= 1; dy++) {
            int radius = dy <= -1 ? 2 : 1;
            if (t.type == Tree.SPRUCE) radius = dy <= -1 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && dy >= 0) continue;
                    setWorldBlockIfAir(chunk, baseX, baseZ, t.x + dx, topY + dy + 1, t.z + dz, leafId);
                }
            }
        }
        setWorldBlockIfAir(chunk, baseX, baseZ, t.x, topY + 2, t.z, leafId);
        // Trunk.
        for (int y = 1; y <= t.trunk; y++) {
            setWorldBlock(chunk, baseX, baseZ, t.x, ground + y, t.z, logId);
        }
    }

    private void setWorldBlock(Chunk chunk, int baseX, int baseZ, int wx, int wy, int wz, int id) {
        int lx = wx - baseX, lz = wz - baseZ;
        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && wy >= 0 && wy < Chunk.SIZE_Y) {
            chunk.setBlockId(lx, wy, lz, id);
        }
    }

    private void setWorldBlockIfAir(Chunk chunk, int baseX, int baseZ, int wx, int wy, int wz, int id) {
        int lx = wx - baseX, lz = wz - baseZ;
        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && wy >= 0 && wy < Chunk.SIZE_Y
            && chunk.getBlockId(lx, wy, lz) == 0) {
            chunk.setBlockId(lx, wy, lz, id);
        }
    }

    // ---------------- Ruins ----------------

    private void placeRuins(Chunk chunk) {
        Random rnd = new Random(chunkSeed(chunk.chunkX, chunk.chunkZ, 4242));
        if (rnd.nextInt(50) != 0) return;
        int lx = 3 + rnd.nextInt(8), lz = 3 + rnd.nextInt(8);
        int wx = (chunk.chunkX << 4) + lx, wz = (chunk.chunkZ << 4) + lz;
        BiomeType biome = biomeAt(wx, wz);
        if (biome == BiomeType.OCEAN || biome == BiomeType.RIVER) return;
        int ground = surfaceHeight(wx, wz);
        int size = 3 + rnd.nextInt(3);
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                if (dx != 0 && dx != size - 1 && dz != 0 && dz != size - 1) continue;
                int wallH = rnd.nextInt(3); // broken walls
                for (int dy = 1; dy <= wallH; dy++) {
                    int id = rnd.nextInt(3) == 0 ? Blocks.MOSSY_STONE_BRICKS.id : Blocks.STONE_BRICKS.id;
                    if (Chunk.inBounds(lx + dx, ground + dy, lz + dz)) {
                        chunk.setBlockId(lx + dx, ground + dy, lz + dz, id);
                    }
                }
            }
        }
    }

    // ---------------- Decoration ----------------

    private void decorate(Chunk chunk, int[] heights, BiomeType[] biomes) {
        Random rnd = new Random(chunkSeed(chunk.chunkX, chunk.chunkZ, 31337));
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int h = heights[(lz << 4) | lx];
                BiomeType biome = biomes[(lz << 4) | lx];
                if (h >= Chunk.SIZE_Y - 2) continue;
                int above = chunk.getBlockId(lx, h + 1, lz);
                int surface = chunk.getBlockId(lx, h, lz);
                if (above != 0) continue;
                if (surface == Blocks.GRASS.id) {
                    int roll = rnd.nextInt(100);
                    if (biome == BiomeType.PLAINS || biome == BiomeType.FOREST) {
                        if (roll < 8) chunk.setBlockId(lx, h + 1, lz, Blocks.TALL_GRASS.id);
                        else if (roll < 10) chunk.setBlockId(lx, h + 1, lz,
                            rnd.nextBoolean() ? Blocks.FLOWER_YELLOW.id : Blocks.FLOWER_RED.id);
                    } else if (biome == BiomeType.SWAMP && roll < 12) {
                        chunk.setBlockId(lx, h + 1, lz, Blocks.TALL_GRASS.id);
                    }
                } else if (surface == Blocks.SAND.id && biome == BiomeType.DESERT) {
                    if (rnd.nextInt(100) < 2) chunk.setBlockId(lx, h + 1, lz, Blocks.DEAD_BUSH.id);
                }
            }
        }
    }

    /** Deterministic villager spawn positions for a chunk (empty for most chunks). */
    public List<double[]> villagerSpawns(int cx, int cz) {
        return villages.villagerSpawns(cx, cz, this);
    }

    public VillagePlanner villagePlanner() { return villages; }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
