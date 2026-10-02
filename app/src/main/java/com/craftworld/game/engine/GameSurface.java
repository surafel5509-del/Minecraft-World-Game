package com.craftworld.game.engine;
import android.content.Context; import android.opengl.GLSurfaceView; import android.view.MotionEvent;
public final class GameSurface extends GLSurfaceView {
 private final GameState game; private float lx,ly;
 public GameSurface(Context c,GameRenderer r){super(c);game=r.getGame();setEGLContextClientVersion(3);setEGLConfigChooser(8,8,8,8,24,8);setRenderer(r);setRenderMode(RENDERMODE_CONTINUOUSLY);}
 @Override public boolean onTouchEvent(MotionEvent e){if(e.getX()>getWidth()*.38f){if(e.getAction()==MotionEvent.ACTION_DOWN){lx=e.getX();ly=e.getY();}else if(e.getAction()==MotionEvent.ACTION_MOVE){game.lookDX+=e.getX()-lx;game.lookDY+=e.getY()-ly;lx=e.getX();ly=e.getY();}return true;}return false;}
}
