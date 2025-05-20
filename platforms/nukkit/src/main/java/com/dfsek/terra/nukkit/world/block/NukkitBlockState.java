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

import cn.nukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.block.BlockType;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.block.state.properties.Property;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.nukkit.mapping.NukkitBlockMapping;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of Terra's BlockState for Nukkit blocks.
 */
public class NukkitBlockState implements BlockState, Handle {
    private final Block nukkitBlock;
    private final int damage;
    private final NukkitBlockMapping mapping;
    private final RegistryKey registryKey;
    private final NukkitBlockType blockType;

    public NukkitBlockState(Block nukkitBlock) {
        this.nukkitBlock = nukkitBlock;
        this.damage = nukkitBlock.getDamage();
        
        this.registryKey = RegistryKey.of("minecraft", nukkitBlock.getName().toLowerCase().replace(" ", "_"));
        this.mapping = new NukkitBlockMapping(nukkitBlock.getId(), damage);

        // Create the block type (uses this state as default state)
        this.blockType = new NukkitBlockType(registryKey, this);
    }

    public NukkitBlockState(Block nukkitBlock, NukkitBlockMapping mapping, int damage) {
        this.nukkitBlock = nukkitBlock;
        this.damage = damage;
        this.mapping = mapping;
        
        this.registryKey = RegistryKey.of("minecraft", nukkitBlock.getName().toLowerCase().replace(" ", "_"));
        this.blockType = new NukkitBlockType(registryKey, this);
    }

    @Override
    public boolean matches(BlockState other) {
        return getBlockType().equals(other.getBlockType());
    }

    @Override
    public <T extends Comparable<T>> boolean has(Property<T> property) {
        // Nukkit doesn't use property objects like Terra expects
        return false;
    }

    @Override
    public <T extends Comparable<T>> T get(Property<T> property) {
        throw new UnsupportedOperationException("Nukkit blocks don't support property objects");
    }

    @Override
    public <T extends Comparable<T>> BlockState set(Property<T> property, T value) {
        // This operation isn't supported with Nukkit blocks
        return this;
    }

    @Override
    public BlockType getBlockType() {
        return blockType;
    }

    @Override
    public String getAsString(boolean properties) {
        String base = registryKey.toString();
        if (properties && damage > 0) {
            return base + "[damage=" + damage + "]";
        }
        return base;
    }

    @Override
    public boolean isAir() {
        return nukkitBlock.getId() == 0;
    }

    /**
     * Get the Nukkit block for this block state.
     *
     * @return Nukkit block
     */
    public Block getNukkitBlock() {
        return nukkitBlock;
    }

    /**
     * Get the Nukkit block mapping for this block state.
     *
     * @return Nukkit block mapping
     */
    public NukkitBlockMapping getNukkitMapping() {
        return mapping;
    }

    /**
     * Get the damage value for this block state.
     *
     * @return Damage value
     */
    public int getNukkitDamage() {
        return damage;
    }

    /**
     * Get the registry key for this block state.
     *
     * @return Registry key
     */
    public RegistryKey getRegistryKey() {
        return registryKey;
    }

    @Override
    public Object getHandle() {
        return nukkitBlock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NukkitBlockState that = (NukkitBlockState) o;
        return damage == that.damage && Objects.equals(nukkitBlock.getId(), that.nukkitBlock.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(nukkitBlock.getId(), damage);
    }
} 