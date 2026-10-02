package com.craftworld3d.core;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.entity.Player;
import com.craftworld3d.core.item.ItemIds;
import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.item.Items;
import com.craftworld3d.core.progress.Achievement;
import com.craftworld3d.core.progress.AchievementManager;
import com.craftworld3d.core.progress.Stats;
import com.craftworld3d.core.save.WorldStorage;
import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.World;
import com.craftworld3d.core.world.blockentity.ChestEntity;
import com.craftworld3d.core.world.blockentity.FurnaceEntity;
import com.craftworld3d.core.world.gen.OverworldGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SaveLoadTest {
    private File dir;
    private WorldStorage storage;

    @Before
    public void setUp() throws Exception {
        Items.init();
        dir = Files.createTempDirectory("cw3d-test").toFile();
        storage = new WorldStorage(dir);
    }

    @After
    public void tearDown() {
        deleteRecursive(dir);
    }

    private static void deleteRecursive(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursive(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    @Test
    public void chunkRoundTrip() throws Exception {
        World world = new World(77L, World.DIM_OVERWORLD, new OverworldGenerator(77L));
        Chunk chunk = world.getOrGenerateChunk(3, -2);
        chunk.setBlockId(5, 70, 5, Blocks.GOLD_BLOCK.id);

        storage.saveChunk(world, chunk);
        assertTrue(storage.chunkExists(World.DIM_OVERWORLD, 3, -2));

        World world2 = new World(77L, World.DIM_OVERWORLD, new OverworldGenerator(77L));
        Chunk loaded = storage.loadChunk(world2, 3, -2);
        assertNotNull(loaded);
        assertArrayEquals(chunk.rawBlocks(), loaded.rawBlocks());
        assertEquals(Blocks.GOLD_BLOCK.id, loaded.getBlockId(5, 70, 5));
    }

    @Test
    public void blockEntitiesPersistWithChunk() throws Exception {
        World world = new World(1L, World.DIM_OVERWORLD, new TestWorlds.FlatGenerator());
        Chunk chunk = world.getOrGenerateChunk(0, 0);
        ChestEntity chest = new ChestEntity(4, 64, 4);
        chest.inventory.set(3, new ItemStack(ItemIds.DIAMOND, 7));
        world.setBlockEntity(chest);
        FurnaceEntity furnace = new FurnaceEntity(5, 64, 4);
        furnace.input = new ItemStack(Blocks.IRON_ORE.id, 3);
        furnace.burnTicksLeft = 55;
        world.setBlockEntity(furnace);

        storage.saveChunk(world, chunk);

        World world2 = new World(1L, World.DIM_OVERWORLD, new TestWorlds.FlatGenerator());
        storage.loadChunk(world2, 0, 0);
        ChestEntity chest2 = (ChestEntity) world2.getBlockEntity(4, 64, 4);
        assertNotNull(chest2);
        assertEquals(7, chest2.inventory.get(3).count);
        FurnaceEntity furnace2 = (FurnaceEntity) world2.getBlockEntity(5, 64, 4);
        assertNotNull(furnace2);
        assertEquals(3, furnace2.input.count);
        assertEquals(55, furnace2.burnTicksLeft);
    }

    @Test
    public void corruptedChunkRecoversFromBackup() throws Exception {
        World world = new World(5L, World.DIM_OVERWORLD, new TestWorlds.FlatGenerator());
        Chunk chunk = world.getOrGenerateChunk(1, 1);
        storage.saveChunk(world, chunk);           // first save (no .bak yet)
        chunk.setBlockId(0, 100, 0, Blocks.STONE.id);
        storage.saveChunk(world, chunk);           // second save creates .bak

        // Corrupt the main file.
        File f = new File(dir, "chunks/0/c.1.1.dat");
        assertTrue(f.isFile());
        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            raf.seek(20);
            raf.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        }

        World world2 = new World(5L, World.DIM_OVERWORLD, new TestWorlds.FlatGenerator());
        Chunk recovered = storage.loadChunk(world2, 1, 1);
        assertNotNull("backup should recover the chunk", recovered);
    }

    @Test
    public void totallyCorruptedChunkReturnsNull() throws Exception {
        File dimDir = new File(dir, "chunks/0");
        assertTrue(dimDir.mkdirs() || dimDir.isDirectory());
        try (FileOutputStream out = new FileOutputStream(new File(dimDir, "c.9.9.dat"))) {
            out.write("garbage-not-a-chunk".getBytes());
        }
        World world = new World(5L, World.DIM_OVERWORLD, new TestWorlds.FlatGenerator());
        assertNull(storage.loadChunk(world, 9, 9));
    }

    @Test
    public void levelJsonRoundTripWithPlayer() throws Exception {
        Player player = new Player();
        player.setPosition(10.5, 72, -4.25);
        player.yaw = 123.5f;
        player.pitch = -10f;
        player.health = 13.5f;
        player.hunger = 7;
        player.inventory.add(new ItemStack(Blocks.COBBLESTONE.id, 42));
        player.inventory.add(new ItemStack(ItemIds.IRON_PICKAXE, 1, 17));
        player.inventory.selectedSlot = 4;

        Map<String, Object> level = new LinkedHashMap<>();
        level.put("name", "My World");
        level.put("seed", 987654321L);
        level.put("time", 5555L);
        level.put("weather", "RAIN");
        level.put("player", player.save());
        storage.saveJson("level.json", level);

        Map<String, Object> loaded = storage.loadJson("level.json");
        assertNotNull(loaded);
        assertEquals("My World", MiniJson.getString(loaded, "name", ""));
        assertEquals(987654321L, MiniJson.getLong(loaded, "seed", 0));

        Player restored = new Player();
        //noinspection unchecked
        restored.load((Map<String, Object>) loaded.get("player"));
        assertEquals(10.5, restored.x, 1e-6);
        assertEquals(-4.25, restored.z, 1e-6);
        assertEquals(123.5f, restored.yaw, 1e-4);
        assertEquals(13.5f, restored.health, 1e-4);
        assertEquals(7f, restored.hunger, 1e-4);
        assertEquals(4, restored.inventory.selectedSlot);
        assertEquals(42, restored.inventory.count(Blocks.COBBLESTONE.id));
        ItemStack pick = null;
        for (int i = 0; i < restored.inventory.size(); i++) {
            ItemStack s = restored.inventory.get(i);
            if (s != null && s.itemId == ItemIds.IRON_PICKAXE) pick = s;
        }
        assertNotNull(pick);
        assertEquals(17, pick.damage);
    }

    @Test
    public void corruptedLevelJsonFallsBackToBackup() throws Exception {
        Map<String, Object> v1 = new LinkedHashMap<>();
        v1.put("name", "v1");
        storage.saveJson("level.json", v1);
        Map<String, Object> v2 = new LinkedHashMap<>();
        v2.put("name", "v2");
        storage.saveJson("level.json", v2); // creates .bak with v1

        try (FileOutputStream out = new FileOutputStream(new File(dir, "level.json"))) {
            out.write("{broken".getBytes());
        }
        Map<String, Object> loaded = storage.loadJson("level.json");
        assertNotNull(loaded);
        assertEquals("v1", MiniJson.getString(loaded, "name", ""));
    }

    @Test
    public void statsAndAchievementsRoundTrip() throws Exception {
        Stats stats = new Stats();
        stats.add(Stats.Stat.BLOCKS_MINED, 100);
        stats.add(Stats.Stat.DEATHS, 2);
        storage.saveJson("stats.json", stats.save());

        AchievementManager am = new AchievementManager();
        am.unlock(Achievement.FIRST_BLOCK);
        am.unlock(Achievement.FIRST_DIAMOND);
        storage.saveJson("achievements.json", am.save());

        Stats stats2 = new Stats();
        stats2.load(storage.loadJson("stats.json"));
        assertEquals(100, stats2.get(Stats.Stat.BLOCKS_MINED));
        assertEquals(2, stats2.get(Stats.Stat.DEATHS));
        assertEquals(0, stats2.get(Stats.Stat.MOBS_DEFEATED));

        AchievementManager am2 = new AchievementManager();
        am2.load(storage.loadJson("achievements.json"));
        assertTrue(am2.isUnlocked(Achievement.FIRST_BLOCK));
        assertTrue(am2.isUnlocked(Achievement.FIRST_DIAMOND));
        assertFalse(am2.isUnlocked(Achievement.FIRST_FLIGHT));
    }

    @Test
    public void worldListingShowsSavedWorlds() throws Exception {
        Map<String, Object> level = new LinkedHashMap<>();
        level.put("name", "Alpha");
        level.put("seed", 42L);
        level.put("lastPlayed", 1000L);
        storage.saveJson("level.json", level);

        var worlds = WorldStorage.listWorlds(dir.getParentFile());
        boolean found = false;
        for (var info : worlds) {
            if (info.id().equals(dir.getName())) {
                found = true;
                assertEquals("Alpha", info.name());
                assertEquals(42L, info.seed());
            }
        }
        assertTrue(found);
    }
}
