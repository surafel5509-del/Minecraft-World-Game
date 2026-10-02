package com.craftworld3d.game.engine;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.BlockRegistry;
import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.block.SoundType;
import com.craftworld3d.core.block.ToolType;
import com.craftworld3d.core.crafting.Recipes;
import com.craftworld3d.core.entity.DroppedItem;
import com.craftworld3d.core.entity.Entity;
import com.craftworld3d.core.entity.EntityFactory;
import com.craftworld3d.core.entity.LivingEntity;
import com.craftworld3d.core.entity.Mob;
import com.craftworld3d.core.entity.MobType;
import com.craftworld3d.core.entity.Player;
import com.craftworld3d.core.entity.Villager;
import com.craftworld3d.core.entity.vehicle.Vehicle;
import com.craftworld3d.core.entity.vehicle.VehicleType;
import com.craftworld3d.core.inventory.Inventory;
import com.craftworld3d.core.item.Item;
import com.craftworld3d.core.item.ItemIds;
import com.craftworld3d.core.item.ItemRegistry;
import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.item.ToolItem;
import com.craftworld3d.core.item.Items;
import com.craftworld3d.core.progress.Achievement;
import com.craftworld3d.core.progress.AchievementManager;
import com.craftworld3d.core.progress.Stats;
import com.craftworld3d.core.save.WorldStorage;
import com.craftworld3d.core.util.MathUtil;
import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.Chunk;
import com.craftworld3d.core.world.ChunkGenerator;
import com.craftworld3d.core.world.RaycastHit;
import com.craftworld3d.core.world.Weather;
import com.craftworld3d.core.world.World;
import com.craftworld3d.core.world.blockentity.ChestEntity;
import com.craftworld3d.core.world.blockentity.FurnaceEntity;
import com.craftworld3d.core.world.gen.DepthsGenerator;
import com.craftworld3d.core.world.gen.OverworldGenerator;
import com.craftworld3d.game.assets.AtlasData;
import com.craftworld3d.game.audio.SoundManager;
import com.craftworld3d.game.gl.ParticleSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Game loop and world lifecycle. Runs a fixed 20 TPS tick thread; all world
 * mutation happens there. UI-thread requests are queued as actions. The GL
 * renderer only reads immutable snapshots plus the thread-safe chunk map.
 */
public final class GameEngine {
    public static final float TPS = 20f;
    private static final long TICK_NANOS = 50_000_000L;
    private static final float REACH = 5f;
    private static final float AI_ACTIVATION_RANGE = 64f;
    private static final int AUTOSAVE_TICKS = 1200;     // 60 s

    /** UI callbacks, invoked on the tick thread; the UI must hop threads itself. */
    public interface Callbacks {
        void onOpenCrafting(boolean grid3x3);
        void onOpenFurnace(int x, int y, int z);
        void onOpenChest(int x, int y, int z);
        void onOpenTrading(Villager villager);
        void onDeath();
        void onAchievement(Achievement a);
        void onWorldSaved();
    }

    public final AtlasData atlasData;
    public final ControlState controls = new ControlState();
    /** Last FPS measured by the renderer; shown in the HUD. */
    public volatile int rendererFps;
    public final Stats stats = new Stats();
    public final AchievementManager achievements = new AchievementManager();

    private final SoundManager sounds;
    private final WorldStorage storage;
    private Callbacks callbacks;
    private ParticleSystem particles;

    private World overworld, depths;
    private volatile World activeWorld;
    private Player player;
    public String worldName = "World";
    public long seed;
    public int difficulty = 2;
    public String worldType = "default";
    private double spawnX, spawnY = 70, spawnZ;
    private long playTimeMsBase;
    private long sessionStartMs;

    // tick thread
    private Thread tickThread;
    private volatile boolean running;
    private volatile boolean paused;
    private volatile long lastTickNanos;
    private final ConcurrentLinkedQueue<Runnable> actions = new ConcurrentLinkedQueue<>();

