package com.dfsek.terra.nukkit.handles;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.enchantment.Enchantment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.dfsek.terra.api.handle.ItemHandle;
import com.dfsek.terra.nukkit.inventory.NukkitEnchantment;
import com.dfsek.terra.nukkit.inventory.NukkitItemType;

/**
 * Nukkit implementation of Terra's ItemHandle.
 */
public class NukkitItemHandle implements ItemHandle {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitItemHandle.class);

    @Override
    public com.dfsek.terra.api.inventory.Item createItem(String data) {
        String itemId = data;
        if (data.startsWith("minecraft:")) {
            itemId = data.substring(10); // Remove "minecraft:" prefix
        }
        
        // Convert itemId to Nukkit's item system
        // This is a simplified version - a full implementation would need more mappings
        try {
            Item nukkitItem = Item.fromString(itemId);
            return new NukkitItemType(nukkitItem);
        } catch (Exception e) {
            LOGGER.warn("Failed to create item from data: {}", data);
            // Default to apple if we can't parse the item
            return new NukkitItemType(Item.get(ItemID.APPLE));
        }
    }

    @Override
    public com.dfsek.terra.api.inventory.item.Enchantment getEnchantment(String id) {
        String enchantId = id;
        if (id.startsWith("minecraft:")) {
            enchantId = id.substring(10); // Remove "minecraft:" prefix
        }
        
        // Map the enchantment ID to a Nukkit enchantment
        // This is a simplified approach
        try {
            // Try to map the enchantment ID to a Nukkit enchantment
            for (Enchantment enchantment : Enchantment.getEnchantments()) {
                if (enchantment.getName().equalsIgnoreCase(enchantId)) {
                    return new NukkitEnchantment(enchantment);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get enchantment: {}", id, e);
        }
        
        // Return a default enchantment if we can't find the one requested
        return new NukkitEnchantment(Enchantment.getEnchantment(Enchantment.ID_DURABILITY));
    }

    @Override
    public Set<com.dfsek.terra.api.inventory.item.Enchantment> getEnchantments() {
        // Return all available Nukkit enchantments
        return Arrays.stream(Enchantment.getEnchantments())
                .map(NukkitEnchantment::new)
                .collect(Collectors.toSet());
    }
} 