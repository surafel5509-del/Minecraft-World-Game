package com.craftworld3d.entity;
public abstract class Mob {public float x,y,z,health=10;public boolean hostile;public void damage(float n){health-=n;}public boolean alive(){return health>0;}public abstract void tick(float dt,float px,float pz);}
