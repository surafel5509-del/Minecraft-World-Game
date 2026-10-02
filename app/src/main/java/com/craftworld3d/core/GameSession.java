package com.craftworld3d.core;

import android.content.Context;
import com.craftworld3d.player.*;import com.craftworld3d.world.*;import java.io.*;import java.util.zip.*;
/** Complete offline session boundary. No network, account, cloud, or monetization dependency. */
public final class GameSession {
 public final World world=new World(); public final Player player=new Player(); public final Inventory inventory=new Inventory(); public final RecipeBook recipes=new RecipeBook();
 private final File save;
 public GameSession(Context c){save=new File(c.getFilesDir(),"world-1.dat");inventory.seed();load();}
 public void save(){try(DataOutputStream o=new DataOutputStream(new GZIPOutputStream(new FileOutputStream(save)))){o.writeInt(2);o.writeFloat(player.x);o.writeFloat(player.y);o.writeFloat(player.z);o.writeFloat(player.yaw);o.writeLong(world.time);for(int i=0;i<Inventory.SIZE;i++){o.writeInt(inventory.get(i).id);o.writeInt(inventory.get(i).count);}}catch(IOException ignored){}}
 private void load(){if(!save.exists())return;try(DataInputStream i=new DataInputStream(new GZIPInputStream(new FileInputStream(save)))){if(i.readInt()!=2)return;player.x=i.readFloat();player.y=i.readFloat();player.z=i.readFloat();player.yaw=i.readFloat();world.time=i.readLong();for(int n=0;n<Inventory.SIZE;n++){inventory.get(n).id=i.readInt();inventory.get(n).count=i.readInt();}}catch(IOException ignored){}}
 public void close(){save();world.close();}
}
