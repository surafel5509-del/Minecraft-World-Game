package com.craftworld3d.player;

import com.craftworld3d.world.BlockType;

/** 36 slot inventory with a nine-slot hotbar and stack merging. */
public final class Inventory {
    public static final int SIZE=36, HOTBAR=9, MAX_STACK=64;
    private final ItemStack[] slots=new ItemStack[SIZE];
    public Inventory(){ for(int i=0;i<SIZE;i++)slots[i]=new ItemStack(0,0); }
    public ItemStack get(int i){return slots[i];}
    public int add(BlockType type,int amount){
        if(type==BlockType.AIR)return amount;
        for(ItemStack s:slots){if(s.id==type.id&&s.count<MAX_STACK){int n=Math.min(amount,MAX_STACK-s.count);s.count+=n;amount-=n;if(amount==0)return 0;}}
        for(ItemStack s:slots){if(s.empty()){int n=Math.min(amount,MAX_STACK);s.id=type.id;s.count=n;amount-=n;if(amount==0)return 0;}}
        return amount;
    }
    public boolean remove(int id,int amount){int have=0;for(ItemStack s:slots)if(s.id==id)have+=s.count;if(have<amount)return false;for(ItemStack s:slots)if(s.id==id){int n=Math.min(amount,s.count);s.count-=n;amount-=n;if(s.count==0)s.id=0;if(amount==0)break;}return true;}
    public void seed(){add(BlockType.DIRT,32);add(BlockType.WOOD,16);add(BlockType.STONE,32);add(BlockType.TORCH,8);}
}
