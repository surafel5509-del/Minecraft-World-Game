package com.craftworld3d.player;
import com.craftworld3d.world.BlockType;import java.util.*;
/** Offline 2x2/3x3 crafting recipes. Recipes are data, so new content does not touch the UI. */
public final class RecipeBook {
 public static final class Recipe{public final BlockType out;public final int amount;public final BlockType[] in;Recipe(BlockType o,int n,BlockType...i){out=o;amount=n;in=i;}}
 private final List<Recipe> recipes=Arrays.asList(
  new Recipe(BlockType.PLANKS,4,BlockType.WOOD),new Recipe(BlockType.TORCH,4,BlockType.COAL_ORE),
  new Recipe(BlockType.BRICK,4,BlockType.CLAY,BlockType.CLAY,BlockType.CLAY,BlockType.CLAY),
  new Recipe(BlockType.ENGINE,1,BlockType.IRON_BLOCK,BlockType.REDSTONE,BlockType.FURNACE),
  new Recipe(BlockType.WHEEL,4,BlockType.RUBBER,BlockType.IRON_ORE),
  new Recipe(BlockType.LIGHTWEIGHT_METAL,2,BlockType.ALUMINUM,BlockType.COAL_ORE));
 public List<Recipe> all(){return recipes;}
 public boolean craft(Inventory inv,Recipe r){Map<Integer,Integer> need=new HashMap<>();for(BlockType b:r.in)need.put(b.id,need.getOrDefault(b.id,0)+1);for(Map.Entry<Integer,Integer> e:need.entrySet()){int total=0;for(int i=0;i<Inventory.SIZE;i++)if(inv.get(i).id==e.getKey())total+=inv.get(i).count;if(total<e.getValue())return false;}for(Map.Entry<Integer,Integer> e:need.entrySet())inv.remove(e.getKey(),e.getValue());inv.add(r.out,r.amount);return true;}
}
