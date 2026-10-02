package com.craftworld.game.engine;
import com.craftworld.game.player.Player; import com.craftworld.game.world.World; import com.craftworld.game.world.Block;
/** Shared simulation state. Volatile controls are written by UI and consumed on GL thread. */
public final class GameState {
 public final World world=new World(20261002L); public final Player player=new Player();
 public volatile float moveX,moveZ,lookDX,lookDY; public volatile boolean jump,breaking,placing; public float time=.28f; private int streamedX=Integer.MIN_VALUE,streamedZ=Integer.MIN_VALUE;
 public void tick(float dt){player.yaw+=lookDX*.12f;player.pitch=Math.max(-89,Math.min(89,player.pitch+lookDY*.12f));lookDX=lookDY=0;player.update(world,moveX,moveZ,jump,dt);jump=false;time=(time+dt/600f)%1f;int cx=(int)Math.floor(player.x/16),cz=(int)Math.floor(player.z/16);if(cx!=streamedX||cz!=streamedZ){streamedX=cx;streamedZ=cz;world.stream(cx,cz,2);}}
 /** Center-screen voxel ray cast. A short tap places; callers can expose breaking separately. */
 public void interact(boolean place){float yaw=(float)Math.toRadians(player.yaw),pitch=(float)Math.toRadians(player.pitch);float dx=(float)(Math.sin(yaw)*Math.cos(pitch)),dy=-(float)Math.sin(pitch),dz=-(float)(Math.cos(yaw)*Math.cos(pitch));int lastX=0,lastY=0,lastZ=0;for(float d=0;d<6;d+=.12f){int x=(int)Math.floor(player.x+dx*d),y=(int)Math.floor(player.y+dy*d),z=(int)Math.floor(player.z+dz*d);Block b=world.get(x,y,z);if(b.solid()){if(place)world.set(lastX,lastY,lastZ,player.inventory.selectedBlock());else if(b.hardness>=0)world.set(x,y,z,Block.AIR);return;}lastX=x;lastY=y;lastZ=z;}}
}
