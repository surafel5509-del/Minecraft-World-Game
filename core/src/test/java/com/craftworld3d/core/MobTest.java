package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.entity.Mob;
import com.craftworld3d.core.entity.MobType;
import com.craftworld3d.core.entity.Player;
import com.craftworld3d.core.world.World;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class MobTest {
    private World world;
    private Player player;

    @Before
    public void setUp() {
        world = TestWorlds.flatWorld();
        player = new Player();
        world.player = player;
        player.setPosition(8.5, 64, 8.5);
    }

    @Test
    public void zombieChasesAndHurtsPlayer() {
        Mob zombie = new Mob(MobType.ZOMBIE);
        zombie.setPosition(8.5, 64, 14.5);
        float startHealth = player.health;
        double startDist = zombie.distSqTo(player);
        for (int i = 0; i < 200; i++) {
            zombie.tick(world);
        }
        assertTrue("zombie should approach", zombie.distSqTo(player) < startDist);
        assertTrue("player should be hurt", player.health < startHealth);
    }

    @Test
    public void passiveMobsDoNotAttack() {
        Mob pig = new Mob(MobType.PIG);
        pig.setPosition(9.5, 64, 9.5);
        for (int i = 0; i < 300; i++) pig.tick(world);
        assertEquals(20, player.health, 0.001);
    }

    @Test
    public void knockbackPushesVictim() {
        Mob pig = new Mob(MobType.PIG);
        pig.setPosition(9.5, 64, 9.5);
        pig.hurt(2, 8.5, 8.5); // hit from the player's direction
        assertTrue(pig.vx > 0 || pig.vz > 0);
        assertTrue(pig.justHurt);
        assertEquals(8, pig.health, 0.001);
        // Invulnerability frames.
        assertFalse(pig.hurt(2, 8.5, 8.5));
    }

    @Test
    public void deathMarksDropsPending() {
        Mob cow = new Mob(MobType.COW);
        cow.setPosition(10, 64, 10);
        cow.hurt(100, 8, 8);
        assertTrue(cow.dead);
        assertTrue(cow.deathDropsPending);
        var drops = cow.rollDrops(new Random(1));
        assertNotNull(drops);
        assertTrue(drops.count >= 1);
    }

    @Test
    public void creeperExplodesNearPlayerAndBreaksBlocks() {
        Mob creeper = new Mob(MobType.CREEPER);
        creeper.setPosition(9.0, 64, 9.0);
        int before = world.getBlockId(9, 63, 9);
        assertEquals(Blocks.GRASS.id, before);
        for (int i = 0; i < 120 && !creeper.dead; i++) {
            creeper.tick(world);
        }
        assertTrue("creeper should explode", creeper.dead);
        assertTrue("player damaged by blast", player.health < 20);
        assertEquals("grass destroyed by blast", 0, world.getBlockId(9, 63, 9));
    }

    @Test
    public void naturalMobsDespawnFarFromPlayer() {
        Mob zombie = new Mob(MobType.ZOMBIE);
        zombie.setPosition(500, 64, 500);
        zombie.tick(world);
        assertTrue(zombie.dead);

        Mob persistent = new Mob(MobType.ZOMBIE);
        persistent.naturalSpawn = false;
        persistent.setPosition(500, 64, 500);
        persistent.tick(world);
        assertFalse(persistent.dead);
    }

    @Test
    public void worldTickSkipsMobsOutsideActivationRange() {
        Mob far = new Mob(MobType.PIG);
        far.setPosition(8.5 + 80, 64, 8.5); // outside 64-block activation
        world.addEntity(far);
        world.tick(player.x, player.y, player.z, 64);
        assertEquals(0, far.ageTicks);

        Mob near = new Mob(MobType.PIG);
        near.setPosition(10, 64, 10);
        world.addEntity(near);
        world.tick(player.x, player.y, player.z, 64);
        assertEquals(1, near.ageTicks);
    }

    @Test
    public void skeletonShootsArrows() {
        Mob skeleton = new Mob(MobType.SKELETON);
        skeleton.setPosition(8.5, 64, 16.5); // 8 blocks away
        for (int i = 0; i < 10; i++) skeleton.tick(world);
        world.tick(player.x, player.y, player.z, 64);
        boolean hasArrow = false;
        for (var e : world.entities()) {
            if (e.typeKey().equals("arrow")) hasArrow = true;
        }
        assertTrue(hasArrow);
    }
}
