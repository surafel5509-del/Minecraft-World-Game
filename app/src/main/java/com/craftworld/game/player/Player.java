package com.craftworld.game.player;
import com.craftworld.game.world.*;
/** Player physics, survival state and camera orientation. */
public final class Player {
 public float x=0,y=82,z=0,yaw=35,pitch=-22, vx,vy,vz; public int health=20,hunger=20; public boolean grounded;
 public final Inventory inventory=new Inventory();
 public void update(World w,float moveX,float moveZ,boolean jump,float dt){float r=(float)Math.toRadians(yaw),speed=moveX*moveX+moveZ*moveZ>.01?5.2f:0;vx=(float)(Math.sin(r)*moveZ+Math.cos(r)*moveX)*speed;vz=(float)(-Math.cos(r)*moveZ+Math.sin(r)*moveX)*speed;if(jump&&grounded){vy=7.2f;grounded=false;}vy-=18*dt;float nx=x+vx*dt,nz=z+vz*dt,ny=y+vy*dt;int floor=highest(w,(int)Math.floor(nx),(int)Math.floor(nz));if(ny<floor+1.72f){ny=floor+1.72f;vy=0;grounded=true;}x=nx;y=ny;z=nz;}
 private int highest(World w,int x,int z){for(int y=120;y>0;y--)if(w.get(x,y,z).solid())return y;return 0;}
}
