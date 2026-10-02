package com.craftworld3d.core.block;

/**
 * Immutable block definition. All gameplay data is carried here so new blocks
 * can be added purely by registering another definition (data-driven).
 */
public final class Block {
    public final int id;
    /** Stable machine name, also used as translation key ("block.grass"). */
    public final String name;
    /** Texture tile names resolved by the renderer's atlas (top, side, bottom). */
    public final String texTop, texSide, texBottom;
    /** Seconds to break bare-handed (scaled by tools); < 0 = unbreakable. */
    public final float hardness;
    /** Explosion resistance. */
    public final float resistance;
    /** True if light/faces pass through (glass, leaves, water, plants). */
    public final boolean transparent;
    /** Emitted light level 0..15. */
    public final int lightEmission;
    /** True if entities collide with this block. */
    public final boolean collidable;
    /** True for water/lava. */
    public final boolean liquid;
    /** True if the player can climb it (ladder). */
    public final boolean climbable;
    public final RenderShape shape;
    public final SoundType sound;
    /** Tool class that speeds up mining / is required for drops. */
    public final ToolType tool;
    /** Minimal tool tier (0 wood, 1 stone, 2 iron, 3 diamond) required for drops; 0 = none. */
    public final int toolTier;
    /** True when the correct tool class is mandatory for any drop (like stone needing a pickaxe). */
    public final boolean toolRequired;
    /** Item id dropped when mined; -1 = drops itself. */
    public final int dropItem;
    public final int dropCountMin, dropCountMax;

    private Block(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.texTop = b.texTop;
        this.texSide = b.texSide;
        this.texBottom = b.texBottom;
        this.hardness = b.hardness;
        this.resistance = b.resistance;
        this.transparent = b.transparent;
        this.lightEmission = b.lightEmission;
        this.collidable = b.collidable;
        this.liquid = b.liquid;
        this.climbable = b.climbable;
        this.shape = b.shape;
        this.sound = b.sound;
        this.tool = b.tool;
        this.toolTier = b.toolTier;
        this.toolRequired = b.toolRequired;
        this.dropItem = b.dropItem;
        this.dropCountMin = b.dropCountMin;
        this.dropCountMax = b.dropCountMax;
    }

    public boolean isAir() { return id == 0; }

    /** Full opaque cube: hides neighbouring faces. */
    public boolean isOpaqueCube() { return shape == RenderShape.CUBE && !transparent; }

    public boolean isUnbreakable() { return hardness < 0; }

    public static Builder builder(int id, String name, String tex) {
        return new Builder(id, name, tex);
    }

    public static final class Builder {
        private final int id;
        private final String name;
        private String texTop, texSide, texBottom;
        private float hardness = 1.0f;
        private float resistance = 5.0f;
        private boolean transparent = false;
        private int lightEmission = 0;
        private boolean collidable = true;
        private boolean liquid = false;
        private boolean climbable = false;
        private RenderShape shape = RenderShape.CUBE;
        private SoundType sound = SoundType.STONE;
        private ToolType tool = ToolType.NONE;
        private int toolTier = 0;
        private boolean toolRequired = false;
        private int dropItem = -1;
        private int dropCountMin = 1, dropCountMax = 1;

        Builder(int id, String name, String tex) {
            this.id = id;
            this.name = name;
            this.texTop = tex;
            this.texSide = tex;
            this.texBottom = tex;
        }

        public Builder tex(String top, String side, String bottom) {
            this.texTop = top; this.texSide = side; this.texBottom = bottom;
            return this;
        }

        public Builder hardness(float h) { this.hardness = h; return this; }
        public Builder resistance(float r) { this.resistance = r; return this; }
        public Builder strength(float h, float r) { this.hardness = h; this.resistance = r; return this; }
        public Builder transparent() { this.transparent = true; return this; }
        public Builder light(int l) { this.lightEmission = l; return this; }
        public Builder noCollision() { this.collidable = false; return this; }
        public Builder liquid() { this.liquid = true; this.collidable = false; this.transparent = true; return this; }
        public Builder climbable() { this.climbable = true; return this; }
        public Builder shape(RenderShape s) { this.shape = s; return this; }
        public Builder sound(SoundType s) { this.sound = s; return this; }
        public Builder tool(ToolType t, int tier, boolean required) {
            this.tool = t; this.toolTier = tier; this.toolRequired = required;
            return this;
        }
        public Builder drops(int itemId, int min, int max) {
            this.dropItem = itemId; this.dropCountMin = min; this.dropCountMax = max;
            return this;
        }
        public Builder dropsNothing() { this.dropItem = -2; return this; }

        public Block build() { return new Block(this); }
    }
}
