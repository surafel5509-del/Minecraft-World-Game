package com.craftworld3d;
import android.app.*;import android.os.*;import android.opengl.*;import android.view.*;import android.widget.*;import com.craftworld3d.world.*;import com.craftworld3d.player.*;import com.craftworld3d.render.*;import com.craftworld3d.ui.*;
/** Entry point. The render loop stays on GL thread; world generation never blocks it. */
public final class MainActivity extends Activity {World world;Player player;GLSurfaceView surface;
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(1024,1024);world=new World();player=new Player();world.streamAround(0,0,3);surface=new GLSurfaceView(this);surface.setEGLContextClientVersion(3);surface.setPreserveEGLContextOnPause(true);surface.setRenderer(new VoxelRenderer(world,player));surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);FrameLayout root=new FrameLayout(this);root.addView(surface);root.addView(new HudView(this,player,world));setContentView(root);new Handler().post(tick);}
 private final Runnable tick=new Runnable(){public void run(){world.tick();player.update(1/30f,false,false,false,false,false);world.streamAround((int)player.x/16,(int)player.z/16,3);new Handler().postDelayed(this,33);}};
 protected void onDestroy(){world.close();super.onDestroy();}
}
