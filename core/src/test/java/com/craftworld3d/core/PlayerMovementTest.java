package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.entity.Player;
import com.craftworld3d.core.world.World;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerMovementTest {
    private World world;
    private Player player;

    @Before
    public void setUp() {
        world = TestWorlds.flatWorld();
        player = new Player();
        world.player = player;
        player.setPosition(8.5, 64, 8.5); // on top of grass at y=63
    }

    private void tick(int n) {
        for (int i = 0; i < n; i++) player.tick(world);
    }

    @Test
    public void fallsToGroundAndRests() {
        player.setPosition(8.5, 70, 8.5);
        tick(60);
        assertTrue(player.onGround);
        assertEquals(64.0, player.y, 0.01);
    }

    @Test
    public void walksForward() {
        tick(5); // settle
        double startZ = player.z;
        player.inputForward = 1;
        tick(40); // 2 seconds
        double moved = player.z - startZ;
        assertTrue("moved " + moved, moved > 5);
        assertTrue(player.distanceTraveled > 5);
    }

    @Test
    public void sprintingIsFasterAndDrainsStamina() {
        tick(5);
        player.inputForward = 1;
        tick(40);
        double walked = player.z;

        player.setPosition(8.5, 64, 8.5);
        player.vx = player.vy = player.vz = 0;
        tick(5);
        double start = player.z;
        player.inputForward = 1;
        player.wantSprint = true;
        tick(40);
        assertTrue(player.z - start > walked - 8.5 + 0.5);
        assertTrue(player.stamina < 20);
    }

    @Test
    public void crouchingIsSlower() {
        tick(5);
        player.inputForward = 1;
        player.wantCrouch = true;
        double start = player.z;
        tick(40);
        double crouched = player.z - start;
        assertTrue("crouch moved " + crouched, crouched < 4.0 && crouched > 0.5);
    }

    @Test
    public void jumpLeavesGroundAndLands() {
        tick(10);
        assertTrue(player.onGround);
        player.wantJump = true;
        tick(2);
        player.wantJump = false;
        assertFalse(player.onGround);
        double peak = 0;
        for (int i = 0; i < 30; i++) {
            player.tick(world);
            peak = Math.max(peak, player.y);
        }
        assertTrue("peak " + peak, peak > 64.8);
        assertTrue(player.onGround);
    }

    @Test
    public void collidesWithWalls() {
        tick(5);
        // Build a wall just north (+Z) of the player.
        for (int y = 63; y < 67; y++) {
            for (int x = 6; x < 12; x++) {
                world.setBlock(x, y, 10, Blocks.STONE.id);
            }
        }
        player.inputForward = 1; // +Z
        tick(40);
        assertTrue("stopped before wall at z=10, z=" + player.z, player.z < 10.0);
    }

    @Test
    public void fallDamageAppliedOnHardLanding() {
        player.setPosition(8.5, 80, 8.5); // ~16 block drop
        tick(80);
        assertTrue(player.onGround);
        assertTrue("health " + player.health, player.health < 20);
    }

    @Test
    public void noFallDamageIntoWater() {
        // Pool at the surface.
        for (int x = 7; x <= 10; x++) {
            for (int z = 7; z <= 10; z++) {
                world.setBlock(x, 63, z, Blocks.WATER.id);
                world.setBlock(x, 62, z, Blocks.WATER.id);
                world.setBlock(x, 61, z, Blocks.WATER.id);
            }
        }
        player.setPosition(8.5, 80, 8.5);
        tick(80);
        assertEquals(20, player.health, 0.01);
    }

    @Test
    public void climbsLadders() {
        tick(5);
        // Wall in front (+Z) with a ladder column on its face; the player
        // pushes into the wall and climbs.
        for (int y = 64; y < 72; y++) {
            for (int x = 6; x <= 10; x++) {
                world.setBlock(x, y, 9, Blocks.STONE.id);
            }
            world.setBlock(8, y, 8, Blocks.LADDER.id);
        }
        player.setPosition(8.5, 64, 8.5);
        player.inputForward = 1;
        tick(40);
        assertTrue("y=" + player.y, player.y > 65);
    }

    @Test
    public void creativeModeIgnoresFallDamageAndHunger() {
        player.gamemode = Player.GAMEMODE_CREATIVE;
        player.setPosition(8.5, 90, 8.5);
        tick(100);
        assertEquals(20, player.health, 0.01);
        assertEquals(20, player.hunger, 0.01);
    }

    @Test
    public void respawnRestoresVitals() {
        player.health = 0;
        player.respawn(4, 64, 4);
        assertEquals(20, player.health, 0.01);
        assertEquals(20, player.hunger, 0.01);
        assertFalse(player.dead);
        assertEquals(4, player.x, 0.01);
    }
}
