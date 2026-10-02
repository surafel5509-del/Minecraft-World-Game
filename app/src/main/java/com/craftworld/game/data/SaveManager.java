package com.craftworld.game.data;
import android.content.*; import com.craftworld.game.player.Player; import org.json.*; import java.io.*;
/** Atomic local JSON player save. Chunk binary compression is intentionally isolated from UI. */
public final class SaveManager {private final File file;public SaveManager(Context c){file=new File(c.getFilesDir(),"player.json");}public void save(Player p)throws IOException{try{JSONObject j=new JSONObject();j.put("x",p.x);j.put("y",p.y);j.put("z",p.z);j.put("health",p.health);File tmp=new File(file+".tmp");try(FileWriter w=new FileWriter(tmp)){w.write(j.toString());}if(!tmp.renameTo(file))throw new IOException("Could not commit save");}catch(JSONException e){throw new IOException(e);}}}
