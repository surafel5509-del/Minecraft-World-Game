package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.entity.vehicle.Vehicle;
import com.craftworld3d.core.entity.vehicle.VehicleType;
import com.craftworld3d.core.world.World;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VehicleTest {
    private World world;

    @Before
    public void setUp() {
        world = TestWorlds.flatWorld();
    }

    private static void settle(Vehicle v, World w, int ticks) {
        for (int i = 0; i < ticks; i++) v.tick(w);
    }

    @Test
    public void carDrivesForwardAndBurnsFuel() {
        Vehicle car = new Vehicle(VehicleType.SEDAN);
        car.setPosition(8, 64, 8);
        car.mounted = true;
        settle(car, world, 10);
        float fuelBefore = car.fuel;
        double zBefore = car.z;
        car.throttleInput = 1;
        settle(car, world, 100);
        assertTrue("car should move, z=" + car.z, car.z - zBefore > 5);
        assertTrue("fuel should burn", car.fuel < fuelBefore);
    }

    @Test
    public void carWithoutFuelDoesNotAccelerate() {
        Vehicle car = new Vehicle(VehicleType.SEDAN);
        car.setPosition(8, 64, 8);
        car.mounted = true;
        car.fuel = 0;
        settle(car, world, 10);
        car.throttleInput = 1;
        settle(car, world, 60);
        assertTrue("speed " + car.speed, Math.abs(car.speed) < 0.5f);
    }

    @Test
    public void refuelingCapsAtCapacity() {
        Vehicle car = new Vehicle(VehicleType.TRUCK);
        car.fuel = 0;
        assertTrue(car.addFuel(10));
        assertEquals(10, car.fuel, 0.01);
        car.addFuel(10000);
        assertEquals(car.type.fuelCapacity, car.fuel, 0.01);
        assertFalse(car.addFuel(1));
    }

    @Test
    public void steeringTurnsCar() {
        Vehicle car = new Vehicle(VehicleType.JEEP);
        car.setPosition(8, 64, 8);
        car.mounted = true;
        settle(car, world, 10);
        car.throttleInput = 1;
        settle(car, world, 40);
        float yawBefore = car.yaw;
        car.steerInput = 1;
        settle(car, world, 20);
        assertNotEquals(yawBefore, car.yaw, 0.5f);
    }

    @Test
    public void planeTakesOffOnlyAboveTakeoffSpeed() {
        Vehicle plane = new Vehicle(VehicleType.SMALL_PLANE);
        plane.setPosition(8, 64, 8);
        plane.mounted = true;
        plane.fuel = plane.type.fuelCapacity;
        settle(plane, world, 10);
        assertFalse(plane.airborne);

        plane.throttleInput = 1;
        plane.pitchInput = -1; // pull up
        settle(plane, world, 300);
        assertTrue("plane should be airborne (speed=" + plane.speed + ")", plane.airborne);
        assertTrue("plane should climb, y=" + plane.y, plane.y > 66);
    }

    @Test
    public void helicopterLiftsVertically() {
        Vehicle heli = new Vehicle(VehicleType.HELICOPTER);
        heli.setPosition(8, 64, 8);
        heli.mounted = true;
        heli.fuel = heli.type.fuelCapacity;
        settle(heli, world, 10);
        heli.throttleInput = 1;
        settle(heli, world, 60);
        assertTrue("heli y=" + heli.y, heli.y > 66);
    }

    @Test
    public void boatFloatsAndMovesOnWater() {
        // Water pool.
        for (int x = 4; x <= 14; x++) {
            for (int z = 4; z <= 14; z++) {
                world.setBlock(x, 63, z, Blocks.WATER.id);
                world.setBlock(x, 62, z, Blocks.WATER.id);
            }
        }
        Vehicle boat = new Vehicle(VehicleType.BOAT);
        boat.setPosition(8, 64, 8);
        boat.mounted = true;
        settle(boat, world, 40);
        assertTrue("boat should float near surface, y=" + boat.y, boat.y > 62);
        double zBefore = boat.z;
        boat.throttleInput = 1;
        settle(boat, world, 60);
        assertTrue("boat should move", boat.z - zBefore > 2);
    }

    @Test
    public void minecartFollowsRailAndPoweredRailBoosts() {
        // Rail line along X at y=64.
        for (int x = 0; x <= 15; x++) {
            world.setBlock(x, 64, 8, Blocks.RAIL.id);
        }
        Vehicle cart = new Vehicle(VehicleType.MINECART);
        cart.setPosition(3.5, 64.1, 8.5);
        cart.mounted = true;
        cart.throttleInput = 1;
        settle(cart, world, 60);
        assertTrue("cart should roll along +X, x=" + cart.x, cart.x > 4.5);
        assertEquals("cart stays centered on rail", 8.5, cart.z, 0.2);

        // Powered rail boost (long track so the cart stays on rails).
        Vehicle cart2 = new Vehicle(VehicleType.MINECART);
        for (int x = -40; x <= 45; x++) {
            world.setBlock(x, 64, 12, Blocks.POWERED_RAIL.id);
        }
        cart2.setPosition(3.5, 64.1, 12.5);
        cart2.mounted = true;
        cart2.throttleInput = 1;
        float maxSpeed = 0;
        for (int i = 0; i < 60; i++) {
            cart2.tick(world);
            maxSpeed = Math.max(maxSpeed, Math.abs(cart2.speed));
        }
        assertTrue("boosted cart faster: " + maxSpeed + " vs " + cart.speed,
            maxSpeed > Math.abs(cart.speed));
    }

    @Test
    public void vehicleItemMapping() {
        assertEquals(VehicleType.SEDAN, VehicleType.byItem(VehicleType.SEDAN.itemId));
        assertEquals(VehicleType.JET, VehicleType.byItem(VehicleType.JET.itemId));
        assertNull(VehicleType.byItem(1));
    }

    @Test
    public void damageDestroysVehicle() {
        Vehicle boat = new Vehicle(VehicleType.BOAT);
        boat.damage(100);
        assertTrue(boat.dead);
    }
}
