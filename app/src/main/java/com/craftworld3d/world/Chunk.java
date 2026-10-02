package com.craftworld3d.world;
import java.util.*;
/** 16x16x256 chunk. Generation is deterministic and safe to run on a worker thread. */
public final class Chunk {
 public static final int SIZE=16, HEIGHT=256; public final int cx,cz; private final byte[] blocks=new byte[SIZE*HEIGHT*SIZE];
 public Chunk(int x,int z){cx=x;cz=z; generate();}
 private int i(int x,int y,int z){return (y*SIZE+z)*SIZE+x;}
 public byte get(int x,int y,int z){if(x<0||z<0||x>=SIZE||z>=SIZE||y<0||y>=HEIGHT)return 0;return blocks[i(x,y,z)];}
 public void set(int x,int y,int z,BlockType b){if(x>=0&&z>=0&&x<SIZE&&z<SIZE&&y>=0&&y<HEIGHT)blocks[i(x,y,z)]=(byte)b.id;}
 private void generate(){ Random r=new Random(cx*341873128712L+cz*132897987541L); for(int x=0;x<SIZE;x++)for(int z=0;z<SIZE;z++){int wx=cx*16+x,wz=cz*16+z; double n=Math.sin(wx*.055)*3+Math.cos(wz*.07)*3+Math.sin((wx+wz)*.013)*5; int h=Math.max(2, (int)(18+n)); BlockType top=(Math.abs(n)>6?BlockType.STONE:(Math.floorMod(wx+wz,17)==0?BlockType.SAND:BlockType.GRASS)); for(int y=0;y<=h;y++)set(x,y,z,y==0?BlockType.BEDROCK:y==h?top:y>h-3?BlockType.DIRT:BlockType.STONE); if(top==BlockType.GRASS&&r.nextInt(18)==0&&h+1<HEIGHT)set(x,h+1,z,BlockType.FLOWER);}}
 public byte[] raw(){return blocks;}
}
