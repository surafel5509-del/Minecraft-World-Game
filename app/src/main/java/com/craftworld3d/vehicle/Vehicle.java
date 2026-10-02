package com.craftworld3d.vehicle;
/** Shared simulation contract for cars, aircraft, trains and boats. */
public abstract class Vehicle {public float x,y,z,speed,fuel=100,damage;public boolean occupied,lights;public void tick(float dt){if(occupied){fuel=Math.max(0,fuel-dt*Math.abs(speed)*.01f);x+=speed*dt;}}public void refuel(){fuel=100;}public void repair(){damage=0;}}
