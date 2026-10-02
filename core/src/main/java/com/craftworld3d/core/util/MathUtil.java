package com.craftworld3d.core.util;

/** Small math helpers used across the engine. No allocations. */
public final class MathUtil {
    private MathUtil() {}

    public static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    public static float wrapDegrees(float deg) {
        deg %= 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    /** Packs two 32-bit ints (chunk coords) into one long key. */
    public static long packXZ(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    public static int unpackX(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    public static int unpackZ(long key) {
        return (int) ((key >>> 32) & 0xFFFFFFFFL);
    }

    public static double distSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }
}
