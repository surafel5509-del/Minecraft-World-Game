package com.craftworld3d.core.redstone;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Basic redstone-like power simulation.
 *
 * Sources: lever (on), redstone torch, redstone block, pressed button,
 * occupied pressure plate. Power propagates through wire with 15-step decay
 * (repeaters refresh it to 15). Consumers: lamps, doors, pistons, powered rails.
 *
 * Documented simplifications: wires connect in all 6 directions, repeaters are
 * omnidirectional with no delay, pistons push the block above them.
 */
public final class RedstoneEngine {
    private static final int MAX_NETWORK = 4096;

    /** Wire power levels (posKey -> 0..15). */
    private final Map<Long, Integer> wirePower = new HashMap<>();
    /** Buttons currently pressed: posKey -> ticks remaining. */
    private final Map<Long, Integer> pressedButtons = new HashMap<>();
    /** Plates currently stood on: posKey -> ticks remaining. */
    private final Map<Long, Integer> pressedPlates = new HashMap<>();

    private boolean updating;

    public void onBlockChanged(World world, int x, int y, int z) {
        if (updating) return;
        recompute(world, x, y, z);
    }

    /** Called by the UI when the player taps a lever or button. Returns true if handled. */
    public boolean interact(World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        if (id == Blocks.LEVER.id) {
            world.setBlock(x, y, z, Blocks.LEVER_ON.id);
            return true;
        }
        if (id == Blocks.LEVER_ON.id) {
            world.setBlock(x, y, z, Blocks.LEVER.id);
            return true;
        }
        if (id == Blocks.BUTTON.id) {
            pressedButtons.put(World.posKey(x, y, z), 20);
            recompute(world, x, y, z);
            return true;
        }
        return false;
    }

    /** Called when an entity stands on a block; presses a plate if there is one. */
    public void standOnPlate(World world, int x, int y, int z) {
        if (world.getBlockId(x, y, z) == Blocks.PRESSURE_PLATE.id) {
            Long key = World.posKey(x, y, z);
            boolean wasPressed = pressedPlates.containsKey(key);
            pressedPlates.put(key, 10);
            if (!wasPressed) recompute(world, x, y, z);
        }
    }

    /** Ticks button/plate timers. Call once per world tick. */
    public void tick(World world) {
        tickTimers(world, pressedButtons);
        tickTimers(world, pressedPlates);
    }

    private void tickTimers(World world, Map<Long, Integer> timers) {
        if (timers.isEmpty()) return;
        var it = timers.entrySet().iterator();
        var expired = new ArrayDeque<Long>();
        while (it.hasNext()) {
            var e = it.next();
            int t = e.getValue() - 1;
            if (t <= 0) {
                expired.add(e.getKey());
                it.remove();
            } else {
                e.setValue(t);
            }
        }
        for (Long key : expired) {
            int x = (int) (key >> 38);
            int y = (int) (key & 0xFFF);
            int z = (int) ((key << 26) >> 38);
            recompute(world, x, y, z);
        }
    }

    public int wirePowerAt(int x, int y, int z) {
        Integer p = wirePower.get(World.posKey(x, y, z));
        return p == null ? 0 : p;
    }

    public boolean isPowered(World world, int x, int y, int z) {
        for (int d = 0; d < 6; d++) {
            int nx = x + DX[d], ny = y + DY[d], nz = z + DZ[d];
            if (isSource(world, nx, ny, nz)) return true;
            if (world.getBlockId(nx, ny, nz) == Blocks.REDSTONE_WIRE.id && wirePowerAt(nx, ny, nz) > 0) return true;
        }
        return false;
    }

    private boolean isSource(World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        if (id == Blocks.LEVER_ON.id || id == Blocks.REDSTONE_TORCH.id || id == Blocks.REDSTONE_BLOCK.id)
            return true;
        long key = World.posKey(x, y, z);
        if (id == Blocks.BUTTON.id && pressedButtons.containsKey(key)) return true;
        if (id == Blocks.PRESSURE_PLATE.id && pressedPlates.containsKey(key)) return true;
        return false;
    }

    private static final int[] DX = {1, -1, 0, 0, 0, 0};
    private static final int[] DY = {0, 0, 1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 0, 0, 1, -1};

