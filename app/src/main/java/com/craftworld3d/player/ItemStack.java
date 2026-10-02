package com.craftworld3d.player;

/** Small, serializable inventory value object. */
public final class ItemStack {
    public int id, count;
    public ItemStack(int id, int count) { this.id=id; this.count=count; }
    public boolean empty(){ return id==0 || count<=0; }
}
