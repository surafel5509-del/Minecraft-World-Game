package com.craftworld3d.vehicle;
public final class Car extends Vehicle {public enum Type{SEDAN,SUV,TRUCK,SPORTS_CAR,JEEP}public Type type=Type.SEDAN;public boolean horn;public void accelerate(float a){speed=Math.max(0,Math.min(30,speed+a));}}
