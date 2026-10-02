package com.craftworld3d.entity;
public final class BasicMobs {private BasicMobs(){} public static Mob passive(){return new Walker(false);}public static Mob hostile(){return new Walker(true);}private static final class Walker extends Mob{Walker(boolean h){hostile=h;health=h?20:10;}public void tick(float dt,float px,float pz){if(hostile){x+=(px-x)*dt*.2f;z+=(pz-z)*dt*.2f;}}}}
