package com.craftworld.game.player;
import com.craftworld.game.world.Block;
/** 36 inventory slots; first nine are the hotbar. */
public final class Inventory {
 public static final class Stack {public Block block; public int count; Stack(Block b,int n){block=b;count=n;}}
 private final Stack[] slots=new Stack[36]; public int selected;
 public Inventory(){Block[] starter={Block.GRASS,Block.DIRT,Block.STONE,Block.PLANKS,Block.GLASS,Block.IRON_BLOCK,Block.ROAD,Block.RAIL,Block.GLOWSTONE};for(int i=0;i<9;i++)slots[i]=new Stack(starter[i],64);}
 public Stack get(int i){return slots[i];} public Block selectedBlock(){return slots[selected]==null?Block.DIRT:slots[selected].block;}
}
