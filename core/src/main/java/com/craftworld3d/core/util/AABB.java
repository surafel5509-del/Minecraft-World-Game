package com.craftworld3d.core.util;

/** Axis-aligned bounding box with swept collision helpers (Minecraft-style). Mutable to avoid allocation. */
public final class AABB {
    public double minX, minY, minZ, maxX, maxY, maxZ;

    public AABB() {}

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        set(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        return this;
    }

    public AABB set(AABB o) {
        return set(o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ);
    }

    public AABB move(double dx, double dy, double dz) {
        minX += dx; maxX += dx;
        minY += dy; maxY += dy;
        minZ += dz; maxZ += dz;
        return this;
    }

    public AABB expand(double dx, double dy, double dz) {
        if (dx < 0) minX += dx; else maxX += dx;
        if (dy < 0) minY += dy; else maxY += dy;
        if (dz < 0) minZ += dz; else maxZ += dz;
        return this;
    }

    public boolean intersects(AABB o) {
        return o.maxX > minX && o.minX < maxX
            && o.maxY > minY && o.minY < maxY
            && o.maxZ > minZ && o.minZ < maxZ;
    }

    /**
     * Treats {@code this} as a static obstacle and {@code o} as the moving box:
     * returns the clipped dy so {@code o} stops at this box on the Y axis.
     */
    public double clipY(AABB o, double dy) {
        if (o.maxX <= minX || o.minX >= maxX || o.maxZ <= minZ || o.minZ >= maxZ) return dy;
        if (dy > 0 && o.maxY <= minY) {
            double d = minY - o.maxY;
            if (d < dy) dy = d;
        } else if (dy < 0 && o.minY >= maxY) {
            double d = maxY - o.minY;
            if (d > dy) dy = d;
        }
        return dy;
    }

    public double clipX(AABB o, double dx) {
        if (o.maxY <= minY || o.minY >= maxY || o.maxZ <= minZ || o.minZ >= maxZ) return dx;
        if (dx > 0 && o.maxX <= minX) {
            double d = minX - o.maxX;
            if (d < dx) dx = d;
        } else if (dx < 0 && o.minX >= maxX) {
            double d = maxX - o.minX;
            if (d > dx) dx = d;
        }
        return dx;
    }

    public double clipZ(AABB o, double dz) {
        if (o.maxY <= minY || o.minY >= maxY || o.maxX <= minX || o.minX >= maxX) return dz;
        if (dz > 0 && o.maxZ <= minZ) {
            double d = minZ - o.maxZ;
            if (d < dz) dz = d;
        } else if (dz < 0 && o.minZ >= maxZ) {
            double d = maxZ - o.minZ;
            if (d > dz) dz = d;
        }
        return dz;
    }
}
