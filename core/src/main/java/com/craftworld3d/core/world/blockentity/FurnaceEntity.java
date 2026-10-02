package com.craftworld3d.core.world.blockentity;

import com.craftworld3d.core.block.Blocks;
import com.craftworld3d.core.crafting.RecipeRegistry;
import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.util.MiniJson;
import com.craftworld3d.core.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

/** Furnace: input + fuel -> output. 200 ticks per smelt. */
public final class FurnaceEntity extends BlockEntity {
    public static final int SMELT_TICKS = 200;

    public ItemStack input, fuel, output;
    public int burnTicksLeft;      // remaining burn time of current fuel
    public int burnTicksTotal;     // for UI progress
    public int cookTicks;          // progress on current item

    public FurnaceEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String type() { return "furnace"; }

    public boolean isBurning() { return burnTicksLeft > 0; }

    @Override
    public void tick(World world) {
        boolean wasBurning = isBurning();
        if (burnTicksLeft > 0) burnTicksLeft--;

        ItemStack result = (input == null || input.isEmpty())
            ? null : RecipeRegistry.smeltResult(input.itemId);
        boolean canSmelt = result != null && canAccept(result);

        if (canSmelt && burnTicksLeft <= 0 && fuel != null && !fuel.isEmpty()) {
            int ticks = RecipeRegistry.fuelTicks(fuel.itemId);
            if (ticks > 0) {
                burnTicksLeft = burnTicksTotal = ticks;
                fuel.count--;
                if (fuel.count <= 0) fuel = null;
            }
        }

        if (canSmelt && burnTicksLeft > 0) {
            cookTicks++;
            if (cookTicks >= SMELT_TICKS) {
                cookTicks = 0;
                input.count--;
                if (input.count <= 0) input = null;
                if (output == null || output.isEmpty()) output = result.copy();
                else output.count += result.count;
            }
        } else {
            cookTicks = 0;
        }

        boolean burning = isBurning();
        if (burning != wasBurning) {
            int current = world.getBlockId(x, y, z);
            if (current == Blocks.FURNACE.id || current == Blocks.FURNACE_LIT.id) {
                world.setBlockKeepEntity(x, y, z,
                    burning ? Blocks.FURNACE_LIT.id : Blocks.FURNACE.id);
            }
        }
    }

    private boolean canAccept(ItemStack result) {
        if (output == null || output.isEmpty()) return true;
        return output.itemId == result.itemId && output.count + result.count <= output.maxStack();
    }

    @Override
    public Map<String, Object> saveData() {
        Map<String, Object> m = new LinkedHashMap<>();
        saveStack(m, "in", input);
        saveStack(m, "fuel", fuel);
        saveStack(m, "out", output);
        m.put("burn", burnTicksLeft);
        m.put("burnTotal", burnTicksTotal);
        m.put("cook", cookTicks);
        return m;
    }

    @Override
    public void loadData(Map<String, Object> data) {
        input = loadStack(data, "in");
        fuel = loadStack(data, "fuel");
        output = loadStack(data, "out");
        burnTicksLeft = MiniJson.getInt(data, "burn", 0);
        burnTicksTotal = MiniJson.getInt(data, "burnTotal", 0);
        cookTicks = MiniJson.getInt(data, "cook", 0);
    }

    private static void saveStack(Map<String, Object> m, String key, ItemStack s) {
        if (s == null || s.isEmpty()) return;
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("id", s.itemId);
        e.put("n", s.count);
        if (s.damage > 0) e.put("d", s.damage);
        m.put(key, e);
    }

    @SuppressWarnings("unchecked")
    private static ItemStack loadStack(Map<String, Object> m, String key) {
        Object o = m.get(key);
        if (!(o instanceof Map)) return null;
        Map<String, Object> e = (Map<String, Object>) o;
        int id = MiniJson.getInt(e, "id", 0);
        int n = MiniJson.getInt(e, "n", 0);
        if (id <= 0 || n <= 0) return null;
        return new ItemStack(id, n, MiniJson.getInt(e, "d", 0));
    }
}