    // chunk streaming
    private final ExecutorService genPool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ChunkGen");
        t.setDaemon(true);
        return t;
    });
    private final Set<Long> pendingChunks = new HashSet<>();
    private final ConcurrentLinkedQueue<Object[]> genResults = new ConcurrentLinkedQueue<>();

    // render snapshots
    private volatile List<EntityView> entityViews = new ArrayList<>();
    private final double[] playerLerp = new double[6];
    private volatile int[] aimBlockShared;
    private final RaycastHit aim = new RaycastHit();
    public volatile float breakProgress;

    // interaction state
    private Vehicle mounted;
    private int mineX, mineY, mineZ;
    private float mineProgress;
    private int flightTicks;
    private double stepAccum;
    private int autosaveTimer;
    private int spawnTimer;
    private int tickCount;
    private final Random rand = new Random();
    private boolean prevHorn;

    public GameEngine(File worldDir, AtlasData atlasData, SoundManager sounds) {
        Items.init();
        Blocks.init();
        Recipes.init();
        this.atlasData = atlasData;
        this.sounds = sounds;
        this.storage = new WorldStorage(worldDir);
    }

    public void setCallbacks(Callbacks cb) { this.callbacks = cb; }

    public void setParticles(ParticleSystem p) { this.particles = p; }

    // ---------------- world creation / loading ----------------

    /** Creates a new world directory with its level.json; returns the world id. */
    public static String createWorld(File worldsRoot, String name, long seed,
                                     int gamemode, int difficulty, String worldType) {
        String safe = name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (safe.isEmpty()) safe = "world";
        String id = safe.replace(' ', '_') + "-" + Long.toHexString(System.currentTimeMillis());
        File dir = new File(worldsRoot, id);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        Map<String, Object> level = new HashMap<>();
        level.put("name", name);
        level.put("seed", seed);
        level.put("gamemode", gamemode);
        level.put("difficulty", difficulty);
        level.put("worldType", worldType);
        level.put("lastPlayed", System.currentTimeMillis());
        level.put("playTimeMs", 0L);
        try {
            new WorldStorage(dir).saveJson("level.json", level);
        } catch (IOException e) {
            return null;
        }
        return id;
    }

    /** Loads level.json, builds both dimensions, loads or spawns the player. */
    public void load() {
        Map<String, Object> level = storage.loadJson("level.json");
        if (level == null) level = new HashMap<>();
        worldName = MiniJson.getString(level, "name", "World");
        seed = MiniJson.getLong(level, "seed", 1234);
        difficulty = MiniJson.getInt(level, "difficulty", 2);
        worldType = MiniJson.getString(level, "worldType", "default");
        playTimeMsBase = MiniJson.getLong(level, "playTimeMs", 0);

        ChunkGenerator overGen = "flat".equals(worldType)
                ? new FlatGenerator() : new OverworldGenerator(seed);
        overworld = new World(seed, World.DIM_OVERWORLD, overGen);
        depths = new World(seed, World.DIM_DEPTHS, new DepthsGenerator(seed));
        overworld.time = MiniJson.getLong(level, "time", 1000);
        overworld.weatherTicksLeft = MiniJson.getInt(level, "weatherTicks", 12000);
        try {
            overworld.weather = Weather.valueOf(MiniJson.getString(level, "weather", "CLEAR"));
        } catch (IllegalArgumentException ignored) {}
        depths.time = overworld.time;

        player = new Player();
        player.gamemode = MiniJson.getInt(level, "gamemode", Player.GAMEMODE_SURVIVAL);

        Map<String, Object> saved = asMap(level.get("player"));
        if (saved != null) {
            player.load(saved);
            spawnX = MiniJson.getDouble(level, "spawnX", player.x);
            spawnY = MiniJson.getDouble(level, "spawnY", player.y);
            spawnZ = MiniJson.getDouble(level, "spawnZ", player.z);
        }
        activeWorld = player.dimension == World.DIM_DEPTHS ? depths : overworld;
        activeWorld.player = player;

        // make sure the spawn area exists before the first frame
        if (saved == null) {
            ensureAreaSync(activeWorld, 0, 0, 1);
            int h = Math.max(activeWorld.surfaceHeight(0, 0), World.SEA_LEVEL);
            spawnX = 0.5;
            spawnY = h + 1;
            spawnZ = 0.5;
            player.setPosition(spawnX, spawnY, spawnZ);
            if (player.gamemode == Player.GAMEMODE_CREATIVE) giveCreativeKit();
        }
        ensureAreaSync(activeWorld,
                MathUtil.floor(player.x) >> 4, MathUtil.floor(player.z) >> 4, 1);

        loadEntities(level, "entities0", overworld);
        loadEntities(level, "entitiesM1", depths);

        Map<String, Object> st = storage.loadJson("stats.json");
        if (st != null) stats.load(st);
        Map<String, Object> ach = storage.loadJson("achievements.json");
        if (ach != null) achievements.load(ach);

        overworld.setListener((x, y, z, oldId, newId) -> {});
        sessionStartMs = System.currentTimeMillis();
        syncPlayerLerp();
    }

    private void giveCreativeKit() {
        Inventory inv = player.inventory;
        inv.add(new ItemStack(Blocks.STONE.id, 64));
        inv.add(new ItemStack(Blocks.OAK_PLANKS.id, 64));
        inv.add(new ItemStack(Blocks.GLASS.id, 64));
        inv.add(new ItemStack(Blocks.TORCH.id, 64));
        inv.add(new ItemStack(ItemIds.DIAMOND_PICKAXE, 1));
    }

    private void loadEntities(Map<String, Object> level, String key, World world) {
        Object list = level.get(key);
        if (!(list instanceof List<?> l)) return;
        for (Object o : l) {
            Map<String, Object> m = asMap(o);
            if (m == null) continue;
            Entity e = EntityFactory.fromSave(m);
            if (e != null) world.addEntity(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    // ---------------- tick thread ----------------

    public void start() {
        if (running) return;
        running = true;
        tickThread = new Thread(this::tickLoop, "GameTick");
        tickThread.start();
    }

    public void setPaused(boolean p) { paused = p; }

    public boolean isPaused() { return paused; }

    /** Stops the tick thread and performs the final save. */
    public void stop() {
        running = false;
        if (tickThread != null) {
            try { tickThread.join(2000); } catch (InterruptedException ignored) {}
        }
        genPool.shutdownNow();
        saveEverything(true);
    }

    private void tickLoop() {
        long next = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            if (now < next) {
                try { Thread.sleep(Math.max(1, (next - now) / 1_000_000)); }
                catch (InterruptedException e) { return; }
                continue;
            }
            next += TICK_NANOS;
            if (next < now - 10 * TICK_NANOS) next = now; // catch-up cap
            try {
                if (!paused) tick();
                else lastTickNanos = System.nanoTime();
            } catch (Exception e) {
                // keep the game alive; a single bad tick must not crash the app
            }
        }
    }

    private void tick() {
        tickCount++;
        Runnable a;
        while ((a = actions.poll()) != null) a.run();

        World world = activeWorld;

        // ---- input -> player / vehicle ----
        if (mounted != null) {
            mounted.throttleInput = controls.vehicleThrottle;
            mounted.steerInput = controls.vehicleSteer;
            mounted.pitchInput = controls.vehiclePitch;
            mounted.headlightsOn = controls.headlights;
            if (controls.horn && !prevHorn) sounds.play("horn");
            prevHorn = controls.horn;
            if (mounted.dead) {
                dismount();
            } else {
                player.setPosition(mounted.x, mounted.seatY(), mounted.z);
                player.vx = player.vy = player.vz = 0;
                player.fallDistance = 0;
                trackFlight();
                engineSound();
            }
        } else {
            player.inputForward = controls.moveZ;
            player.inputStrafe = controls.moveX;
            player.wantJump = controls.jump;
            player.wantSprint = controls.sprint;
            player.wantCrouch = controls.crouch;
        }

        double prevX = player.x, prevY = player.y, prevZ = player.z;
        playerLerp[0] = prevX; playerLerp[1] = prevY; playerLerp[2] = prevZ;

        float healthBefore = player.health;
        if (mounted == null) {
            player.tick(world);
        }

        world.tick(player.x, player.y, player.z, AI_ACTIVATION_RANGE);
        if (world == overworld) depths.time = overworld.time;

        playerLerp[3] = player.x; playerLerp[4] = player.y; playerLerp[5] = player.z;

        // ---- gameplay systems ----
        handleMining(world);
        handlePortal(world);
        handleSounds(world, healthBefore, prevX, prevZ);
        handleMobDeaths(world);
        streamChunks(world);
        if (spawnTimer++ >= 40) {
            spawnTimer = 0;
            trySpawnMobs(world);
        }
        if (world.explosionHappened) {
            sounds.play("explosion");
            if (particles != null) {
                particles.explosion(atlasData.tileIndex("stone"),
                        (float) player.x, (float) player.y + 1, (float) player.z);
            }
        }
        if (world.itemPickedUp) sounds.play("click", 0.6f, 1.4f);

        stats.add(Stats.Stat.DISTANCE_TRAVELED_M,
                (long) Math.sqrt(sq(player.x - prevX) + sq(player.z - prevZ)));
        if (tickCount % 20 == 0) stats.add(Stats.Stat.TIME_PLAYED_SECONDS, 1);

        if (player.justDied) {
            stats.add(Stats.Stat.DEATHS, 1);
            sounds.play("hurt", 1f, 0.6f);
            if (callbacks != null) callbacks.onDeath();
        }

        if (++autosaveTimer >= AUTOSAVE_TICKS) {
            autosaveTimer = 0;
            saveEverything(false);
            if (callbacks != null) callbacks.onWorldSaved();
        }

        snapshotEntities(world);
        updateAim(world);
        lastTickNanos = System.nanoTime();
    }

    private static double sq(double v) { return v * v; }

    // ---------------- aiming & mining ----------------

    private void updateAim(World world) {
        double[] d = lookDir();
        world.raycast(player.x, player.eyeY(), player.z, d[0], d[1], d[2], REACH, aim);
        if (aim.hit) {
            aimBlockShared = new int[]{aim.x, aim.y, aim.z, aim.faceX, aim.faceY, aim.faceZ};
        } else {
            aimBlockShared = null;
        }
    }

    private double[] lookDir() {
        double yawRad = Math.toRadians(player.yaw);
        double pitchRad = Math.toRadians(player.pitch);
        return new double[]{
                -Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)};
    }

    private void handleMining(World world) {
        if (!controls.breaking || mounted != null || player.dead) {
            mineProgress = 0;
            breakProgress = 0;
            return;
        }
        if (!aim.hit) { mineProgress = 0; breakProgress = 0; return; }
        if (aim.x != mineX || aim.y != mineY || aim.z != mineZ) {
            mineX = aim.x; mineY = aim.y; mineZ = aim.z;
            mineProgress = 0;
        }
        Block block = world.getBlock(mineX, mineY, mineZ);
        if (block.isAir() || block.isUnbreakable() || block.liquid) {
            mineProgress = 0;
            breakProgress = 0;
            return;
        }
        if (player.gamemode == Player.GAMEMODE_CREATIVE) {
            breakBlock(world, block, false);
            return;
        }
        ItemStack held = player.inventory.selected();
        float speedFactor = 1f;
        if (held.item() instanceof ToolItem tool) speedFactor = tool.speedAgainst(block);
        float ticksNeeded = Math.max(3, block.hardness * 30f / speedFactor);
        mineProgress += 1f / ticksNeeded;
        breakProgress = Math.min(1f, mineProgress);
        if (tickCount % 5 == 0) sounds.play(digSound(block.sound), 0.5f, 1f);
        if (mineProgress >= 1f) {
            breakBlock(world, block, true);
        }
    }

    private void breakBlock(World world, Block block, boolean survival) {
        world.setBlock(mineX, mineY, mineZ, 0);
        mineProgress = 0;
        breakProgress = 0;
        sounds.play(digSound(block.sound));
        if (particles != null) {
            particles.burst(atlasData.tileIndex(block.texSide),
                    mineX, mineY, mineZ, 10);
        }
        stats.add(Stats.Stat.BLOCKS_MINED, 1);
        unlock(Achievement.FIRST_BLOCK);
        if (block.id == Blocks.DIAMOND_ORE.id) unlock(Achievement.FIRST_DIAMOND);

        if (!survival) return;
        ItemStack held = player.inventory.selected();
        boolean harvest = true;
        if (block.toolRequired) {
            harvest = held.item() instanceof ToolItem tool && tool.canHarvest(block);
        }
        if (harvest && block.dropItem != -2) {
            int itemId = block.dropItem == -1 ? block.id : block.dropItem;
            int n = block.dropCountMin
                    + (block.dropCountMax > block.dropCountMin
                    ? rand.nextInt(block.dropCountMax - block.dropCountMin + 1) : 0);
            if (itemId > 0 && n > 0) {
                world.addEntity(new DroppedItem(new ItemStack(itemId, n),
                        mineX + 0.5, mineY + 0.3, mineZ + 0.5));
            }
        }
        if (held.item() instanceof ToolItem) {
            if (held.damageItem()) {
                player.inventory.set(player.inventory.selectedSlot, ItemStack.EMPTY);
                sounds.play("dig_glass", 0.8f, 0.7f);
            }
        }
    }

    // ---------------- use / place / interact (UI thread entry points) ----------------

    public void runOnTick(Runnable r) { actions.add(r); }

    public void selectHotbarSlot(int slot) {
        if (slot >= 0 && slot < Inventory.HOTBAR_SIZE) {
            player.inventory.selectedSlot = slot;
            sounds.play("click", 0.5f, 1.2f);
        }
    }

    /** Look rotation, applied immediately for responsive aiming. */
    public void addLook(float dYawDeg, float dPitchDeg) {
        if (player == null) return;
        player.yaw += dYawDeg;
        player.pitch = MathUtil.clamp(player.pitch + dPitchDeg, -89.5f, 89.5f);
    }

    /** The "use / place" button. */
    public void useTapped() {
        runOnTick(() -> {
            if (player.dead) return;
            World world = activeWorld;
            if (mounted != null) return;

            // entity under the crosshair?
            Entity target = entityUnderCrosshair(world);
            if (target instanceof Vehicle v) { mount(v); return; }
            if (target instanceof Villager v) {
                if (callbacks != null) callbacks.onOpenTrading(v);
                return;
            }

            if (aim.hit && interactBlock(world, aim.x, aim.y, aim.z)) return;

            ItemStack held = player.inventory.selected();
            if (held.isEmpty()) return;
            Item item = held.item();

            // vehicles are placed into the world
            VehicleType vt = VehicleType.byItem(held.itemId);
            if (vt != null && aim.hit) {
                Vehicle v = Vehicle.create(vt);
                v.setPosition(aim.x + aim.faceX + 0.5, aim.y + aim.faceY, aim.z + aim.faceZ + 0.5);
                v.yaw = player.yaw;
                world.addEntity(v);
                consumeHeld();
                sounds.play("place");
                return;
            }

            if (item != null && item.isFood()) {
                if (player.eatSelected()) sounds.play("eat");
                return;
            }

            if (item != null && item.isPlaceable() && aim.hit) {
                placeBlock(world, item.placesBlock);
            }
        });
    }

    /** Attack tap: hit the entity under the crosshair (used by the break button edge). */
    public void attackTapped() {
        runOnTick(() -> {
            if (player.dead || mounted != null) return;
            World world = activeWorld;
            Entity target = entityUnderCrosshair(world);
            if (target == null) return;
            ItemStack held = player.inventory.selected();
            float dmg = 1;
            if (held.item() instanceof ToolItem tool) {
                dmg = tool.attackDamage;
                if (held.damageItem()) {
                    player.inventory.set(player.inventory.selectedSlot, ItemStack.EMPTY);
                }
            }
            if (target instanceof LivingEntity le) {
                le.hurt(dmg, player.x, player.z);
                sounds.play("hurt", 0.8f, 1.1f);
            } else if (target instanceof Vehicle v) {
                v.damage(dmg * 4);
                sounds.play("dig_stone", 0.6f, 0.8f);
            }
        });
    }

    private Entity entityUnderCrosshair(World world) {
        double[] d = lookDir();
        double ox = player.x, oy = player.eyeY(), oz = player.z;
        Entity best = null;
        double bestT = aim.hit ? aim.distance : REACH;
        for (Entity e : world.entities()) {
            if (e == player || e.dead || e instanceof DroppedItem) continue;
            double t = rayBoxIntersect(ox, oy, oz, d[0], d[1], d[2],
                    e.x - e.width / 2, e.y, e.z - e.width / 2,
                    e.x + e.width / 2, e.y + e.height, e.z + e.width / 2);
            if (t >= 0 && t < bestT) { bestT = t; best = e; }
        }
        return best;
    }

    private static double rayBoxIntersect(double ox, double oy, double oz,
                                          double dx, double dy, double dz,
                                          double minX, double minY, double minZ,
                                          double maxX, double maxY, double maxZ) {
        double tMin = 0, tMax = 64;
        double[] o = {ox, oy, oz}, d = {dx, dy, dz};
        double[] lo = {minX, minY, minZ}, hi = {maxX, maxY, maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (o[i] < lo[i] || o[i] > hi[i]) return -1;
            } else {
                double t1 = (lo[i] - o[i]) / d[i];
                double t2 = (hi[i] - o[i]) / d[i];
                tMin = Math.max(tMin, Math.min(t1, t2));
                tMax = Math.min(tMax, Math.max(t1, t2));
                if (tMin > tMax) return -1;
            }
        }
        return tMin;
    }

    /** Returns true if the tapped block consumed the interaction. */
    private boolean interactBlock(World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        if (id == Blocks.CRAFTING_TABLE.id) {
            if (callbacks != null) callbacks.onOpenCrafting(true);
            return true;
        }
        if (id == Blocks.FURNACE.id || id == Blocks.FURNACE_LIT.id) {
            if (world.getBlockEntity(x, y, z) == null) {
                world.setBlockEntity(new FurnaceEntity(x, y, z));
            }
            if (callbacks != null) callbacks.onOpenFurnace(x, y, z);
            return true;
        }
        if (id == Blocks.CHEST.id) {
            if (world.getBlockEntity(x, y, z) == null) {
                world.setBlockEntity(new ChestEntity(x, y, z));
            }
            if (callbacks != null) callbacks.onOpenChest(x, y, z);
            return true;
        }
        if (id == Blocks.DOOR_CLOSED.id || id == Blocks.DOOR_OPEN.id
                || id == Blocks.LEVER.id || id == Blocks.LEVER_ON.id
                || id == Blocks.BUTTON.id || id == Blocks.REPEATER.id) {
            boolean handled = world.redstone.interact(world, x, y, z);
            if (handled) sounds.play("click");
            return handled;
        }
        // igniting an obsidian block with a nether crystal builds a portal
        if (id == Blocks.OBSIDIAN.id
                && player.inventory.selected().itemId == ItemIds.NETHER_CRYSTAL) {
            buildPortal(world, x, y + 1, z);
            consumeHeld();
            sounds.play("portal");
            return true;
        }
        return false;
    }

    private void placeBlock(World world, int blockId) {
        int px = aim.x + aim.faceX, py = aim.y + aim.faceY, pz = aim.z + aim.faceZ;
        if (py < 0 || py >= Chunk.SIZE_Y) return;
        Block existing = world.getBlock(px, py, pz);
        if (!existing.isAir() && !existing.liquid && existing.shape != com.craftworld3d.core.block.RenderShape.CROSS) return;

        Block placing = BlockRegistry.get(blockId);
        if (placing == null) return;
        // don't place a solid block inside the player
        if (placing.collidable) {
            com.craftworld3d.core.util.AABB blockBox = new com.craftworld3d.core.util.AABB(
                    px, py, pz, px + 1, py + 1, pz + 1);
            if (blockBox.intersects(player.boundingBox())) return;
        }
        if (!world.setBlock(px, py, pz, blockId)) return;
        if (blockId == Blocks.CHEST.id) world.setBlockEntity(new ChestEntity(px, py, pz));
        if (blockId == Blocks.FURNACE.id) world.setBlockEntity(new FurnaceEntity(px, py, pz));
        if (player.gamemode != Player.GAMEMODE_CREATIVE) consumeHeld();
        stats.add(Stats.Stat.BLOCKS_PLACED, 1);
        if (blockId == Blocks.DOOR_CLOSED.id) unlock(Achievement.FIRST_HOUSE);
        sounds.play("place");
    }

    private void consumeHeld() {
        if (player.gamemode == Player.GAMEMODE_CREATIVE) return;
        player.inventory.shrink(player.inventory.selectedSlot);
    }

    // ---------------- vehicles ----------------

    private void mount(Vehicle v) {
        // refuel with coal instead of mounting
        ItemStack held = player.inventory.selected();
        if (held.itemId == ItemIds.COAL && v.type.needsFuel()) {
            if (v.addFuel(1)) {
                consumeHeld();
                sounds.play("click");
            }
            return;
        }
        mounted = v;
        v.mounted = true;
        flightTicks = 0;
        controls.vehicleThrottle = 0;
        unlockVehicleUsed();
    }

    private void unlockVehicleUsed() {
        unlock(Achievement.FIRST_VEHICLE);
    }

    public void dismountRequested() { runOnTick(this::dismount); }

    private void dismount() {
        if (mounted == null) return;
        Vehicle v = mounted;
        v.mounted = false;
        mounted = null;
        sounds.stopLoop("engine");
        player.setPosition(v.x + 1.2, v.y + 0.5, v.z);
        player.vx = player.vy = player.vz = 0;
    }

    public boolean isMounted() { return mounted != null; }

    public Vehicle mountedVehicle() { return mounted; }

    private void trackFlight() {
        if (mounted != null && mounted.airborne
                && (mounted.type.kind == VehicleType.Kind.PLANE
                || mounted.type.kind == VehicleType.Kind.HELICOPTER)) {
            if (++flightTicks > 40) unlock(Achievement.FIRST_FLIGHT);
        } else {
            flightTicks = 0;
        }
    }

    private void engineSound() {
        if (mounted != null && mounted.type.needsFuel() && mounted.fuel > 0
                && Math.abs(controls.vehicleThrottle) > 0.05f) {
            sounds.loop("engine", 0.5f);
        } else {
            sounds.stopLoop("engine");
        }
    }

    // ---------------- portal & dimensions ----------------

    private void buildPortal(World world, int x, int y, int z) {
        for (int dy = -1; dy <= 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                boolean frame = dx == -1 || dx == 1 || dy == -1 || dy == 3;
                world.setBlock(x + dx, y + dy, z,
                        frame ? Blocks.OBSIDIAN.id : Blocks.PORTAL.id);
            }
        }
    }

    private void handlePortal(World world) {
        if (!player.canEnterPortal()) return;
        int bx = MathUtil.floor(player.x), by = MathUtil.floor(player.y + 0.5), bz = MathUtil.floor(player.z);
        if (world.getBlockId(bx, by, bz) != Blocks.PORTAL.id) return;

        World target = world == overworld ? depths : overworld;
        player.notePortalUsed();
        unlock(Achievement.FIRST_DIMENSION);
        sounds.play("portal");

        int tcx = bx >> 4, tcz = bz >> 4;
        ensureAreaSync(target, tcx, tcz, 1);
        int h = target.surfaceHeight(bx, bz);
        if (h < 2) h = target.dimension == World.DIM_DEPTHS ? DepthsGenerator.LAVA_LEVEL + 4 : World.SEA_LEVEL;
        // arrival platform + return portal
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                target.setBlock(bx + dx, h, bz + dz,
                        target.dimension == World.DIM_DEPTHS ? Blocks.NETHERRACK.id : Blocks.STONE.id);
                for (int dy = 1; dy <= 4; dy++) target.setBlock(bx + dx, h + dy, bz + dz, 0);
            }
        }
        buildPortal(target, bx, h + 1, bz - 2);

        switchDimension(target, bx + 0.5, h + 1, bz + 0.5);
    }

    private void switchDimension(World target, double x, double y, double z) {
        World old = activeWorld;
        old.player = null;
        target.player = player;
        player.dimension = target.dimension;
        player.setPosition(x, y, z);
        player.vx = player.vy = player.vz = 0;
        player.fallDistance = 0;
        activeWorld = target;
        syncPlayerLerp();
    }

    private void syncPlayerLerp() {
        playerLerp[0] = playerLerp[3] = player.x;
        playerLerp[1] = playerLerp[4] = player.y;
        playerLerp[2] = playerLerp[5] = player.z;
    }

    // ---------------- mobs ----------------

    private void trySpawnMobs(World world) {
        boolean inDepths = world.dimension == World.DIM_DEPTHS;
        int hostiles = 0, passives = 0;
        for (Entity e : world.entities()) {
            if (e instanceof Villager) continue;
            if (e instanceof Mob m) {
                if (m.type.hostile) hostiles++;
                else passives++;
            }
        }
        boolean night = world.isNight();
        boolean wantHostile = difficulty > 0 && (night || inDepths) && hostiles < 8 + difficulty * 2;
        boolean wantPassive = !inDepths && !night && passives < 8;
        if (!wantHostile && !wantPassive) return;

        double ang = rand.nextDouble() * Math.PI * 2;
        double dist = 24 + rand.nextDouble() * 24;
        int sx = MathUtil.floor(player.x + Math.cos(ang) * dist);
        int sz = MathUtil.floor(player.z + Math.sin(ang) * dist);
        if (world.getLoadedChunk(sx >> 4, sz >> 4) == null) return;
        int sy = world.surfaceHeight(sx, sz) + 1;
        if (sy <= 1 || sy >= Chunk.SIZE_Y - 2) return;
        Block ground = world.getBlock(sx, sy - 1, sz);
        if (!ground.collidable || ground.liquid) return;

        MobType type;
        if (wantHostile) {
            MobType[] pool = inDepths
                    ? new MobType[]{MobType.ZOMBIE, MobType.DEPTH_FIEND, MobType.SPIDER}
                    : new MobType[]{MobType.ZOMBIE, MobType.SKELETON, MobType.CREEPER, MobType.SPIDER};
            type = pool[rand.nextInt(pool.length)];
        } else {
            MobType[] pool = {MobType.PIG, MobType.COW, MobType.SHEEP, MobType.CHICKEN};
            type = pool[rand.nextInt(pool.length)];
        }
        Mob mob = new Mob(type);
        mob.setPosition(sx + 0.5, sy, sz + 0.5);
        world.addEntity(mob);
    }

    private void handleMobDeaths(World world) {
        for (Entity e : world.entities()) {
            if (e instanceof Mob m && m.deathDropsPending) {
                m.deathDropsPending = false;
                stats.add(Stats.Stat.MOBS_DEFEATED, 1);
                ItemStack drop = m.rollDrops(rand);
                if (drop != null) {
                    world.addEntity(new DroppedItem(drop, m.x, m.y + 0.3, m.z));
                }
                sounds.playAt(mobSound(m.type), Math.sqrt(m.distSqTo(player)));
            }
        }
    }

    // ---------------- ambient sound ----------------

    private void handleSounds(World world, float healthBefore, double prevX, double prevZ) {
        if (player.health < healthBefore - 0.4f) sounds.play("hurt");

        // footsteps
        if (player.onGround && mounted == null) {
            stepAccum += Math.sqrt(sq(player.x - prevX) + sq(player.z - prevZ));
            if (stepAccum > 2.2) {
                stepAccum = 0;
                Block under = world.getBlock(MathUtil.floor(player.x),
                        MathUtil.floor(player.y - 0.5), MathUtil.floor(player.z));
                sounds.play(stepSound(under.sound), 0.4f, 0.9f + rand.nextFloat() * 0.2f);
            }
        }
        if (player.inWater && tickCount % 30 == 0 && Math.abs(player.vx) + Math.abs(player.vz) > 0.5) {
            sounds.play("splash", 0.4f, 1f);
        }

        // weather
        if (world == overworld && world.weather.isPrecipitating()) {
            sounds.loop("rain", 0.5f);
            if (world.weather == Weather.THUNDER && rand.nextInt(300) == 0) {
                sounds.play("thunder", 1f, 0.8f + rand.nextFloat() * 0.4f);
            }
        } else {
            sounds.stopLoop("rain");
        }

        // creeper fuses nearby
        for (Entity e : world.entities()) {
            if (e instanceof Mob m && m.fuse >= 0 && m.fuse % 20 == 10) {
                sounds.playAt("creeper_fuse", Math.sqrt(m.distSqTo(player)));
            }
        }
    }

    private static String stepSound(SoundType t) {
        return switch (t) {
            case GRASS, CLOTH -> "step_grass";
            case WOOD -> "step_wood";
            case SAND, GRAVEL, SNOW -> "step_sand";
            default -> "step_stone";
        };
    }

    private static String digSound(SoundType t) {
        return switch (t) {
            case GRASS, CLOTH -> "dig_grass";
            case WOOD -> "dig_wood";
            case SAND, GRAVEL, SNOW -> "dig_sand";
            case GLASS -> "dig_glass";
            default -> "dig_stone";
        };
    }

    private static String mobSound(MobType t) {
        return switch (t) {
            case PIG -> "mob_pig";
            case COW -> "mob_cow";
            case SHEEP -> "mob_sheep";
            case CHICKEN -> "mob_chicken";
            default -> "mob_zombie";
        };
    }

    // ---------------- chunk streaming ----------------

    private void streamChunks(World world) {
        // completed generation jobs
        Object[] res;
        while ((res = genResults.poll()) != null) {
            World w = (World) res[0];
            Chunk chunk = (Chunk) res[1];
            boolean fresh = (Boolean) res[2];
            pendingChunks.remove(streamKey(w.dimension, chunk.chunkX, chunk.chunkZ));
            if (w != activeWorld) continue;
            w.putChunk(chunk);
            if (fresh && w == overworld && w.generator instanceof OverworldGenerator og) {
                for (double[] pos : og.villagerSpawns(chunk.chunkX, chunk.chunkZ)) {
                    Villager v = new Villager();
                    v.setPosition(pos[0], pos[1], pos[2]);
                    w.addEntity(v);
                }
            }
        }

        int radius = Math.min(10, Math.max(3, rdChunks()));
        int pcx = MathUtil.floor(player.x) >> 4;
        int pcz = MathUtil.floor(player.z) >> 4;

        // request missing chunks, nearest first, limited per tick
        int requested = 0;
        for (int r = 0; r <= radius && requested < 6; r++) {
            for (int dx = -r; dx <= r && requested < 6; dx++) {
                for (int dz = -r; dz <= r && requested < 6; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    if (world.getLoadedChunk(cx, cz) != null) continue;
                    long key = streamKey(world.dimension, cx, cz);
                    if (!pendingChunks.add(key)) continue;
                    requested++;
                    World w = world;
                    genPool.submit(() -> {
                        Chunk c = storage.loadChunk(w, cx, cz);
                        boolean fresh = false;
                        if (c == null) {
                            c = new Chunk(cx, cz);
                            w.generator.generate(c);
                            c.recalcHeightMap();
                            c.recalcLight();
                            c.ready = true;
                            fresh = true;
                        }
                        genResults.add(new Object[]{w, c, fresh});
                    });
                }
            }
        }

        // unload far chunks (save a couple per tick to spread IO)
        int saved = 0;
        for (Chunk c : world.loadedChunks()) {
            int dx = c.chunkX - pcx, dz = c.chunkZ - pcz;
            if (Math.max(Math.abs(dx), Math.abs(dz)) <= radius + 2) continue;
            if (saved >= 2) break;
            world.removeChunk(c.chunkX, c.chunkZ);
            if (c.modified) {
                saved++;
                try { storage.saveChunk(world, c); } catch (IOException ignored) {}
            }
        }
    }

    private int rdChunks() {
        // render distance setting is injected via setRenderDistance
        return renderDistance;
    }

    private volatile int renderDistance = 5;

    public void setRenderDistance(int chunks) { renderDistance = chunks; }

    private static long streamKey(int dim, int cx, int cz) {
        return (((long) cx << 32) ^ (cz & 0xFFFFFFFFL)) * 31 + dim;
    }

    /** Synchronously loads/generates a small area (first spawn, portals). */
    private void ensureAreaSync(World world, int ccx, int ccz, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = ccx + dx, cz = ccz + dz;
                if (world.getLoadedChunk(cx, cz) != null) continue;
                Chunk c = storage.loadChunk(world, cx, cz);
                if (c == null) {
                    c = new Chunk(cx, cz);
                    world.generator.generate(c);
                    c.recalcHeightMap();
                    c.recalcLight();
                    c.ready = true;
                }
                world.putChunk(c);
            }
        }
    }

    // ---------------- saving ----------------

    private void saveEverything(boolean full) {
        try {
            Map<String, Object> level = new HashMap<>();
            level.put("name", worldName);
            level.put("seed", seed);
            level.put("gamemode", player.gamemode);
            level.put("difficulty", difficulty);
            level.put("worldType", worldType);
            level.put("lastPlayed", System.currentTimeMillis());
            long sessionMs = System.currentTimeMillis() - sessionStartMs;
            level.put("playTimeMs", playTimeMsBase + sessionMs);
            level.put("time", overworld.time);
            level.put("weather", overworld.weather.name());
            level.put("weatherTicks", overworld.weatherTicksLeft);
            level.put("spawnX", spawnX);
            level.put("spawnY", spawnY);
            level.put("spawnZ", spawnZ);
            level.put("player", player.save());
            level.put("entities0", saveEntities(overworld));
            level.put("entitiesM1", saveEntities(depths));
            storage.saveJson("level.json", level);
            storage.saveJson("stats.json", stats.save());
            storage.saveJson("achievements.json", achievements.save());

            for (World w : new World[]{overworld, depths}) {
                for (Chunk c : w.loadedChunks()) {
                    if (c.modified) {
                        storage.saveChunk(w, c);
                        c.modified = false;
                    }
                }
            }
        } catch (IOException ignored) {
            // disk full etc. — keep playing; next autosave retries
        }
    }

    private List<Object> saveEntities(World world) {
        List<Object> out = new ArrayList<>();
        for (Entity e : world.entities()) {
            if (e == player || e.dead) continue;
            out.add(e.save());
        }
        return out;
    }

    /** Manual save from the pause menu (synchronous on the tick queue). */
    public void saveRequested() {
        runOnTick(() -> {
            saveEverything(true);
            if (callbacks != null) callbacks.onWorldSaved();
        });
    }

    public void respawnRequested() {
        runOnTick(() -> {
            if (activeWorld != overworld) {
                switchDimension(overworld, spawnX, spawnY, spawnZ);
            }
            ensureAreaSync(overworld, MathUtil.floor(spawnX) >> 4, MathUtil.floor(spawnZ) >> 4, 1);
            player.respawn(spawnX, Math.max(spawnY, overworld.surfaceHeight(
                    MathUtil.floor(spawnX), MathUtil.floor(spawnZ)) + 1), spawnZ);
            syncPlayerLerp();
        });
    }

    // ---------------- crafting / achievements ----------------

    public void onItemCrafted(ItemStack result) {
        stats.add(Stats.Stat.ITEMS_CRAFTED, result.count);
        unlock(Achievement.FIRST_CRAFT);
        Item item = result.item();
        if (item != null && item.isTool()) unlock(Achievement.FIRST_TOOL);
        if (VehicleType.byItem(result.itemId) != null) unlock(Achievement.FIRST_VEHICLE);
        sounds.play("click");
    }

    private void unlock(Achievement a) {
        if (achievements.unlock(a)) {
            sounds.play("achievement");
            if (callbacks != null) callbacks.onAchievement(a);
        }
    }

    // ---------------- render thread accessors ----------------

    public World renderWorld() { return activeWorld; }

    public World worldForDimension(int dim) {
        return dim == World.DIM_DEPTHS ? depths : overworld;
    }

    public Player player() { return player; }

    public List<EntityView> entityViews() { return entityViews; }

    public int[] aimBlock() { return aimBlockShared; }

    public float tickAlpha() {
        long dt = System.nanoTime() - lastTickNanos;
        return Math.max(0f, Math.min(1f, dt / (float) TICK_NANOS));
    }

    /** Interpolated eye position for the camera. */
    public void playerEyePos(float alpha, double[] out) {
        out[0] = lerp(playerLerp[0], playerLerp[3], alpha);
        out[1] = lerp(playerLerp[1], playerLerp[4], alpha) + player.eyeHeight();
        out[2] = lerp(playerLerp[2], playerLerp[5], alpha);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private void snapshotEntities(World world) {
        List<EntityView> views = new ArrayList<>(world.entities().size());
        for (Entity e : world.entities()) {
            if (e == player || e.dead) continue;
            int tile;
            float w = e.width, h = e.height;
            boolean vehicle = false;
            if (e instanceof Vehicle v) {
                tile = atlasData.tileIndex(vehicleTile(v.type));
                vehicle = true;
                w = Math.max(1.6f, v.width);
            } else if (e instanceof Villager) {
                tile = atlasData.tileIndex("mob_villager");
            } else if (e instanceof Mob m) {
                tile = atlasData.tileIndex(m.type == MobType.DEPTH_FIEND
                        ? "mob_fiend" : "mob_" + m.type.name().toLowerCase());
            } else if (e instanceof DroppedItem di) {
                tile = atlasData.iconTileForItem(di.stack.itemId);
                w = 0.35f;
                h = 0.35f;
            } else { // arrow
                tile = atlasData.iconTileForItem(ItemIds.ARROW);
                w = 0.3f;
                h = 0.3f;
            }
            double px = e.x, py = e.y, pz = e.z; // previous tick pos approximated by current
            boolean flash = e instanceof LivingEntity le && le.hurtTicks > 0;
            views.add(new EntityView(tile, w, h, px, py, pz, e.x, e.y, e.z, flash, vehicle));
        }
        entityViews = views;
    }

    private static String vehicleTile(VehicleType t) {
        return switch (t) {
            case SEDAN -> "vehicle_red";
            case SUV -> "vehicle_blue";
            case TRUCK, MINECART -> "vehicle_gray";
            case SPORTS_CAR -> "vehicle_yellow";
            case JEEP -> "vehicle_green";
            default -> "vehicle_white";
        };
    }

    // ---------------- flat generator (superflat world type) ----------------

    private static final class FlatGenerator implements ChunkGenerator {
        @Override
        public int surfaceHeight(int worldX, int worldZ) { return 63; }

        @Override
        public void generate(Chunk chunk) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlockId(x, 0, z, Blocks.BEDROCK.id);
                    for (int y = 1; y < 60; y++) chunk.setBlockId(x, y, z, Blocks.STONE.id);
                    for (int y = 60; y < 63; y++) chunk.setBlockId(x, y, z, Blocks.DIRT.id);
                    chunk.setBlockId(x, 63, z, Blocks.GRASS.id);
                }
            }
            chunk.recalcHeightMap();
            chunk.recalcLight();
            chunk.ready = true;
        }
    }
}
