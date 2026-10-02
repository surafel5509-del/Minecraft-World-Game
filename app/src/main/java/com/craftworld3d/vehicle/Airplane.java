package com.craftworld3d.vehicle;
public final class Airplane extends Vehicle {public enum Type{SMALL_PLANE,JET,HELICOPTER,BIPLANE}public Type type=Type.SMALL_PLANE;public float altitude,pitch,roll,yaw;public boolean landingGear=true;public void fly(float throttle,float dt){speed=throttle*80;altitude+=pitch*dt*speed;tick(dt);}}
