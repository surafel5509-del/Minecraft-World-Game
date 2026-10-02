package com.craftworld.game.world;
/** Seeded biome and terrain generator. Generation can safely run on worker threads. */
public final class WorldGenerator {
 private final Noise terrain, climate; public WorldGenerator(long seed){terrain=new Noise(seed);climate=new Noise(seed^0x5DEECE66DL);}
 public Chunk generate(int cx,int cz){Chunk c=new Chunk(cx,cz);for(int x=0;x<16;x++)for(int z=0;z<16;z++){int wx=cx*16+x,wz=cz*16+z;float t=terrain.fractal(wx/90f,wz/90f),bio=climate.fractal(wx/220f,wz/220f);int h=64+(int)(t*21)+(t>.45? (int)((t-.45)*55):0);boolean desert=bio>.34,snow=bio<-.42;for(int y=0;y<=h;y++){Block b=y==0?Block.BEDROCK:y<h-4?Block.STONE:y<h?Block.DIRT:desert?Block.SAND:snow?Block.SNOW:Block.GRASS;c.set(x,y,z,b);}for(int y=h+1;y<=62;y++)c.set(x,y,z,Block.WATER);if(!desert&&!snow&&h>63&&hash(wx,wz)%41==0)tree(c,x,h+1,z);}return c;}
 private void tree(Chunk c,int x,int y,int z){for(int a=0;a<4;a++)c.set(x,y+a,z,Block.OAK_LOG);for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=2;dy<=4;dy++)if(Math.abs(dx)+Math.abs(dz)<4)c.set(x+dx,y+dy,z+dz,Block.OAK_LEAVES);}
 private int hash(int x,int z){return Math.abs((x*73428767)^(z*912931));}
}
