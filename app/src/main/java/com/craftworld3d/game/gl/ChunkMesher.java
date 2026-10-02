package com.craftworld3d.game.gl;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.BlockRegistry;
import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.block.RenderShape;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.World;

/**
 * Builds triangle meshes for one chunk on a background thread.
 *
 * Vertex layout: x,y,z, u,v, skyLight,blockLight  (7 floats, 6 verts/quad).
 * Hidden-face culling: faces against opaque cubes are skipped. Two passes:
 * opaque (alpha-tested) and translucent (water/ice/portal, blended).
 */
public final class ChunkMesher {
    public static final int FLOATS_PER_VERT = 7;

    /** Result of meshing one chunk. */
    public static final class MeshData {
        public long key;
        public int chunkX, chunkZ;
        public float[] opaque;
        public int opaqueVerts;
        public float[] translucent;
        public int translucentVerts;
    }

    private final TextureAtlasGL atlas;
    private FloatArr op = new FloatArr(16384);
    private FloatArr tr = new FloatArr(2048);

    public ChunkMesher(TextureAtlasGL atlas) {
        this.atlas = atlas;
    }

    public MeshData mesh(World world, Chunk chunk) {
        op.size = 0;
        tr.size = 0;
        int baseX = chunk.chunkX << 4;
        int baseZ = chunk.chunkZ << 4;
        for (int y = 0; y < Chunk.SIZE_Y; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int id = chunk.getBlockId(x, y, z);
                    if (id == 0) continue;
                    Block b = BlockRegistry.get(id);
                    if (b == null || b.shape == RenderShape.NONE) continue;
                    int wx = baseX + x, wz = baseZ + z;
                    switch (b.shape) {
                        case CUBE -> cube(world, b, wx, y, wz);
                        case CROSS -> cross(world, b, wx, y, wz);
                        case FLAT -> flat(world, b, wx, y, wz);
                        case SLAB -> slab(world, b, wx, y, wz);
                        default -> {}
                    }
                }
            }
        }
        MeshData m = new MeshData();
        m.chunkX = chunk.chunkX;
        m.chunkZ = chunk.chunkZ;
        m.key = key(chunk.chunkX, chunk.chunkZ);
        m.opaqueVerts = op.size / FLOATS_PER_VERT;
        m.opaque = op.trimmed();
        m.translucentVerts = tr.size / FLOATS_PER_VERT;
        m.translucent = tr.trimmed();
        return m;
    }

    public static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    // ---------------- shapes ----------------

    private void cube(World w, Block b, int x, int y, int z) {
        boolean translucent = isTranslucent(b);
        FloatArr out = translucent ? tr : op;
        float topY = 1f;
        if (b.liquid && !sameLiquid(w, b, x, y + 1, z)) topY = 14f / 16f;

        int side = atlas.sideTile[b.id];
        int top = atlas.topTile[b.id];
        int bottom = atlas.bottomTile[b.id];

        // +Y
        if (showFace(w, b, x, y + 1, z)) {
            float l = light(w, b, x, y + 1, z);
            quad(out, top, l, 1.00f,
                    x, y + topY, z + 1, x + 1, y + topY, z + 1,
                    x + 1, y + topY, z, x, y + topY, z, false);
        }
        // -Y
        if (y > 0 && showFace(w, b, x, y - 1, z)) {
            float l = light(w, b, x, y - 1, z);
            quad(out, bottom, l, 0.55f,
                    x, y, z, x + 1, y, z,
                    x + 1, y, z + 1, x, y, z + 1, false);
        }
        // +X
        if (showFace(w, b, x + 1, y, z)) {
            float l = light(w, b, x + 1, y, z);
            quadSide(out, side, l, 0.75f,
                    x + 1, z + 1, x + 1, z, y, topY, false);
        }
        // -X
        if (showFace(w, b, x - 1, y, z)) {
            float l = light(w, b, x - 1, y, z);
            quadSide(out, side, l, 0.75f,
                    x, z, x, z + 1, y, topY, false);
        }
        // +Z
        if (showFace(w, b, x, y, z + 1)) {
            float l = light(w, b, x, y, z + 1);
            quadSide(out, side, l, 0.85f,
                    x, z + 1, x + 1, z + 1, y, topY, false);
        }
        // -Z
        if (showFace(w, b, x, y, z - 1)) {
            float l = light(w, b, x, y, z - 1);
            quadSide(out, side, l, 0.85f,
                    x + 1, z, x, z, y, topY, false);
        }
    }

    private void cross(World w, Block b, int x, int y, int z) {
        int tile = atlas.sideTile[b.id];
        float l = light(w, b, x, y, z);
        float a = 0.15f, c = 0.85f;
        // two diagonal quads, each emitted double-sided
        quad(op, tile, l, 1f,
                x + a, y, z + a, x + c, y, z + c,
                x + c, y + 1, z + c, x + a, y + 1, z + a, true);
        quad(op, tile, l, 1f,
                x + a, y, z + c, x + c, y, z + a,
                x + c, y + 1, z + a, x + a, y + 1, z + c, true);
    }

    private void flat(World w, Block b, int x, int y, int z) {
        int tile = atlas.topTile[b.id];
        float l = light(w, b, x, y, z);
        float h = y + 1f / 16f;
        quad(op, tile, l, 1f,
                x, h, z + 1, x + 1, h, z + 1,
                x + 1, h, z, x, h, z, true);
    }

    private void slab(World w, Block b, int x, int y, int z) {
        // thin panel (used for doors): 3/16 thick on the -Z side of the cell
        int tile = atlas.sideTile[b.id];
        float l = light(w, b, x, y, z);
        float t = 3f / 16f;
        // front/back
        quad(op, tile, l, 0.85f,
                x + 1, y, z, x, y, z,
                x, y + 1, z, x + 1, y + 1, z, false);
        quad(op, tile, l, 0.85f,
                x, y, z + t, x + 1, y, z + t,
                x + 1, y + 1, z + t, x, y + 1, z + t, false);
        // edges
        quad(op, tile, l, 0.75f,
                x, y, z, x, y, z + t,
                x, y + 1, z + t, x, y + 1, z, false);
        quad(op, tile, l, 0.75f,
                x + 1, y, z + t, x + 1, y, z,
                x + 1, y + 1, z, x + 1, y + 1, z + t, false);
        quad(op, tile, l, 1f,
                x, y + 1, z + t, x + 1, y + 1, z + t,
                x + 1, y + 1, z, x, y + 1, z, false);
    }

    // ---------------- helpers ----------------

    private static boolean isTranslucent(Block b) {
        return b.liquid || b.id == Blocks.ICE.id || b.id == Blocks.PORTAL.id;
    }

    private boolean sameLiquid(World w, Block b, int x, int y, int z) {
        return w.getBlockId(x, y, z) == b.id;
    }

    /** Should the face towards (nx,ny,nz) be drawn? */
    private boolean showFace(World w, Block self, int nx, int ny, int nz) {
        if (ny >= Chunk.SIZE_Y) return true;
        if (ny < 0) return false;
        Block n = BlockRegistry.get(w.getBlockId(nx, ny, nz));
        if (n == null || n.isAir()) return true;
        if (n.id == self.id) return false;               // merge same blocks (water-water, glass-glass)
        if (n.isOpaqueCube()) return false;
        if (self.liquid && n.liquid) return false;
        return true;
    }

    /**
     * Packs sky and block light (0..1 each) of the neighbor cell into one float:
     * floor(sky*15)*16 + block*15. Unpacked in quad().
     */
    private float light(World w, Block self, int x, int y, int z) {
        int sky = 15, blk = 0;
        if (y >= 0 && y < Chunk.SIZE_Y) {
            Chunk c = w.getLoadedChunk(x >> 4, z >> 4);
            if (c != null) {
                int lx = x & 15, lz = z & 15;
                sky = c.getSkyLight(lx, y, lz);
                blk = c.getBlockLight(lx, y, lz);
            }
        } else if (y < 0) {
            sky = 0;
        }
        blk = Math.max(blk, self.lightEmission);
        return sky * 16 + blk;
    }

    /** Emits one quad (6 verts). Shade multiplies both light channels. */
    private void quad(FloatArr out, int tile, float packedLight, float shade,
                      float x0, float y0, float z0, float x1, float y1, float z1,
                      float x2, float y2, float z2, float x3, float y3, float z3,
                      boolean doubleSided) {
        float sky = ((int) packedLight >> 4) / 15f * shade;
        float blk = ((int) packedLight & 15) / 15f * shade;
        float u = atlas.u0[tile], v = atlas.v0[tile], s = atlas.tileUv;
        // v0(u,v+s) v1(u+s,v+s) v2(u+s,v) v3(u,v)
        vert(out, x0, y0, z0, u, v + s, sky, blk);
        vert(out, x1, y1, z1, u + s, v + s, sky, blk);
        vert(out, x2, y2, z2, u + s, v, sky, blk);
        vert(out, x0, y0, z0, u, v + s, sky, blk);
        vert(out, x2, y2, z2, u + s, v, sky, blk);
        vert(out, x3, y3, z3, u, v, sky, blk);
        if (doubleSided) {
            vert(out, x2, y2, z2, u + s, v, sky, blk);
            vert(out, x1, y1, z1, u + s, v + s, sky, blk);
            vert(out, x0, y0, z0, u, v + s, sky, blk);
            vert(out, x3, y3, z3, u, v, sky, blk);
            vert(out, x2, y2, z2, u + s, v, sky, blk);
            vert(out, x0, y0, z0, u, v + s, sky, blk);
        }
    }

    /** Vertical side face from (xa,za) to (xb,zb), height y..y+topY. */
    private void quadSide(FloatArr out, int tile, float packedLight, float shade,
                          float xa, float za, float xb, float zb,
                          float y, float topY, boolean doubleSided) {
        quad(out, tile, packedLight, shade,
                xa, y, za, xb, y, zb,
                xb, y + topY, zb, xa, y + topY, za, doubleSided);
    }

    private static void vert(FloatArr out, float x, float y, float z,
                             float u, float v, float sky, float blk) {
        out.add(x, y, z, u, v, sky, blk);
    }

    /** Growable float array without per-add allocation. */
    static final class FloatArr {
        float[] a;
        int size;

        FloatArr(int cap) { a = new float[cap]; }

        void add(float f0, float f1, float f2, float f3, float f4, float f5, float f6) {
            if (size + 7 > a.length) {
                float[] n = new float[a.length * 2];
                System.arraycopy(a, 0, n, 0, size);
                a = n;
            }
            a[size] = f0; a[size + 1] = f1; a[size + 2] = f2; a[size + 3] = f3;
            a[size + 4] = f4; a[size + 5] = f5; a[size + 6] = f6;
            size += 7;
        }

        float[] trimmed() {
            float[] n = new float[size];
            System.arraycopy(a, 0, n, 0, size);
            return n;
        }
    }
}
