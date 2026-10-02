package com.craftworld3d.core.item;

import com.craftworld3d.core.block.Block;
import com.craftworld3d.core.block.ToolType;

/** A damageable tool with a class (pickaxe/axe/...) and a material tier. */
public final class ToolItem extends Item {
    public final ToolType toolType;
    public final ToolMaterial material;
    /** Melee damage dealt with this tool. */
    public final int attackDamage;

    public ToolItem(int id, String name, String icon, ToolType toolType, ToolMaterial material) {
        super(id, name, icon, 1, -1);
        this.toolType = toolType;
        this.material = material;
        int base = switch (toolType) {
            case SWORD -> 4;
            case AXE -> 3;
            case PICKAXE -> 2;
            default -> 1;
        };
        this.attackDamage = base + material.damageBonus;
    }

    @Override
    public int maxDurability() { return material.durability; }

    /** Mining speed multiplier against the given block. */
    public float speedAgainst(Block block) {
        return block.tool == toolType ? material.speed : 1.0f;
    }

    /** Whether this tool is good enough for the block to drop loot. */
    public boolean canHarvest(Block block) {
        if (!block.toolRequired) return true;
        return block.tool == toolType && material.miningLevel >= block.toolTier;
    }
}
