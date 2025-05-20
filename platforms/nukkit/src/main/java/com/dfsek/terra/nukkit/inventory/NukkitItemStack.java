package com.dfsek.terra.nukkit.inventory;

import cn.nukkit.item.Item;

import com.dfsek.terra.api.inventory.ItemStack;
import com.dfsek.terra.api.inventory.item.ItemMeta;

/**
 * Nukkit implementation of Terra's ItemStack interface.
 */
public class NukkitItemStack implements ItemStack {
    private final Item nukkitItem;
    
    public NukkitItemStack(Item nukkitItem) {
        this.nukkitItem = nukkitItem != null ? nukkitItem : Item.get(Item.AIR);
    }
    
    @Override
    public int getAmount() {
        return nukkitItem.getCount();
    }
    
    @Override
    public void setAmount(int amount) {
        nukkitItem.setCount(amount);
    }
    
    @Override
    public com.dfsek.terra.api.inventory.Item getType() {
        return new NukkitItemType(nukkitItem);
    }
    
    @Override
    public ItemMeta getItemMeta() {
        // Nukkit doesn't have a direct ItemMeta equivalent
        // We would need to implement this if needed
        return null;
    }
    
    @Override
    public void setItemMeta(ItemMeta meta) {
        // Nukkit doesn't have a direct ItemMeta equivalent
        // We would implement this if needed
    }
    
    @Override
    public Object getHandle() {
        return nukkitItem;
    }
    
    /**
     * Get the underlying Nukkit Item.
     */
    public Item getNukkitItem() {
        return nukkitItem;
    }
} 