package com.craftworld3d.core.world;

/** World weather states. */
public enum Weather {
    CLEAR, RAIN, THUNDER;

    public boolean isPrecipitating() { return this != CLEAR; }
}
