package com.dfsek.terra.nukkit.inventory;

// Use fully qualified name for Nukkit Item to avoid collision
import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.inventory.Item; // Import Terra Item interface
import com.dfsek.terra.api.inventory.ItemStack; // Import Terra ItemStack interface
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.mapping.NukkitMappingRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapper for Nukkit Item (representing the type, not the stack).
 * Implements Terra's Item interface.
 */
public class NukkitItem implements Item, Handle {
    private final cn.nukkit.item.Item handle; // Nukkit Item instance
    private final RegistryKey registryKey;

    // Constructor needs the mapped key
    public NukkitItem(cn.nukkit.item.Item handle, RegistryKey registryKey) {
        this.handle = handle.clone();
        this.handle.setCount(1);
        this.registryKey = registryKey;
    }

    // Method from Handle interface
    @Override
    public cn.nukkit.item.Item getHandle() {
        return handle;
    }

    // This is NOT part of the Item interface, remove @Override
    public @NotNull RegistryKey getRegistryKey() {
        return registryKey;
    }

    @Override
    public ItemStack newItemStack(int amount) {
        cn.nukkit.item.Item newNukkitStack = this.handle.clone();
        newNukkitStack.setCount(amount);
        return new NukkitItemStack(newNukkitStack);
    }

    @Override
    public double getMaxDurability() {
        return this.handle.getMaxDurability();
    }

    // Need equals/hashCode based on the key for reliable use in maps/sets
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NukkitItem that = (NukkitItem) o;
        return registryKey.equals(that.registryKey);
    }

    @Override
    public int hashCode() {
        return registryKey.hashCode();
    }
} 