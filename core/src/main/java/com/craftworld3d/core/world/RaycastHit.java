package com.craftworld3d.core.world;

/** Result of a block raycast. Reused to avoid allocations in the input loop. */
public final class RaycastHit {
    public boolean hit;
    public int x, y, z;          // block hit
    public int faceX, faceY, faceZ; // normal of the face hit (placement offset)
    public double distance;

    public void clear() {
        hit = false;
        distance = 0;
    }
}
