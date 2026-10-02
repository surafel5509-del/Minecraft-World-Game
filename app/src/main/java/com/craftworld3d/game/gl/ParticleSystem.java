package com.craftworld3d.game.gl;

import java.util.Random;

/**
 * Pooled particle system (block-break bursts, explosion smoke, rain splash).
 * Fixed-size pool, no allocations during gameplay. Updated and rendered on
 * the GL thread; spawn requests may come from any thread.
 */
public final class ParticleSystem {
    public static final int MAX = 512;

    // struct-of-arrays pool
    private final float[] x = new float[MAX], y = new float[MAX], z = new float[MAX];
    private final float[] vx = new float[MAX], vy = new float[MAX], vz = new float[MAX];
    private final float[] life = new float[MAX];
    private final int[] tile = new int[MAX];
    private final float[] size = new float[MAX];
    private int count;

    private final Random rnd = new Random();

    /** Spawn a burst of block fragments. Safe to call from the tick thread. */
    public synchronized void burst(int tileIndex, float bx, float by, float bz, int n) {
        for (int i = 0; i < n && count < MAX; i++) {
            int p = count++;
            x[p] = bx + rnd.nextFloat();
            y[p] = by + rnd.nextFloat();
            z[p] = bz + rnd.nextFloat();
            vx[p] = (rnd.nextFloat() - 0.5f) * 3f;
            vy[p] = rnd.nextFloat() * 4f + 1f;
            vz[p] = (rnd.nextFloat() - 0.5f) * 3f;
            life[p] = 0.5f + rnd.nextFloat() * 0.5f;
            tile[p] = tileIndex;
            size[p] = 0.08f + rnd.nextFloat() * 0.06f;
        }
    }

    public synchronized void explosion(int tileIndex, float bx, float by, float bz) {
        for (int i = 0; i < 48 && count < MAX; i++) {
            int p = count++;
            x[p] = bx; y[p] = by; z[p] = bz;
            float a = rnd.nextFloat() * 6.2832f;
            float e = rnd.nextFloat() * 3.1416f - 1.5708f;
            float sp = 3f + rnd.nextFloat() * 6f;
            vx[p] = (float) (Math.cos(a) * Math.cos(e)) * sp;
            vy[p] = (float) Math.sin(e) * sp + 2f;
            vz[p] = (float) (Math.sin(a) * Math.cos(e)) * sp;
            life[p] = 0.6f + rnd.nextFloat();
            tile[p] = tileIndex;
            size[p] = 0.15f + rnd.nextFloat() * 0.15f;
        }
    }

    public synchronized void update(float dt) {
        for (int p = 0; p < count; ) {
            life[p] -= dt;
            if (life[p] <= 0) {
                int last = --count;   // swap-remove
                x[p] = x[last]; y[p] = y[last]; z[p] = z[last];
                vx[p] = vx[last]; vy[p] = vy[last]; vz[p] = vz[last];
                life[p] = life[last]; tile[p] = tile[last]; size[p] = size[last];
                continue;
            }
            vy[p] -= 14f * dt;
            x[p] += vx[p] * dt;
            y[p] += vy[p] * dt;
            z[p] += vz[p] * dt;
            p++;
        }
    }

    /** Renders all particles as camera-facing quads using a corner of their tile. */
    public synchronized void render(QuadBatch batch, TextureAtlasGL atlas, float camYaw) {
        for (int p = 0; p < count; p++) {
            float u = atlas.u0[tile[p]] + atlas.tileUv * 0.25f;
            float v = atlas.v0[tile[p]] + atlas.tileUv * 0.25f;
            float du = atlas.tileUv * 0.25f;
            float rx = (float) Math.cos(camYaw) * size[p];
            float rz = (float) Math.sin(camYaw) * size[p];
            batch.quadUv(1f, 0.6f, u, v + du, u + du, v,
                    x[p] - rx, y[p] - size[p], z[p] - rz,
                    x[p] + rx, y[p] - size[p], z[p] + rz,
                    x[p] + rx, y[p] + size[p], z[p] + rz,
                    x[p] - rx, y[p] + size[p], z[p] - rz);
        }
    }

    public synchronized void clear() { count = 0; }
}
