package com.craftworld3d.core.world.gen;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.world.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic village placement and construction.
 *
 * The world is divided into 16x16-chunk regions; some regions contain one
 * village (houses, roads, farms, a storage house with a chest, and villagers).
 * Village pieces are computed as a pure function of the seed so any chunk can
 * build exactly its slice of a village that crosses chunk borders.
 */
public final class VillagePlanner {
    public static final int REGION_CHUNKS = 16; // 256 blocks

    private final long seed;

    public VillagePlanner(long seed) {
        this.seed = seed;
    }

    /** Village plan for a region, or null. */
    public Plan planFor(int regionX, int regionZ, OverworldGenerator gen) {
        Random rnd = new Random(seed ^ (regionX * 65537L + regionZ * 92821L + 5555));
        if (rnd.nextInt(100) >= 34) return null; // ~1/3 of regions

        int centerX = regionX * REGION_CHUNKS * 16 + 64 + rnd.nextInt(128);
        int centerZ = regionZ * REGION_CHUNKS * 16 + 64 + rnd.nextInt(128);

        BiomeType biome = gen.biomeAt(centerX, centerZ);
        if (biome != BiomeType.PLAINS && biome != BiomeType.FOREST && biome != BiomeType.DESERT) return null;
        int ground = gen.surfaceHeight(centerX, centerZ);
        if (ground <= 62 || ground > 90) return null;

        Plan plan = new Plan(centerX, centerZ, biome == BiomeType.DESERT);
        // Cross roads through the center.
        plan.pieces.add(Piece.road(centerX - 24, centerZ - 1, 48, 3));
        plan.pieces.add(Piece.road(centerX - 1, centerZ - 24, 3, 48));

        // Houses along the roads.
        int houses = 3 + rnd.nextInt(3);
        for (int i = 0; i < houses; i++) {
            int side = rnd.nextBoolean() ? 1 : -1;
            boolean alongX = rnd.nextBoolean();
            int off = -20 + rnd.nextInt(34);
            int hx = alongX ? centerX + off : centerX + side * (4 + rnd.nextInt(4));
            int hz = alongX ? centerZ + side * (4 + rnd.nextInt(4)) : centerZ + off;
            plan.pieces.add(Piece.house(hx, hz, i == 0)); // first house holds the storage chest
        }
        // Farms.
        int farms = 1 + rnd.nextInt(2);
        for (int i = 0; i < farms; i++) {
            int fx = centerX + (rnd.nextBoolean() ? 6 : -13) + rnd.nextInt(5);
            int fz = centerZ + (rnd.nextBoolean() ? 8 : -15) + rnd.nextInt(5);
            plan.pieces.add(Piece.farm(fx, fz));
        }
        // Well at the center.
        plan.pieces.add(Piece.well(centerX + 2, centerZ + 2));
        return plan;
    }

