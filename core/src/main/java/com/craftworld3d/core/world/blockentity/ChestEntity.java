package com.craftworld3d.core.world.blockentity;

import com.craftworld3d.core.inventory.Inventory;
import com.craftworld3d.core.item.ItemStack;
import com.craftworld3d.core.util.MiniJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 27-slot container. */
public final class ChestEntity extends BlockEntity {
    public static final int SIZE = 27;
    public final Inventory inventory = new Inventory(SIZE);

    public ChestEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String type() { return "chest"; }

    @Override
    public Map<String, Object> saveData() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", saveInventory(inventory));
        return m;
    }

    @Override
    public void loadData(Map<String, Object> data) {
        loadInventory(inventory, data.get("items"));
    }

    public static List<Object> saveInventory(Inventory inv) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.get(i);
            if (s == null) continue;
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("slot", i);
            e.put("id", s.itemId);
            e.put("n", s.count);
            if (s.damage > 0) e.put("d", s.damage);
            list.add(e);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public static void loadInventory(Inventory inv, Object data) {
        inv.clear();
        if (!(data instanceof List)) return;
        for (Object o : (List<Object>) data) {
            if (!(o instanceof Map)) continue;
            Map<String, Object> e = (Map<String, Object>) o;
            int slot = MiniJson.getInt(e, "slot", -1);
            int id = MiniJson.getInt(e, "id", 0);
            int n = MiniJson.getInt(e, "n", 0);
            int d = MiniJson.getInt(e, "d", 0);
            if (slot >= 0 && slot < inv.size() && id > 0 && n > 0) {
                inv.set(slot, new ItemStack(id, n, d));
            }
        }
    }
}
