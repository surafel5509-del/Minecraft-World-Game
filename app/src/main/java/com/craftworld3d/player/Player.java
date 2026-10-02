package com.craftworld3d.player;
public final class Player { public float x=0,y=26,z=0,yaw=0,pitch=0,vy=0; public int health=20,hunger=20; public boolean crouching,running; public final int[] inventory=new int[36]; public int selected;
 public void update(float dt, boolean forward,boolean back,boolean left,boolean right,boolean jump){float s=running?7:4;float dx=0,dz=0;if(forward)dz-=1;if(back)dz+=1;if(left)dx-=1;if(right)dx+=1;float c=(float)Math.cos(yaw),si=(float)Math.sin(yaw);x+=(dx*c-dz*si)*s*dt;z+=(dx*si+dz*c)*s*dt;vy-=18*dt;y+=vy*dt;if(y<25){y=25;vy=0;}if(jump&&y<=25.01)vy=7;}
}
