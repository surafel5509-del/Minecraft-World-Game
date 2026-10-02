package com.craftworld.game.world;
/** A canonical 16x256x16 chunk. Byte IDs keep one chunk at 64 KiB. */
public final class Chunk {
 public static final int SIZE=16, HEIGHT=256; public final int x,z; private final byte[] blocks=new byte[SIZE*HEIGHT*SIZE];
 public Chunk(int x,int z){this.x=x;this.z=z;}
 private int i(int x,int y,int z){return (y*SIZE+z)*SIZE+x;}
 public Block get(int x,int y,int z){if(x<0||x>=SIZE||z<0||z>=SIZE||y<0||y>=HEIGHT)return Block.AIR; return Block.values()[blocks[i(x,y,z)]&255];}
 public void set(int x,int y,int z,Block b){if(x>=0&&x<SIZE&&z>=0&&z<SIZE&&y>=0&&y<HEIGHT)blocks[i(x,y,z)]=(byte)b.ordinal();}
 public byte[] data(){return blocks;}
}
