package com.craftworld3d.core.inventory;

import com.craftworld3d.core.item.Item;
import com.craftworld3d.core.item.ItemRegistry;
import com.craftworld3d.core.item.ItemStack;

/**
 * Player inventory: 36 slots. Slots 0..8 are the hotbar.
 * Supports merging, splitting and moving stacks.
 */
public class Inventory {
    public static final int SIZE = 36;
    public static final int HOTBAR_SIZE = 9;

    protected final ItemStack[] slots;
    public int selectedSlot = 0;

    public Inventory() {
        this(SIZE);
    }

    public Inventory(int size) {
        slots = new ItemStack[size];
    }

    public int size() { return slots.length; }

    public ItemStack get(int slot) {
        ItemStack s = slots[slot];
        return (s == null || s.isEmpty()) ? null : s;
    }

    public void set(int slot, ItemStack stack) {
        slots[slot] = (stack == null || stack.isEmpty()) ? null : stack;
    }

    public ItemStack selected() { return get(selectedSlot); }

    /**
     * Adds a stack, merging into existing stacks first (hotbar preferred),
     * then filling empty slots. Returns the number of items that did NOT fit.
     */
    public int add(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int remaining = stack.count;
        Item item = ItemRegistry.get(stack.itemId);
        int max = item == null ? 64 : item.maxStack;

        // Pass 1: merge into existing stacks.
        if (item == null || item.maxDurability() == 0) {
            for (int i = 0; i < slots.length && remaining > 0; i++) {
                ItemStack s = get(i);
                if (s != null && s.itemId == stack.itemId && s.count < max) {
                    int move = Math.min(max - s.count, remaining);
                    s.count += move;
                    remaining -= move;
                }
            }
        }
        // Pass 2: empty slots.
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (get(i) == null) {
                int move = Math.min(max, remaining);
                set(i, new ItemStack(stack.itemId, move, stack.damage));
                remaining -= move;
            }
        }
        return remaining;
    }

    /** Counts total items of the given id. */
    public int count(int itemId) {
        int n = 0;
        for (ItemStack s : slots) {
            if (s != null && !s.isEmpty() && s.itemId == itemId) n += s.count;
        }
        return n;
    }

    /** Removes up to n items of the given id; returns how many were actually removed. */
    public int remove(int itemId, int n) {
        int removed = 0;
        for (int i = 0; i < slots.length && removed < n; i++) {
            ItemStack s = get(i);
            if (s != null && s.itemId == itemId) {
                int take = Math.min(s.count, n - removed);
                s.count -= take;
                removed += take;
                if (s.count <= 0) set(i, null);
            }
        }
        return removed;
    }

    /** Consumes 1 item from the given slot. */
    public void shrink(int slot) {
        ItemStack s = get(slot);
        if (s != null) {
            s.count--;
            if (s.count <= 0) set(slot, null);
        }
    }

    /**
     * Classic click-to-move: swaps/merges the cursor stack with the slot.
     * Returns the new cursor stack.
     */
    public ItemStack clickSlot(int slot, ItemStack cursor) {
        ItemStack inSlot = get(slot);
        if (cursor == null || cursor.isEmpty()) {
            set(slot, null);
            return inSlot; // pick up
        }
        if (inSlot == null) {
            set(slot, cursor);
            return null; // place all
        }
        if (inSlot.canStackWith(cursor)) {
            int max = inSlot.maxStack();
            int move = Math.min(cursor.count, max - inSlot.count);
            inSlot.count += move;
            cursor.count -= move;
            return cursor.isEmpty() ? null : cursor;
        }
        set(slot, cursor);
        return inSlot; // swap
    }

    /** Splits half of the slot's stack into the returned cursor stack (long-press). */
    public ItemStack splitSlot(int slot) {
        ItemStack s = get(slot);
        if (s == null) return null;
        int half = (s.count + 1) / 2;
        ItemStack out = s.split(half);
        if (s.count <= 0) set(slot, null);
        return out;
    }

    public void clear() {
        for (int i = 0; i < slots.length; i++) slots[i] = null;
    }

    public boolean isEmpty() {
        for (ItemStack s : slots) if (s != null && !s.isEmpty()) return false;
        return true;
    }
}
