/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.nukkit.world.biome;

import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.world.biome.BiomeType;
import com.dfsek.terra.api.world.biome.PlatformBiome;
import org.jetbrains.annotations.NotNull;

import cn.nukkit.level.biome.Biome;
import cn.nukkit.level.biome.EnumBiome;

/**
 * Nukkit implementation of PlatformBiome.
 */
public class NukkitBiome implements PlatformBiome, BiomeType {
    private final Biome nukkitBiome;
    private final RegistryKey key;

    /**
     * Create a new NukkitBiome from a Nukkit Biome.
     *
     * @param nukkitBiome Nukkit Biome instance
     * @param key Registry key for this biome
     */
    public NukkitBiome(Biome nukkitBiome, RegistryKey key) {
        this.nukkitBiome = nukkitBiome;
        this.key = key;
    }

    /**
     * Get the Nukkit biome instance.
     *
     * @return Nukkit Biome
     */
    public Biome getNukkitBiome() {
        return nukkitBiome;
    }

    /**
     * Get the Nukkit biome ID.
     *
     * @return Biome ID
     */
    public int getBiomeId() {
        // Find the biome ID from the Nukkit biome registry
        for (EnumBiome enumBiome : EnumBiome.values()) {
            if (enumBiome.biome == nukkitBiome) {
                return enumBiome.id;
            }
        }
        return EnumBiome.PLAINS.id; // Default to plains if not found
    }

    @Override
    public Object getHandle() {
        return nukkitBiome;
    }
    
    @Override
    public @NotNull RegistryKey getRegistryKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof NukkitBiome that)) return false;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "NukkitBiome{key=" + key + "}";
    }
} 