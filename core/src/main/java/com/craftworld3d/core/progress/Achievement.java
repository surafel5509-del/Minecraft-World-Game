package com.craftworld3d.core.progress;

/** Offline achievements. Translation keys are "achievement.<key>". */
public enum Achievement {
    FIRST_BLOCK("first_block"),
    FIRST_CRAFT("first_craft"),
    FIRST_TOOL("first_tool"),
    FIRST_HOUSE("first_house"),   // heuristic: place a door
    FIRST_DIAMOND("first_diamond"),
    FIRST_VEHICLE("first_vehicle"),
    FIRST_FLIGHT("first_flight"),
    FIRST_DIMENSION("first_dimension");

    public final String key;

    Achievement(String key) {
        this.key = key;
    }
}
