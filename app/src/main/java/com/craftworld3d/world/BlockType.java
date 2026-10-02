package com.craftworld3d.world;

/** Registry of the game's blocks. IDs are save-file stable; colors are a temporary texture-atlas palette. */
public enum BlockType {
 AIR(0,0,0,0), GRASS(1,.32f,.68f,.20f), DIRT(2,.45f,.25f,.10f), STONE(3,.42f,.45f,.48f),
 COBBLESTONE(4,.30f,.32f,.34f), SAND(5,.82f,.70f,.40f), WATER(6,.12f,.35f,.78f), WOOD(7,.38f,.20f,.08f),
 LEAVES(8,.12f,.48f,.12f), GLASS(9,.65f,.85f,1), COAL_ORE(10,.15f,.15f,.15f), IRON_ORE(11,.65f,.40f,.25f),
 GOLD_ORE(12,1,.72f,.10f), DIAMOND_ORE(13,.15f,.85f,.95f), BEDROCK(14,.08f,.08f,.09f), GLOWSTONE(15,1,.65f,.15f),
 REDSTONE(16,.75f,.05f,.03f), BRICK(17,.60f,.20f,.12f), SNOW(18,.95f,.97f,1), ICE(19,.55f,.82f,1), CACTUS(20,.1f,.55f,.18f),
 CLAY(21,.48f,.52f,.55f), GRAVEL(22,.5f,.47f,.4f), OBSIDIAN(23,.12f,.06f,.18f), TNT(24,.8f,.08f,.05f),
 RAIL(25,.15f,.12f,.10f), RUBBER(26,.08f,.08f,.08f), ALUMINUM(27,.7f,.74f,.78f), FURNACE(28,.25f,.27f,.3f),
 IRON_BLOCK(29,.65f,.67f,.7f), LIGHTWEIGHT_METAL(30,.75f,.8f,.85f), ENGINE(31,.35f,.38f,.42f), WHEEL(32,.05f,.05f,.05f),
 STEERING_WHEEL(33,.1f,.1f,.1f), FUEL_TANK(34,.25f,.3f,.35f), WING(35,.6f,.65f,.7f), PROPELLER(36,.45f,.48f,.52f),
 PORTAL(37,.5f,.05f,.7f), LAVA(38,.95f,.18f,.02f), TORCH(39,1,.7f,.1f), PLANKS(40,.6f,.36f,.12f),
 FARMLAND(41,.3f,.18f,.08f), WHEAT(42,.7f,.62f,.12f), RAIL_POWERED(43,.8f,.22f,.1f), PISTON(44,.5f,.35f,.2f),
 GLASS_PANE(45,.65f,.9f,1), LADDER(46,.55f,.3f,.12f), BOOKSHELF(47,.5f,.25f,.08f), SPONGE(48,.75f,.75f,.2f),
 MOSS(49,.15f,.4f,.14f), END_STONE(50,.75f,.7f,.45f), PURPUR(51,.6f,.3f,.65f), BASALT(52,.18f,.2f,.21f),
 COPPER_ORE(53,.7f,.3f,.15f), COPPER_BLOCK(54,.7f,.35f,.2f), AMETHYST(55,.55f,.2f,.8f), LATEX(56,.9f,.9f,.75f),
 GAS_STATION(57,.8f,.1f,.08f), RUNE(58,.2f,.8f,.8f), BRONZE(59,.55f,.3f,.12f), HAY(60,.8f,.65f,.15f),
 FLOWER(61,.9f,.15f,.3f), MUSHROOM(62,.7f,.15f,.1f), CLOUD(63,.9f,.9f,.95f);
 public final int id; public final float r,g,b; BlockType(int i,float r,float g,float b){id=i;this.r=r;this.g=g;this.b=b;}
 public static BlockType fromId(int id){for(BlockType b:values())if(b.id==id)return b;return AIR;}
}
