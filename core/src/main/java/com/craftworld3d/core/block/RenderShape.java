package com.craftworld3d.core.block;

/** How the renderer draws a block. */
public enum RenderShape {
    NONE,   // air
    CUBE,   // full cube
    CROSS,  // X-shaped plant/torch sprite
    FLAT,   // flat overlay on the block below (rails, wire, pressure plate)
    SLAB    // thin box (door panels, etc.)
}
