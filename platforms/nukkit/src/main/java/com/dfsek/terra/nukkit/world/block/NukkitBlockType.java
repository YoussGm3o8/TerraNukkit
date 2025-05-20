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

package com.dfsek.terra.nukkit.world.block;

import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.block.BlockType;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.registry.key.RegistryKey;

/**
 * Implementation of BlockType for Nukkit blocks.
 */
public class NukkitBlockType implements BlockType, Handle {
    private final RegistryKey registryKey;
    private final BlockState defaultState;

    public NukkitBlockType(RegistryKey registryKey, BlockState defaultState) {
        this.registryKey = registryKey;
        this.defaultState = defaultState;
    }

    @Override
    public @NotNull BlockState getDefaultState() {
        return defaultState;
    }

    @Override
    public boolean isSolid() {
        // Check if the block is solid based on its Nukkit properties
        if (defaultState instanceof NukkitBlockState nukkitState) {
            return nukkitState.getNukkitBlock().isSolid();
        }
        return true; // Default to solid if we can't check
    }

    @Override
    public boolean isWater() {
        // Check if the block is water based on its Nukkit ID
        if (defaultState instanceof NukkitBlockState nukkitState) {
            int id = nukkitState.getNukkitBlock().getId();
            return id == 8 || id == 9; // Water and still water IDs in Nukkit
        }
        return false;
    }

    @Override
    public Object getHandle() {
        if (defaultState instanceof NukkitBlockState nukkitState) {
            return nukkitState.getNukkitBlock();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NukkitBlockType that)) return false;
        return registryKey.equals(that.registryKey);
    }

    @Override
    public int hashCode() {
        return registryKey.hashCode();
    }

    /**
     * Get the registry key for this block type.
     *
     * @return Registry key
     */
    public RegistryKey getRegistryKey() {
        return registryKey;
    }
} 