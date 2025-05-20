package com.dfsek.terra.nukkit.inventory;

import cn.nukkit.item.Item;

import com.dfsek.terra.api.inventory.ItemStack;

/**
 * Nukkit implementation of Terra's Item interface.
 */
public class NukkitItemType implements com.dfsek.terra.api.inventory.Item {
    private final Item nukkitItem;
    
    public NukkitItemType(Item nukkitItem) {
        this.nukkitItem = nukkitItem;
    }
    
    @Override
    public ItemStack newItemStack(int amount) {
        Item newItem = nukkitItem.clone();
        newItem.setCount(amount);
        return new NukkitItemStack(newItem);
    }
    
    @Override
    public double getMaxDurability() {
        return nukkitItem.getMaxDurability();
    }
    
    @Override
    public Object getHandle() {
        return nukkitItem;
    }
} 