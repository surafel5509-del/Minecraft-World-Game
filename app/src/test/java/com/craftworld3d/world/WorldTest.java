package com.craftworld3d.world;
import org.junit.Test;import static org.junit.Assert.*;
public class WorldTest {@Test public void generationIsDeterministic(){assertEquals(new Chunk(2,-4).get(3,18,7),new Chunk(2,-4).get(3,18,7));}@Test public void placementRoundTrips(){World w=new World();w.setBlock(20,5,-3,BlockType.DIAMOND_ORE);assertEquals(BlockType.DIAMOND_ORE,w.getBlock(20,5,-3));w.close();}}
