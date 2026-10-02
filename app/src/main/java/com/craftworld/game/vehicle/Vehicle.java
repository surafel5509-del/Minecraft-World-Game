package com.craftworld.game.vehicle;
/** Shared fuel/damage physics contract for drivable entities. */
public abstract class Vehicle { public float x,y,z,speed,fuel=100,health=100; public boolean occupied; public abstract void update(float throttle,float steering,float dt); public void damage(float amount){health=Math.max(0,health-amount);} }
