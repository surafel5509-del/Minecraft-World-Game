package com.craftworld.game.player;
import java.util.*; import com.craftworld.game.world.Block;
/** Shapeless recipe registry used by both 2x2 and 3x3 crafting screens. */
public final class Crafting {private final Map<String,String> recipes=new HashMap<>();public Crafting(){add("CAR","IRON_BLOCKx4","ENGINE","WHEELx4","GLASS");add("ENGINE","IRON_BLOCK","REDSTONE_BLOCK","FURNACE");add("WHEEL","LATEX","IRON_BLOCK");add("AIRPLANE","IRON_BLOCKx8","WINGx2","PROPELLER","ENGINE","GLASSx4","FUEL_TANK");}private void add(String out,String...in){String[] copy=in.clone();Arrays.sort(copy);recipes.put(String.join("+",copy),out);}public String craft(String...items){Arrays.sort(items);return recipes.get(String.join("+",items));}}
