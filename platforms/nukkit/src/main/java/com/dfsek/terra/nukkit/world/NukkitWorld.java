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

package com.dfsek.terra.nukkit.world;

import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.util.vector.Vector2Int;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.Chunk;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.nukkit.NukkitPlatform;
import com.dfsek.terra.nukkit.block.NukkitBlockState;
import com.dfsek.terra.nukkit.world.chunk.NukkitChunk;
import com.dfsek.terra.nukkit.world.entity.NukkitEntity;

import java.util.UUID;

/**
 * Implementation of ServerWorld for Nukkit.
 */
public class NukkitWorld implements ServerWorld, Handle {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitWorld.class);
    
    private final Level level;
    private ChunkGenerator chunkGenerator;
    private final ConfigPack configPack;
    private final NukkitPlatform platform;

    public NukkitWorld(Level level, ChunkGenerator chunkGenerator, ConfigPack configPack, NukkitPlatform platform) {
        this.level = level;
        this.chunkGenerator = chunkGenerator;
        this.configPack = configPack;
        this.platform = platform;
    }

    @Override
    public @NotNull ChunkGenerator getGenerator() {
        return chunkGenerator;
    }

    @Override
    public @NotNull BiomeProvider getBiomeProvider() {
        return configPack.getBiomeProvider();
    }

    @Override
    public @NotNull ConfigPack getPack() {
        return configPack;
    }

    @Override
    public long getSeed() {
        return level.getSeed();
    }

    @Override
    public int getMaxHeight() {
        return 256; // Standard Minecraft height
    }

    @Override
    public int getMinHeight() {
        return 0;
    }

    @Override
    public Object getHandle() {
        return level;
    }

    @Override
    public Chunk getChunkAt(int x, int z) {
        return new NukkitChunk(level.getChunk(x, z), this, configPack, platform);
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        cn.nukkit.block.Block nukkitBlock = level.getBlock(x, y, z);
        return new NukkitBlockState(nukkitBlock, null); // We need to create a JE block state from Nukkit block
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        if (data instanceof NukkitBlockState nukkitData) {
            cn.nukkit.block.Block nukkitBlock = nukkitData.getNukkitBlock();
            level.setBlock(new Vector3(x, y, z), nukkitBlock, true, physics);
        } else {
            LOGGER.warn("Attempted to set non-Nukkit BlockState: {}", data.getClass().getName());
        }
    }

    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        cn.nukkit.blockentity.BlockEntity nukkitEntity = level.getBlockEntity(new Vector3(x, y, z));
        // TODO: Implement proper block entity handling
        return null;
    }

    @Override
    public com.dfsek.terra.api.entity.Entity spawnEntity(double x, double y, double z, EntityType entityType) {
        try {
            // Try to get entity type information from the handle
            Object handle = entityType.getHandle();
            
            // If we have a string handle, use it directly as the entity name
            String entityName;
            if (handle instanceof String) {
                entityName = (String) handle;
            } else {
                // Otherwise, fall back to a default entity type (zombie)
                LOGGER.warn("Could not determine entity type from handle: {}", handle);
                entityName = "Zombie";
            }
            
            Entity nukkitEntity = Entity.createEntity(entityName, 
                    level.getChunk((int)x >> 4, (int)z >> 4),
                    Entity.getDefaultNBT(new Vector3(x, y, z)));
            
            if (nukkitEntity != null) {
                nukkitEntity.spawnToAll();
                return new NukkitEntity(nukkitEntity);
            }
        } catch (Exception e) {
            LOGGER.error("Error spawning entity at {}, {}, {}: {}", x, y, z, e.getMessage());
        }
        return null;
    }

    /**
     * Set the chunk generator for this world
     * 
     * @param generator The chunk generator to use
     */
    public void setGenerator(ChunkGenerator generator) {
        this.chunkGenerator = generator;
    }
} 