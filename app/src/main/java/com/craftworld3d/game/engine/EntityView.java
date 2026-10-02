package com.craftworld3d.game.engine;

/**
 * Immutable per-tick snapshot of an entity for the render thread.
 * The engine publishes a fresh list every tick; the renderer interpolates
 * between prev and cur positions for smooth 60 fps movement.
 */
public final class EntityView {
    public final int tile;          // atlas tile index for the billboard
    public final float w, h;        // render size in blocks
    public final double px, py, pz; // position at previous tick
    public final double cx, cy, cz; // position at current tick
    public final boolean hurtFlash;
    public final boolean isVehicle;

    public EntityView(int tile, float w, float h,
                      double px, double py, double pz,
                      double cx, double cy, double cz,
                      boolean hurtFlash, boolean isVehicle) {
        this.tile = tile;
        this.w = w;
        this.h = h;
        this.px = px; this.py = py; this.pz = pz;
        this.cx = cx; this.cy = cy; this.cz = cz;
        this.hurtFlash = hurtFlash;
        this.isVehicle = isVehicle;
    }
}
