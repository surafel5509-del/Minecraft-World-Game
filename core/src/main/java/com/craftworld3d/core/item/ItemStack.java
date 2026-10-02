package com.craftworld3d.core.item;

/** A stack of items with count and (for tools) accumulated damage. */
public final class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(0, 0);

    public final int itemId;
    public int count;
    /** Damage taken; stack breaks when damage >= maxDurability. */
    public int damage;

    public ItemStack(int itemId, int count) {
        this(itemId, count, 0);
    }

    public ItemStack(int itemId, int count, int damage) {
        this.itemId = itemId;
        this.count = count;
        this.damage = damage;
    }

    public boolean isEmpty() {
        return itemId <= 0 || count <= 0;
    }

    public Item item() {
        return ItemRegistry.get(itemId);
    }

    public int maxStack() {
        Item i = item();
        return i == null ? 64 : i.maxStack;
    }

    public boolean canStackWith(ItemStack other) {
        if (other == null || other.isEmpty() || isEmpty()) return false;
        Item i = item();
        if (i != null && i.maxDurability() > 0) return false; // tools never stack
        return other.itemId == itemId;
    }

    public ItemStack copy() {
        return new ItemStack(itemId, count, damage);
    }

    /** Splits off up to n items into a new stack. */
    public ItemStack split(int n) {
        int take = Math.min(n, count);
        count -= take;
        return new ItemStack(itemId, take, damage);
    }

    /** Applies 1 durability damage. Returns true if the item broke. */
    public boolean damageItem() {
        Item i = item();
        if (i == null || i.maxDurability() <= 0) return false;
        damage++;
        if (damage >= i.maxDurability()) {
            count = 0;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return isEmpty() ? "empty" : (itemId + "x" + count + (damage > 0 ? "(d" + damage + ")" : ""));
    }
}
