package com.craftworld3d.world;
import android.content.Context;import java.io.*;import java.util.zip.*;
/** Offline compressed save adapter. Metadata can be extended with Gson/Room without changing World. */
public final class WorldRepository {private final File file;public WorldRepository(Context c){file=new File(c.getFilesDir(),"craftworld.dat");}public void save(World w)throws IOException{try(DataOutputStream o=new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))){o.writeInt(1);o.writeLong(w.time);o.writeInt(w.loaded().size());for(Chunk c:w.loaded()){o.writeInt(c.cx);o.writeInt(c.cz);o.write(c.raw());}}}public long readTime()throws IOException{try(DataInputStream i=new DataInputStream(new GZIPInputStream(new FileInputStream(file)))){i.readInt();return i.readLong();}}}