    public void generate(Chunk chunk, OverworldGenerator gen) {
        int baseX = chunk.chunkX << 4, baseZ = chunk.chunkZ << 4;
        // A village's pieces stay within its region plus a margin; check 3x3 regions.
        int regionX = Math.floorDiv(chunk.chunkX, REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunk.chunkZ, REGION_CHUNKS);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Plan plan = planFor(regionX + dx, regionZ + dz, gen);
                if (plan == null) continue;
                for (Piece piece : plan.pieces) {
                    piece.build(chunk, baseX, baseZ, gen, plan.desert);
                }
            }
        }
    }

    /** Deterministic villager spawn points whose anchor house center lies in this chunk. */
    public List<double[]> villagerSpawns(int cx, int cz, OverworldGenerator gen) {
        List<double[]> out = new ArrayList<>();
        int regionX = Math.floorDiv(cx, REGION_CHUNKS);
        int regionZ = Math.floorDiv(cz, REGION_CHUNKS);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                Plan plan = planFor(regionX + dx, regionZ + dz, gen);
                if (plan == null) continue;
                for (Piece piece : plan.pieces) {
                    if (piece.kind != Piece.Kind.HOUSE) continue;
                    int hcx = (piece.x + piece.w / 2) >> 4, hcz = (piece.z + piece.d / 2) >> 4;
                    if (hcx == cx && hcz == cz) {
                        int ground = gen.surfaceHeight(piece.x + piece.w / 2, piece.z + piece.d / 2);
                        out.add(new double[]{piece.x + piece.w / 2.0, ground + 1.5, piece.z + piece.d / 2.0});
                    }
                }
            }
        }
        return out;
    }

    public static final class Plan {
        public final int centerX, centerZ;
        public final boolean desert;
        public final List<Piece> pieces = new ArrayList<>();

        Plan(int centerX, int centerZ, boolean desert) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.desert = desert;
        }
    }

    /** A village structure piece with a footprint and a per-cell block function. */
    public static final class Piece {
        public enum Kind { ROAD, HOUSE, FARM, WELL }

        public final Kind kind;
        public final int x, z, w, d; // footprint in world coords
        public final boolean withChest;

        private Piece(Kind kind, int x, int z, int w, int d, boolean withChest) {
            this.kind = kind;
            this.x = x; this.z = z; this.w = w; this.d = d;
            this.withChest = withChest;
        }

        static Piece road(int x, int z, int w, int d) { return new Piece(Kind.ROAD, x, z, w, d, false); }
        static Piece house(int x, int z, boolean chest) { return new Piece(Kind.HOUSE, x, z, 7, 7, chest); }
        static Piece farm(int x, int z) { return new Piece(Kind.FARM, x, z, 7, 5, false); }
        static Piece well(int x, int z) { return new Piece(Kind.WELL, x, z, 3, 3, false); }

        void build(Chunk chunk, int baseX, int baseZ, OverworldGenerator gen, boolean desert) {
            // Quick footprint rejection.
            if (x + w < baseX || x > baseX + 15 || z + d < baseZ || z > baseZ + 15) return;
            switch (kind) {
                case ROAD -> buildRoad(chunk, baseX, baseZ, gen);
                case HOUSE -> buildHouse(chunk, baseX, baseZ, gen, desert);
                case FARM -> buildFarm(chunk, baseX, baseZ, gen);
                case WELL -> buildWell(chunk, baseX, baseZ, gen);
            }
        }

        private void buildRoad(Chunk chunk, int baseX, int baseZ, OverworldGenerator gen) {
            for (int wx = Math.max(x, baseX); wx < Math.min(x + w, baseX + 16); wx++) {
                for (int wz = Math.max(z, baseZ); wz < Math.min(z + d, baseZ + 16); wz++) {
                    int ground = gen.surfaceHeight(wx, wz);
                    if (ground <= 61 || ground > 100) continue;
                    set(chunk, wx - baseX, ground, wz - baseZ, Blocks.GRAVEL.id);
                    // Clear plants above the road.
                    int above = chunk.getBlockId(wx - baseX, ground + 1, wz - baseZ);
                    if (above != 0 && !com.craftworld3d.core.block.BlockRegistry.get(above).isOpaqueCube()) {
                        set(chunk, wx - baseX, ground + 1, wz - baseZ, 0);
                    }
                }
            }
        }

        private void buildHouse(Chunk chunk, int baseX, int baseZ, OverworldGenerator gen, boolean desert) {
            int cxw = x + w / 2, czw = z + d / 2;
            int base = gen.surfaceHeight(cxw, czw);
            if (base <= 61 || base > 100) return;
            int wallId = desert ? Blocks.SANDSTONE.id : Blocks.OAK_PLANKS.id;
            int cornerId = desert ? Blocks.SANDSTONE.id : Blocks.OAK_LOG.id;
            int floorId = Blocks.COBBLESTONE.id;
            int roofId = desert ? Blocks.SANDSTONE.id : Blocks.SPRUCE_PLANKS.id;
            int doorWX = x + w / 2, doorWZ = z; // door on -Z side

            for (int wx = x; wx < x + w; wx++) {
                for (int wz = z; wz < z + d; wz++) {
                    int lx = wx - baseX, lz = wz - baseZ;
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) continue;
                    boolean edge = wx == x || wx == x + w - 1 || wz == z || wz == z + d - 1;
                    boolean corner = (wx == x || wx == x + w - 1) && (wz == z || wz == z + d - 1);

                    // Foundation down to terrain.
                    for (int y = base - 3; y <= base; y++) set(chunk, lx, y, lz, floorId);
                    // Clear interior volume.
                    for (int y = base + 1; y <= base + 4; y++) set(chunk, lx, y, lz, 0);
                    // Walls.
                    if (edge) {
                        for (int y = base + 1; y <= base + 3; y++) {
                            int id = corner ? cornerId : wallId;
                            // Windows at eye level on side walls.
                            if (!corner && y == base + 2 && ((wx - x) % 3 == 1 || (wz - z) % 3 == 1)) {
                                id = Blocks.GLASS.id;
                            }
                            set(chunk, lx, y, lz, id);
                        }
                    }
                    // Roof.
                    set(chunk, lx, base + 4, lz, roofId);
                }
            }
            // Door opening.
            if (doorWX >= baseX && doorWX <= baseX + 15 && doorWZ >= baseZ && doorWZ <= baseZ + 15) {
                set(chunk, doorWX - baseX, base + 1, doorWZ - baseZ, Blocks.DOOR_CLOSED.id);
                set(chunk, doorWX - baseX, base + 2, doorWZ - baseZ, 0);
            }
            // Torch inside.
            int tx = x + 1, tz = z + 1;
            if (tx >= baseX && tx <= baseX + 15 && tz >= baseZ && tz <= baseZ + 15) {
                set(chunk, tx - baseX, base + 1, tz - baseZ, Blocks.TORCH.id);
            }
            // Storage chest.
            if (withChest) {
                int chx = x + w - 2, chz = z + d - 2;
                if (chx >= baseX && chx <= baseX + 15 && chz >= baseZ && chz <= baseZ + 15) {
                    set(chunk, chx - baseX, base + 1, chz - baseZ, Blocks.CHEST.id);
                }
            }
        }

        private void buildFarm(Chunk chunk, int baseX, int baseZ, OverworldGenerator gen) {
            int base = gen.surfaceHeight(x + w / 2, z + d / 2);
            if (base <= 61 || base > 100) return;
            for (int wx = x; wx < x + w; wx++) {
                for (int wz = z; wz < z + d; wz++) {
                    int lx = wx - baseX, lz = wz - baseZ;
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) continue;
                    boolean edge = wx == x || wx == x + w - 1 || wz == z || wz == z + d - 1;
                    boolean channel = wx == x + w / 2;
                    for (int y = base + 1; y <= base + 3; y++) set(chunk, lx, y, lz, 0);
                    if (edge) {
                        set(chunk, lx, base, lz, Blocks.OAK_LOG.id);
                    } else if (channel) {
                        set(chunk, lx, base, lz, Blocks.WATER.id);
                        set(chunk, lx, base - 1, lz, Blocks.DIRT.id);
                    } else {
                        set(chunk, lx, base, lz, Blocks.FARMLAND.id);
                        set(chunk, lx, base + 1, lz, Blocks.WHEAT_CROP.id);
                    }
                }
            }
        }

        private void buildWell(Chunk chunk, int baseX, int baseZ, OverworldGenerator gen) {
            int base = gen.surfaceHeight(x + 1, z + 1);
            if (base <= 61 || base > 100) return;
            for (int wx = x; wx < x + w; wx++) {
                for (int wz = z; wz < z + d; wz++) {
                    int lx = wx - baseX, lz = wz - baseZ;
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) continue;
                    boolean center = wx == x + 1 && wz == z + 1;
                    set(chunk, lx, base, lz, center ? Blocks.WATER.id : Blocks.COBBLESTONE.id);
                    set(chunk, lx, base + 1, lz, center ? 0 : Blocks.COBBLESTONE.id);
                }
            }
        }

        private static void set(Chunk chunk, int lx, int y, int lz, int id) {
            if (Chunk.inBounds(lx, y, lz)) chunk.setBlockId(lx, y, lz, id);
        }
    }
}
