package com.craftworld3d.core.noise;

import java.util.Random;

/**
 * Seeded gradient noise (improved-Perlin style) with 2D and 3D sampling plus
 * fractal Brownian motion helpers. Fully deterministic for a given seed.
 */
public final class PerlinNoise {
    private final int[] perm = new int[512];

    public PerlinNoise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        Random rnd = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }

    private static double lerp(double a, double b, double t) { return a + t * (b - a); }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /** 3D noise in [-1, 1]. */
    public double noise3(double x, double y, double z) {
        int X = fastFloor(x) & 255, Y = fastFloor(y) & 255, Z = fastFloor(z) & 255;
        x -= fastFloor(x); y -= fastFloor(y); z -= fastFloor(z);
        double u = fade(x), v = fade(y), w = fade(z);
        int a = perm[X] + Y, aa = perm[a] + Z, ab = perm[a + 1] + Z;
        int b = perm[X + 1] + Y, ba = perm[b] + Z, bb = perm[b + 1] + Z;
        return lerp(
            lerp(lerp(grad(perm[aa], x, y, z), grad(perm[ba], x - 1, y, z), u),
                 lerp(grad(perm[ab], x, y - 1, z), grad(perm[bb], x - 1, y - 1, z), u), v),
            lerp(lerp(grad(perm[aa + 1], x, y, z - 1), grad(perm[ba + 1], x - 1, y, z - 1), u),
                 lerp(grad(perm[ab + 1], x, y - 1, z - 1), grad(perm[bb + 1], x - 1, y - 1, z - 1), u), v),
            w);
    }

    /** 2D noise in [-1, 1]. */
    public double noise2(double x, double z) {
        return noise3(x, 0.31, z);
    }

    /** Fractal 2D noise in roughly [-1, 1]. */
    public double fbm2(double x, double z, int octaves, double lacunarity, double gain) {
        double sum = 0, amp = 1, freq = 1, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * noise2(x * freq, z * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return sum / norm;
    }

    /** Fractal 3D noise in roughly [-1, 1]. */
    public double fbm3(double x, double y, double z, int octaves, double lacunarity, double gain) {
        double sum = 0, amp = 1, freq = 1, norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * noise3(x * freq, y * freq, z * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return sum / norm;
    }

    /** Ridged noise in [0, 1]; 0 along the ridge line. Useful for rivers. */
    public double ridge2(double x, double z) {
        return Math.abs(noise2(x, z));
    }

    private static int fastFloor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}
