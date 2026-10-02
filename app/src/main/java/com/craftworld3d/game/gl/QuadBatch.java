package com.craftworld3d.game.gl;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Dynamic batch of textured quads using the world vertex format.
 * Used for entities (billboards), particles, precipitation and sun/moon.
 * One preallocated buffer, no allocations while rendering.
 */
public final class QuadBatch {
    private static final int MAX_QUADS = 4096;
    private static final int FLOATS = ChunkMesher.FLOATS_PER_VERT;

    private final float[] cpu = new float[MAX_QUADS * 6 * FLOATS];
    private final FloatBuffer buf = ByteBuffer.allocateDirect(cpu.length * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private int vbo = -1;
    private int size;

    public void createGl() {
        int[] id = new int[1];
        GLES30.glGenBuffers(1, id, 0);
        vbo = id[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, cpu.length * 4, null, GLES30.GL_DYNAMIC_DRAW);
    }

    public void begin() { size = 0; }

    public boolean isEmpty() { return size == 0; }

    /**
     * Adds a quad with the full tile (u0..u0+s). Corners counter-clockwise:
     * bottom-left, bottom-right, top-right, top-left.
     */
    public void quad(TextureAtlasGL atlas, int tile,
                     float sky, float blk,
                     float x0, float y0, float z0,
                     float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3) {
        float u = atlas.u0[tile], v = atlas.v0[tile], s = atlas.tileUv;
        quadUv(sky, blk, u, v + s, u + s, v,
                x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
    }

    /** Adds a quad with explicit uv rectangle (ua,va = bottom-left; ub,vb = top-right). */
    public void quadUv(float sky, float blk,
                       float ua, float va, float ub, float vb,
                       float x0, float y0, float z0,
                       float x1, float y1, float z1,
                       float x2, float y2, float z2,
                       float x3, float y3, float z3) {
        if (size + 6 * FLOATS > cpu.length) return;
        vert(x0, y0, z0, ua, va, sky, blk);
        vert(x1, y1, z1, ub, va, sky, blk);
        vert(x2, y2, z2, ub, vb, sky, blk);
        vert(x0, y0, z0, ua, va, sky, blk);
        vert(x2, y2, z2, ub, vb, sky, blk);
        vert(x3, y3, z3, ua, vb, sky, blk);
    }

    /** Vertical billboard centered at (x, y+h/2, z) facing the camera yaw. */
    public void billboard(TextureAtlasGL atlas, int tile, float sky, float blk,
                          float x, float y, float z, float w, float h, float camYaw) {
        float rx = (float) Math.cos(camYaw) * w * 0.5f;
        float rz = (float) Math.sin(camYaw) * w * 0.5f;
        quad(atlas, tile, sky, blk,
                x - rx, y, z - rz,
                x + rx, y, z + rz,
                x + rx, y + h, z + rz,
                x - rx, y + h, z - rz);
    }

    private void vert(float x, float y, float z, float u, float v, float sky, float blk) {
        cpu[size] = x; cpu[size + 1] = y; cpu[size + 2] = z;
        cpu[size + 3] = u; cpu[size + 4] = v;
        cpu[size + 5] = sky; cpu[size + 6] = blk;
        size += FLOATS;
    }

    public void draw(int posLoc, int uvLoc, int lightLoc) {
        if (size == 0 || vbo < 0) return;
        buf.position(0);
        buf.put(cpu, 0, size).position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, size * 4, buf);
        int stride = FLOATS * 4;
        GLES30.glVertexAttribPointer(posLoc, 3, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glVertexAttribPointer(uvLoc, 2, GLES30.GL_FLOAT, false, stride, 12);
        GLES30.glVertexAttribPointer(lightLoc, 2, GLES30.GL_FLOAT, false, stride, 20);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, size / FLOATS);
    }
}
