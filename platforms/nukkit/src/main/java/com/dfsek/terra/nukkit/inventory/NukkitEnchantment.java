package com.dfsek.terra.nukkit.inventory;

import cn.nukkit.item.enchantment.Enchantment;

import com.dfsek.terra.api.inventory.ItemStack;

/**
 * Nukkit implementation of Terra's Enchantment interface.
 */
public class NukkitEnchantment implements com.dfsek.terra.api.inventory.item.Enchantment {
    private final Enchantment nukkitEnchantment;
    
    public NukkitEnchantment(Enchantment nukkitEnchantment) {
        this.nukkitEnchantment = nukkitEnchantment;
    }
    
    @Override
    public boolean canEnchantItem(ItemStack itemStack) {
        if (itemStack instanceof NukkitItemStack nukkitStack) {
            return nukkitEnchantment.canEnchant(nukkitStack.getNukkitItem());
        }
        return false;
    }
    
    @Override
    public boolean conflictsWith(com.dfsek.terra.api.inventory.item.Enchantment other) {
        if (other instanceof NukkitEnchantment nukkitOther) {
            return !nukkitEnchantment.isCompatibleWith(nukkitOther.nukkitEnchantment);
        }
        return false;
    }
    
    @Override
    public String getID() {
        return nukkitEnchantment.getName();
    }
    
    @Override
    public int getMaxLevel() {
        return nukkitEnchantment.getMaxLevel();
    }
    
    @Override
    public Object getHandle() {
        return nukkitEnchantment;
    }
} 