    /**
     * Recomputes the wire network reachable from the changed position and
     * updates consumers on its border.
     */
    private void recompute(World world, int ox, int oy, int oz) {
        updating = true;
        try {
            // Collect the connected component of wires/repeaters around the change.
            Set<Long> network = new HashSet<>();
            Set<Long> border = new HashSet<>();
            ArrayDeque<long[]> queue = new ArrayDeque<>();
            for (int d = -1; d < 6; d++) {
                int x = d < 0 ? ox : ox + DX[d];
                int y = d < 0 ? oy : oy + DY[d];
                int z = d < 0 ? oz : oz + DZ[d];
                queue.add(new long[]{x, y, z});
            }
            while (!queue.isEmpty() && network.size() < MAX_NETWORK) {
                long[] p = queue.poll();
                int x = (int) p[0], y = (int) p[1], z = (int) p[2];
                long key = World.posKey(x, y, z);
                if (network.contains(key) || border.contains(key)) continue;
                int id = world.getBlockId(x, y, z);
                if (id == Blocks.REDSTONE_WIRE.id || id == Blocks.REPEATER.id) {
                    network.add(key);
                    for (int d = 0; d < 6; d++) {
                        queue.add(new long[]{x + DX[d], y + DY[d], z + DZ[d]});
                    }
                } else {
                    border.add(key);
                }
            }

            // Seed power levels from sources adjacent to each network cell.
            Map<Long, Integer> levels = new HashMap<>();
            ArrayDeque<Long> bfs = new ArrayDeque<>();
            for (Long key : network) {
                int x = (int) (key >> 38), y = (int) (key & 0xFFF), z = (int) ((key << 26) >> 38);
                int best = 0;
                for (int d = 0; d < 6; d++) {
                    if (isSource(world, x + DX[d], y + DY[d], z + DZ[d])) { best = 15; break; }
                }
                // Repeaters refresh to 15 when any neighbouring wire is powered (resolved below).
                if (best > 0) {
                    levels.put(key, best);
                    bfs.add(key);
                }
            }
            // Propagate with decay; repeaters reset to 15.
            while (!bfs.isEmpty()) {
                Long key = bfs.poll();
                int level = levels.getOrDefault(key, 0);
                if (level <= 1) continue;
                int x = (int) (key >> 38), y = (int) (key & 0xFFF), z = (int) ((key << 26) >> 38);
                for (int d = 0; d < 6; d++) {
                    int nx = x + DX[d], ny = y + DY[d], nz = z + DZ[d];
                    long nKey = World.posKey(nx, ny, nz);
                    if (!network.contains(nKey)) continue;
                    int nId = world.getBlockId(nx, ny, nz);
                    int newLevel = nId == Blocks.REPEATER.id ? 15 : level - 1;
                    if (levels.getOrDefault(nKey, 0) < newLevel) {
                        levels.put(nKey, newLevel);
                        bfs.add(nKey);
                    }
                }
            }

            // Store wire power for the network (clear stale entries first).
            for (Long key : network) {
                Integer lv = levels.get(key);
                if (lv == null || lv == 0) wirePower.remove(key);
                else wirePower.put(key, lv);
            }

            // Update consumers adjacent to the network + around the original change.
            Set<Long> consumers = new HashSet<>(border);
            consumers.add(World.posKey(ox, oy, oz));
            for (int d = 0; d < 6; d++) {
                consumers.add(World.posKey(ox + DX[d], oy + DY[d], oz + DZ[d]));
            }
            for (Long key : consumers) {
                int x = (int) (key >> 38), y = (int) (key & 0xFFF), z = (int) ((key << 26) >> 38);
                updateConsumer(world, x, y, z);
            }
        } finally {
            updating = false;
        }
    }

    private void updateConsumer(World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        boolean powered = isPowered(world, x, y, z);
        if (id == Blocks.REDSTONE_LAMP.id && powered) {
            world.setBlockKeepEntity(x, y, z, Blocks.REDSTONE_LAMP_LIT.id);
        } else if (id == Blocks.REDSTONE_LAMP_LIT.id && !powered) {
            world.setBlockKeepEntity(x, y, z, Blocks.REDSTONE_LAMP.id);
        } else if (id == Blocks.DOOR_CLOSED.id && powered) {
            world.setBlockKeepEntity(x, y, z, Blocks.DOOR_OPEN.id);
        } else if (id == Blocks.DOOR_OPEN.id && !powered) {
            world.setBlockKeepEntity(x, y, z, Blocks.DOOR_CLOSED.id);
        } else if (id == Blocks.PISTON.id && powered) {
            // Simplified piston: push the block above up by one if there's room.
            int above = world.getBlockId(x, y + 1, z);
            if (above != 0 && world.getBlockId(x, y + 2, z) == 0
                && !world.getBlock(x, y + 1, z).isUnbreakable()) {
                world.setBlockKeepEntity(x, y + 2, z, above);
                world.setBlockKeepEntity(x, y + 1, z, 0);
            }
        }
    }
}
