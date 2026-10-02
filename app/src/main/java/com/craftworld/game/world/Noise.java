package com.craftworld.game.world;
/** Fast deterministic value noise with smooth interpolation; combines octaves for natural terrain. */
public final class Noise {
 private final long seed; public Noise(long seed){this.seed=seed;}
 private float raw(int x,int z){ long n=x*341873128712L+z*132897987541L+seed; n=(n^(n>>13))*1274126177L; return ((n^(n>>16))&0xffff)/32767.5f-1f; }
 private static float s(float t){return t*t*(3-2*t);}
 public float noise(float x,float z){int ix=(int)Math.floor(x), iz=(int)Math.floor(z);float fx=s(x-ix),fz=s(z-iz);float a=raw(ix,iz)+(raw(ix+1,iz)-raw(ix,iz))*fx,b=raw(ix,iz+1)+(raw(ix+1,iz+1)-raw(ix,iz+1))*fx;return a+(b-a)*fz;}
 public float fractal(float x,float z){float v=0,a=.55f,f=1,total=0;for(int i=0;i<5;i++){v+=noise(x*f,z*f)*a;total+=a;a*=.5f;f*=2;}return v/total;}
}
