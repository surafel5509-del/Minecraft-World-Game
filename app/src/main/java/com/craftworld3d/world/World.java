package com.craftworld3d.world;
import java.util.concurrent.*;import java.util.*;
/** Chunk cache with bounded lifetime; a real game can swap this repository for SQLite/cloud storage. */
public final class World { private final ConcurrentHashMap<Long,Chunk> chunks=new ConcurrentHashMap<>(); private final ExecutorService worker=Executors.newFixedThreadPool(2); public long time;
 private long key(int x,int z){return (((long)x)<<32)^(z&0xffffffffL);} public Chunk get(int cx,int cz){return chunks.computeIfAbsent(key(cx,cz),k->new Chunk(cx,cz));}
 public BlockType getBlock(int x,int y,int z){int cx=Math.floorDiv(x,16),cz=Math.floorDiv(z,16);return BlockType.fromId(get(cx,cz).get(Math.floorMod(x,16),y,Math.floorMod(z,16)));}
 public void setBlock(int x,int y,int z,BlockType b){get(Math.floorDiv(x,16),Math.floorDiv(z,16)).set(Math.floorMod(x,16),y,Math.floorMod(z,16),b);}
 public void streamAround(int cx,int cz,int radius){for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){final int a=cx+x,b=cz+z;worker.submit(()->get(a,b));}}
 public Collection<Chunk> loaded(){return chunks.values();} public void tick(){time=(time+1)%24000;} public void close(){worker.shutdownNow();}
}
