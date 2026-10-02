package com.craftworld.game.world;
import java.util.concurrent.*; import java.util.*;
/** Thread-safe chunk cache with asynchronous generation and distance unloading. */
public final class World {
 private final ConcurrentMap<Long,Chunk> chunks=new ConcurrentHashMap<>(); private final ExecutorService pool=Executors.newFixedThreadPool(2); private final WorldGenerator gen;
 public World(long seed){gen=new WorldGenerator(seed);}
 private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
 public Chunk getOrCreate(int x,int z){return chunks.computeIfAbsent(key(x,z),k->gen.generate(x,z));}
 public void stream(int cx,int cz,int radius){for(int x=cx-radius;x<=cx+radius;x++)for(int z=cz-radius;z<=cz+radius;z++){final int a=x,b=z;pool.execute(()->getOrCreate(a,b));}chunks.entrySet().removeIf(e->Math.abs(e.getValue().x-cx)>radius+1||Math.abs(e.getValue().z-cz)>radius+1);}
 public Collection<Chunk> loaded(){return chunks.values();}
 public Block get(int x,int y,int z){int cx=Math.floorDiv(x,16),cz=Math.floorDiv(z,16);return getOrCreate(cx,cz).get(Math.floorMod(x,16),y,Math.floorMod(z,16));}
 public void set(int x,int y,int z,Block b){getOrCreate(Math.floorDiv(x,16),Math.floorDiv(z,16)).set(Math.floorMod(x,16),y,Math.floorMod(z,16),b);}
}
