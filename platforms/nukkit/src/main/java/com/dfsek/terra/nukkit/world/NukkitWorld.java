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

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.biome.EnumBiome;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.Chunk;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.nukkit.NukkitPlatform;
import com.dfsek.terra.nukkit.block.NukkitBlockState;
import com.dfsek.terra.nukkit.block.NukkitJeBlockState;
import com.dfsek.terra.nukkit.block.NukkitMapping;
import com.dfsek.terra.nukkit.world.chunk.NukkitChunk;
import com.dfsek.terra.nukkit.world.entity.NukkitEntity;
import com.dfsek.terra.nukkit.world.biome.NukkitPlatformBiome;

/**
 * Nukkit implementation of Terra's ServerWorld interface.
 */
public class NukkitWorld implements ServerWorld, Handle {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitWorld.class);

    private final Level level;
    private ChunkGenerator chunkGenerator;
    private final ConfigPack configPack;
    private final NukkitPlatform platform;
    private final String worldName;
    
    // Cache to avoid creating new chunk objects repeatedly
    private final Map<Long, NukkitChunk> chunkCache = new ConcurrentHashMap<>();
    
    // Cache for biome objects to reduce object creation during generation
    private static final Map<Integer, NukkitPlatformBiome> BIOME_CACHE = new ConcurrentHashMap<>();
    
    // Cache frequently used biomes
    static {
        initBiomeCache();
    }
    
    // Create a single air blockstate to reduce object creation
    private static final BlockState AIR_BLOCKSTATE = new NukkitBlockState(Block.get(Block.AIR), 
                                                    NukkitJeBlockState.fromString("minecraft:air"));

    // Store customized height limits
    private int minHeight = -64; // Default to -64
    private int maxHeight = 320; // Default to 320

    public NukkitWorld(Level level, ChunkGenerator chunkGenerator, ConfigPack configPack, NukkitPlatform platform) {
        this.level = level;
        this.chunkGenerator = chunkGenerator;
        this.configPack = configPack;
        this.platform = platform;
        this.worldName = level != null ? level.getName() : "unknown";
    }
    
    /**
     * Initialize the biome cache with common biomes to reduce object creation
     */
    private static void initBiomeCache() {
        try {
            // Cache common biomes used during generation
            for (EnumBiome biome : EnumBiome.values()) {
                BIOME_CACHE.put(biome.id, new NukkitPlatformBiome(biome));
            }
        } catch (Exception e) {
            LOGGER.error("Error initializing biome cache", e);
        }
    }
    
    /**
     * Get a cached biome instance by ID
     *
     * @param biomeId Biome ID
     * @return Cached biome instance
     */
    public static NukkitPlatformBiome getCachedBiome(int biomeId) {
        // Check the cache first
        NukkitPlatformBiome cachedBiome = BIOME_CACHE.get(biomeId);
        if (cachedBiome != null) {
            return cachedBiome;
        }
        
        // If not found, create new instance and cache it
        EnumBiome nukkitBiome;
        try {
            // Try to get the biome by ID
            cn.nukkit.level.biome.Biome biome = cn.nukkit.level.biome.Biome.getBiome(biomeId);
            
            // Find matching enum value
            for (EnumBiome enumValue : EnumBiome.values()) {
                if (enumValue.id == biomeId) {
                    nukkitBiome = enumValue;
                    break;
                }
            }
            // Default to PLAINS if no match found
            nukkitBiome = EnumBiome.PLAINS;
        } catch (Exception e) {
            // Default to plains if error occurs
            nukkitBiome = EnumBiome.PLAINS;
        }
        
        // Create new biome and store in cache
        NukkitPlatformBiome newBiome = new NukkitPlatformBiome(nukkitBiome);
        BIOME_CACHE.put(biomeId, newBiome);
        
        return newBiome;
    }

    @Override
    public @NotNull ChunkGenerator getGenerator() {
        return Objects.requireNonNull(chunkGenerator, "ChunkGenerator not initialized!");
    }

    @Override
    public @NotNull BiomeProvider getBiomeProvider() {
        return Objects.requireNonNull(configPack, "ConfigPack not initialized!").getBiomeProvider();
    }

    @Override
    public @NotNull ConfigPack getPack() {
        return Objects.requireNonNull(configPack, "ConfigPack not initialized!");
    }

    @Override
    public long getSeed() {
        return level != null ? level.getSeed() : 0L;
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public int getMinHeight() {
        return minHeight;
    }

    @Override
    public Object getHandle() {
        return level;
    }

    @Override
    public Chunk getChunkAt(int x, int z) {
        if (level == null) {
            LOGGER.debug("Level is null for world {} when attempting to get chunk at {},{}", worldName, x, z);
            return null;
        }
        
        // Create a unique key for this chunk
        long chunkKey = ((long)x << 32) | (z & 0xFFFFFFFFL);
        
        // Check if we already have this chunk cached
        NukkitChunk cached = chunkCache.get(chunkKey);
        if (cached != null) {
            return cached;
        }
        
        try {
            // Ensure the chunk is loaded before trying to get it
            level.loadChunk(x, z);
            FullChunk chunk = level.getChunk(x, z);
            
            if (chunk == null) {
                return null;
            }
            
            NukkitChunk nukkitChunk = new NukkitChunk(chunk, this, configPack, platform);
            
            // Cache the chunk for future use
            chunkCache.put(chunkKey, nukkitChunk);
            
            return nukkitChunk;
        } catch (Exception e) {
            LOGGER.error("Failed to get chunk at {}, {}: {}", x, z, e.getMessage());
            return null;
        }
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        if (level == null || y < 0 || y >= getMaxHeight()) {
            return AIR_BLOCKSTATE;
        }
        
        try {
            Block block = level.getBlock(x, y, z);
            if (block == null) {
                return AIR_BLOCKSTATE;
            }
            
            // Create a NukkitJeBlockState from the block
            NukkitJeBlockState jeBlockState = NukkitMapping.blockStateNukkitToJe(block);
            return new NukkitBlockState(block, jeBlockState);
        } catch (Exception e) {
            LOGGER.debug("Failed to get block state at {}, {}, {}: {}", x, y, z, e.getMessage());
            return AIR_BLOCKSTATE;
        }
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        if (level == null || y < 0 || y >= getMaxHeight()) {
            return;
        }
        
        try {
            // Handle NukkitBlockState directly if possible
            if (data instanceof NukkitBlockState) {
                Block nukkitBlock = ((NukkitBlockState) data).getNukkitBlock();
                level.setBlock(x, y, z, nukkitBlock, false, physics);
                return;
            }
            
            // Otherwise use the mappings
            Block nukkitBlock = NukkitMapping.blockStateJeToNukkit(
                NukkitJeBlockState.fromString(data.getAsString(true)));
            level.setBlock(x, y, z, nukkitBlock, false, physics);
        } catch (Exception e) {
            LOGGER.error("Failed to set block state at {}, {}, {}: {}", x, y, z, e.getMessage());
        }
    }

    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        // Nukkit doesn't have a direct BlockEntity equivalent to Terra's
        // For now, return null, but a more sophisticated implementation could map between the two
        return null;
    }

    @Override
    public com.dfsek.terra.api.entity.Entity spawnEntity(double x, double y, double z, EntityType entityType) {
        if (level == null) {
            LOGGER.error("Cannot spawn entity: level is null");
            return null;
        }
        
        try {
            // Convert Terra EntityType to Nukkit entity ID
            String entityId = "minecraft:zombie"; // Default fallback
            
            if (entityType.getHandle() instanceof String) {
                entityId = (String) entityType.getHandle();
            }
            
            // Use Nukkit's entity creation mechanism
            cn.nukkit.nbt.tag.CompoundTag nbt = new cn.nukkit.nbt.tag.CompoundTag()
                    .putList(new cn.nukkit.nbt.tag.ListTag<>("Pos")
                        .add(new cn.nukkit.nbt.tag.DoubleTag("", x))
                        .add(new cn.nukkit.nbt.tag.DoubleTag("", y))
                        .add(new cn.nukkit.nbt.tag.DoubleTag("", z)))
                    .putList(new cn.nukkit.nbt.tag.ListTag<>("Motion")
                        .add(new cn.nukkit.nbt.tag.DoubleTag("", 0))
                        .add(new cn.nukkit.nbt.tag.DoubleTag("", 0))
                        .add(new cn.nukkit.nbt.tag.DoubleTag("", 0)))
                    .putList(new cn.nukkit.nbt.tag.ListTag<>("Rotation")
                        .add(new cn.nukkit.nbt.tag.FloatTag("", 0))
                        .add(new cn.nukkit.nbt.tag.FloatTag("", 0)));
            
            // Try to create entity
            Entity entity = Entity.createEntity(entityId, level.getChunk((int)x >> 4, (int)z >> 4), nbt);
            
            if (entity != null) {
                entity.spawnToAll();
                return new NukkitEntity(entity);
            } else {
                LOGGER.error("Failed to create entity with ID: {}", entityId);
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to spawn entity at {}, {}, {}: {}", x, y, z, e.getMessage());
            return null;
        }
    }
    
    /**
     * Set the ChunkGenerator for this world.
     * This is called by the platform when the generator is created.
     * 
     * @param generator The generator to use
     */
    public void setGenerator(ChunkGenerator generator) {
        this.chunkGenerator = generator;
    }
    
    /**
     * Clear chunk cache to free memory when worlds are unloaded
     */
    public void clearChunkCache() {
        chunkCache.clear();
    }
    
    /**
     * Get the name of this world.
     * 
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Set the minimum height for this world
     * 
     * @param minHeight New minimum height
     */
    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }
    
    /**
     * Set the maximum height for this world
     * 
     * @param maxHeight New maximum height
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }
} 