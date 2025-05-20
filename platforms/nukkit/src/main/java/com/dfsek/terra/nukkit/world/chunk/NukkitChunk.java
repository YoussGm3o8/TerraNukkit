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

package com.dfsek.terra.nukkit.world.chunk;

import cn.nukkit.level.format.FullChunk;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.world.biome.BiomeType;
import com.dfsek.terra.api.world.chunk.Chunk;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.nukkit.NukkitPlatform;
import com.dfsek.terra.nukkit.world.NukkitWorld;
import com.dfsek.terra.nukkit.world.biome.NukkitBiome;

/**
 * Nukkit implementation of Terra chunk interface.
 */
public class NukkitChunk implements Chunk, Handle {
    private final FullChunk chunk;
    private final NukkitWorld world;
    private final ConfigPack configPack;
    private final NukkitPlatform platform;

    public NukkitChunk(FullChunk chunk, NukkitWorld world, ConfigPack configPack, NukkitPlatform platform) {
        this.chunk = chunk;
        this.world = world;
        this.configPack = configPack;
        this.platform = platform;
    }

    @Override
    public int getX() {
        return chunk.getX();
    }

    @Override
    public int getZ() {
        return chunk.getZ();
    }

    @Override
    public @NotNull ServerWorld getWorld() {
        return world;
    }

    @Override
    public @NotNull BlockState getBlock(int x, int y, int z) {
        int blockX = (getX() << 4) + x;
        int blockZ = (getZ() << 4) + z;
        return world.getBlockState(blockX, y, blockZ);
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull BlockState data) {
        setBlock(x, y, z, data, false);
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull BlockState data, boolean physics) {
        if (y < 0 || y >= world.getMaxHeight()) return;
        int blockX = (getX() << 4) + x;
        int blockZ = (getZ() << 4) + z;
        world.setBlockState(blockX, y, blockZ, data, physics);
    }

    /**
     * Get the biome at the specified coordinates.
     * 
     * @param x X coordinate relative to chunk
     * @param y Y coordinate (ignored for Nukkit)
     * @param z Z coordinate relative to chunk
     * @return The biome at the coordinates
     */
    public BiomeType getBiome(int x, int y, int z) {
        int biomeId = chunk.getBiomeId(x, z);
        // Convert Nukkit biome ID to Terra biome
        cn.nukkit.level.biome.Biome nukkitBiome = cn.nukkit.level.biome.Biome.getBiome(biomeId);
        
        // Create a Terra biome from the Nukkit biome
        // The registry key is created from the biome name, using minecraft namespace
        String biomeName = determineBiomeName(biomeId);
        RegistryKey key = RegistryKey.of("minecraft", biomeName);
        
        return new NukkitBiome(nukkitBiome, key);
    }

    /**
     * Set the biome at the specified coordinates.
     * 
     * @param x X coordinate relative to chunk
     * @param y Y coordinate (ignored for Nukkit)
     * @param z Z coordinate relative to chunk
     * @param biome The biome to set
     */
    public void setBiome(int x, int y, int z, @NotNull BiomeType biome) {
        if (y < 0 || y >= world.getMaxHeight()) return;
        
        // Extract biome ID from the biome
        int biomeId;
        if (biome instanceof NukkitBiome nukkitBiome) {
            biomeId = nukkitBiome.getBiomeId();
        } else {
            // Default to plains if the biome type is unknown
            biomeId = cn.nukkit.level.biome.EnumBiome.PLAINS.id;
        }
        
        chunk.setBiomeId(x, z, biomeId);
    }

    /**
     * Determine the biome name from the Nukkit biome ID.
     * This is a utility method to convert biome IDs to names for the registry key.
     *
     * @param biomeId The Nukkit biome ID
     * @return The biome name (lowercase, without minecraft: prefix)
     */
    private String determineBiomeName(int biomeId) {
        // Simple mapping from Nukkit biome IDs to names
        for (cn.nukkit.level.biome.EnumBiome enumBiome : cn.nukkit.level.biome.EnumBiome.values()) {
            if (enumBiome.id == biomeId) {
                return enumBiome.name().toLowerCase();
            }
        }
        return "plains"; // Default to plains if no match
    }

    @Override
    public Object getHandle() {
        return chunk;
    }
} 