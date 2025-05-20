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

package com.dfsek.terra.nukkit.generator;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.Level;
import cn.nukkit.level.biome.Biome;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.world.NukkitWorld;
import com.dfsek.terra.nukkit.world.biome.NukkitPlatformBiome;
import com.dfsek.terra.nukkit.world.chunk.NukkitProtoChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Nukkit generator implementation for Terra.
 */
public class NukkitGenerator extends Generator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitGenerator.class);
    
    private ChunkManager chunkManager;
    private Random random;
    private NukkitRandom nukkitRandom;
    private String worldName;
    private final Map<String, Object> options;
    
    private volatile ConfigPack configPack;
    private volatile ChunkGenerator terraGenerator;
    private volatile BiomeProvider biomeProvider;
    private volatile NukkitWorld terraWorld;
    
    // Thread-local initialization flag to ensure we only initialize once per thread
    private final ThreadLocal<Boolean> initialized = ThreadLocal.withInitial(() -> false);

    public NukkitGenerator(Map<String, Object> options) {
        this.options = options != null ? options : new HashMap<>();
    }

    /**
     * Get the ConfigPack associated with this generator
     * 
     * @return The ConfigPack used by this generator
     */
    public ConfigPack getConfigPack() {
        return configPack;
    }
    
    /**
     * Set the ConfigPack to be used by this generator
     * 
     * @param configPack The ConfigPack to use
     */
    public void setConfigPack(ConfigPack configPack) {
        this.configPack = configPack;
    }

    @Override
    public int getId() {
        return Generator.TYPE_INFINITE;
    }

    @Override
    public void init(ChunkManager chunkManager, NukkitRandom nukkitRandom) {
        this.chunkManager = chunkManager;
        this.nukkitRandom = nukkitRandom;
        
        // Create a random from the Nukkit random
        long seed = 0;
        if (chunkManager instanceof Level) {
            seed = ((Level) chunkManager).getSeed();
        }
        this.random = new Random(seed);
        
        // Get world name from level if possible
        if (chunkManager instanceof Level) {
            this.worldName = ((Level) chunkManager).getName();
        } else {
            this.worldName = "unknown";
        }
        
        LOGGER.info("Initializing Terra generator for world: " + worldName);
        
        // Try to get a config pack for this specific generator
        try {
            // First check if this generator has a specific pack
            this.configPack = getConfigPack();
            
            // If no specific pack, try to get one from the platform
            if (this.configPack == null && TerraNukkitPlugin.PLATFORM != null) {
                // Try to get config pack ID from options if available
                String packId = null;
                if (options.containsKey("pack")) {
                    packId = options.get("pack").toString();
                }
                
                if (packId != null && !packId.isEmpty()) {
                    // Look up the pack by ID
                    this.configPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                         .getByID(packId)
                                         .orElse(null);
                }
                
                // If still null, use default/first available pack
                if (this.configPack == null) {
                    this.configPack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                }
            }
            
            LOGGER.info("Using config pack: " + 
                       (configPack != null ? configPack.getID() : "none (using fallback)"));
            
            // Debug the config pack
            if (configPack != null) {
                debugConfigPack(configPack);
            }
            
            // Create the Terra world with the selected config pack
            if (chunkManager instanceof Level) {
                Level level = (Level) chunkManager;
                this.terraWorld = new NukkitWorld(level, null, configPack, TerraNukkitPlugin.PLATFORM);
                
                // Create the generator and biome provider now
                try {
                    if (configPack.getGeneratorProvider() != null) {
                        LOGGER.info("Creating Terra generator during initialization");
                        this.terraGenerator = configPack.getGeneratorProvider().newInstance(configPack);
                        if (this.terraGenerator != null) {
                            LOGGER.info("Successfully created Terra generator: {}", terraGenerator.getClass().getName());
                            // Set the generator in the world
                            this.terraWorld.setGenerator(this.terraGenerator);
                        } else {
                            LOGGER.error("Failed to create Terra generator - result is null");
                        }
                    } else {
                        LOGGER.error("No generator provider in config pack");
                    }
                    
                    this.biomeProvider = configPack.getBiomeProvider();
                    if (this.biomeProvider != null) {
                        LOGGER.info("Successfully got biome provider: {}", biomeProvider.getClass().getName());
                    } else {
                        LOGGER.error("Failed to get biome provider - result is null");
                    }
                } catch (Exception e) {
                    LOGGER.error("Error creating Terra generator or biome provider", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize generator: " + e.getMessage(), e);
        }
            
        LOGGER.info("Terra Nukkit generator ready for world: " + worldName);
    }
    
    /**
     * Debug the config pack to help diagnose issues
     */
    private void debugConfigPack(ConfigPack pack) {
        LOGGER.info("ConfigPack Debug Info:");
        LOGGER.info("  ID: {}", pack.getID());
        LOGGER.info("  Has Generator Provider: {}", pack.getGeneratorProvider() != null);
        LOGGER.info("  Has Biome Provider: {}", pack.getBiomeProvider() != null);
        
        if (pack.getGeneratorProvider() != null) {
            try {
                LOGGER.info("  Generator Provider Class: {}", pack.getGeneratorProvider().getClass().getName());
            } catch (Exception e) {
                LOGGER.error("  Error getting generator provider class", e);
            }
        }
        
        if (pack.getBiomeProvider() != null) {
            try {
                LOGGER.info("  Biome Provider Class: {}", pack.getBiomeProvider().getClass().getName());
            } catch (Exception e) {
                LOGGER.error("  Error getting biome provider class", e);
            }
        }
    }

    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        LOGGER.info("Generating chunk {}, {} for world {} on thread {}", 
                  chunkX, chunkZ, worldName, Thread.currentThread().getName());
        
        // Ensure initialization for this thread
        ensureInitialized();
        
        // Use Terra generation if we have a config pack, otherwise use fallback
        if (configPack != null && terraWorld != null) {
            LOGGER.info("Have config pack and Terra world for chunk {}, {}", chunkX, chunkZ);
            try {
                // Initialize generator if not yet done
                if (terraGenerator == null && configPack.getGeneratorProvider() != null) {
                    LOGGER.info("Creating new Terra generator instance for pack: {}", configPack.getID());
                    terraGenerator = configPack.getGeneratorProvider().newInstance(configPack);
                    if (terraGenerator == null) {
                        LOGGER.error("Failed to create Terra generator - generator is null after provider.newInstance()");
                    } else {
                        LOGGER.info("Successfully created Terra generator: {}", terraGenerator.getClass().getName());
                        terraWorld.setGenerator(terraGenerator);
                    }
                } else if (terraGenerator == null) {
                    LOGGER.error("No generator provider available in config pack: {}", configPack.getID());
                }
                
                if (biomeProvider == null) {
                    LOGGER.info("Getting biome provider from config pack: {}", configPack.getID());
                    biomeProvider = configPack.getBiomeProvider();
                    if (biomeProvider == null) {
                        LOGGER.error("Failed to get biome provider from config pack");
                    } else {
                        LOGGER.info("Successfully got biome provider: {}", biomeProvider.getClass().getName());
                    }
                }
                
                if (terraGenerator != null) {
                    LOGGER.info("Using Terra generation for chunk {}, {} in world {} with pack {}", 
                                chunkX, chunkZ, worldName, configPack.getID());
                    
                    // Generate the chunk using Terra
                    BaseFullChunk chunk = (BaseFullChunk) chunkManager.getChunk(chunkX, chunkZ);
                    if (chunk != null) {
                        LOGGER.info("Got chunk from chunk manager for {}, {}", chunkX, chunkZ);
                        NukkitProtoChunk protoChunk = new NukkitProtoChunk(chunk, terraWorld);
                        
                        // First set biomes for the chunk
                        if (biomeProvider != null) {
                            LOGGER.debug("Setting biomes for chunk {}, {}", chunkX, chunkZ);
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    int worldX = chunkX * 16 + x;
                                    int worldZ = chunkZ * 16 + z;
                                    
                                    // Get biome from Terra's biome provider
                                    com.dfsek.terra.api.world.biome.Biome terraBiome = biomeProvider.getBiome(worldX, 0, worldZ, random.nextLong());
                                    
                                    // Convert to platform biome and set in chunk
                                    if (terraBiome instanceof com.dfsek.terra.api.world.biome.PlatformBiome platformBiome) {
                                        protoChunk.setBiome(x, z, platformBiome);
                                    }
                                }
                            }
                        } else {
                            LOGGER.warn("No biome provider available for chunk {}, {}", chunkX, chunkZ);
                        }
                        
                        // Then generate the terrain
                        LOGGER.info("Calling Terra generator for chunk {}, {}", chunkX, chunkZ);
                        try {
                            terraGenerator.generateChunkData(protoChunk, terraWorld, biomeProvider, chunkX, chunkZ);
                            LOGGER.info("Successfully generated terrain for chunk {}, {}", chunkX, chunkZ);
                            return;
                        } catch (Exception e) {
                            LOGGER.error("Error generating terrain for chunk {}, {}: {}", chunkX, chunkZ, e.getMessage(), e);
                        }
                    } else {
                        LOGGER.warn("Failed to get chunk {}, {} from chunk manager", chunkX, chunkZ);
                    }
                } else {
                    LOGGER.warn("No Terra generator available for chunk {}, {} in world {}. Using fallback generation.", 
                               chunkX, chunkZ, worldName);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to generate chunk {}, {} with Terra: {}", chunkX, chunkZ, e.getMessage(), e);
            }
        } else {
            LOGGER.info("No config pack or Terra world available for chunk {}, {}. Using fallback generation.", 
                      chunkX, chunkZ);
        }
        
        // Fallback to basic terrain if Terra generation failed or wasn't available
        LOGGER.info("Using fallback generation for chunk {}, {}", chunkX, chunkZ);
        generateFallbackChunk(chunkX, chunkZ);
    }
    
    /**
     * Ensures the generator is initialized for the current thread
     */
    private synchronized void ensureInitialized() {
        if (initialized.get()) {
            return; // Already initialized for this thread
        }
        
        LOGGER.info("Initializing Terra generator for thread {}", Thread.currentThread().getName());
        
        // Try to get a config pack for this specific generator
        try {
            // First check if this generator has a specific pack
            if (this.configPack == null) {
                this.configPack = getConfigPack();
            }
            
            // If no specific pack, try to get one from the platform
            if (this.configPack == null && TerraNukkitPlugin.PLATFORM != null) {
                // Try to get config pack ID from options if available
                String packId = null;
                if (options.containsKey("pack")) {
                    packId = options.get("pack").toString();
                }
                
                if (packId != null && !packId.isEmpty()) {
                    // Look up the pack by ID
                    this.configPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                         .getByID(packId)
                                         .orElse(null);
                }
                
                // If still null, use default/first available pack
                if (this.configPack == null) {
                    this.configPack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                }
            }
            
            LOGGER.info("Thread {} using config pack: {}", 
                      Thread.currentThread().getName(),
                      (configPack != null ? configPack.getID() : "none (using fallback)"));
            
            // Debug the config pack
            if (configPack != null) {
                debugConfigPack(configPack);
            }
            
            // Create the Terra world with the selected config pack
            if (chunkManager instanceof Level) {
                Level level = (Level) chunkManager;
                if (this.terraWorld == null) {
                    this.terraWorld = new NukkitWorld(level, null, configPack, TerraNukkitPlugin.PLATFORM);
                }
                
                // Create the generator and biome provider now
                try {
                    if (configPack != null && configPack.getGeneratorProvider() != null) {
                        LOGGER.info("Creating Terra generator during initialization for thread {}", 
                                  Thread.currentThread().getName());
                        this.terraGenerator = configPack.getGeneratorProvider().newInstance(configPack);
                        if (this.terraGenerator != null) {
                            LOGGER.info("Successfully created Terra generator: {}", terraGenerator.getClass().getName());
                            // Set the generator in the world
                            this.terraWorld.setGenerator(this.terraGenerator);
                        } else {
                            LOGGER.error("Failed to create Terra generator - result is null");
                        }
                    } else {
                        LOGGER.error("No generator provider in config pack");
                    }
                    
                    if (configPack != null) {
                        this.biomeProvider = configPack.getBiomeProvider();
                        if (this.biomeProvider != null) {
                            LOGGER.info("Successfully got biome provider: {}", biomeProvider.getClass().getName());
                        } else {
                            LOGGER.error("Failed to get biome provider - result is null");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error creating Terra generator or biome provider", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize generator for thread {}: {}", 
                        Thread.currentThread().getName(), e.getMessage(), e);
        }
        
        LOGGER.info("Terra Nukkit generator ready for thread {}", Thread.currentThread().getName());
        
        // Mark as initialized for this thread
        initialized.set(true);
    }
    
    /**
     * Generate a simple fallback chunk if Terra generation fails.
     *
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    private void generateFallbackChunk(int chunkX, int chunkZ) {
        BaseFullChunk chunk = (BaseFullChunk) chunkManager.getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }
        
        // Generate a simple flat world as fallback
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.setBiomeId(x, z, 1); // Plains biome ID is 1 in Nukkit
                
                for (int y = 0; y < 256; y++) {
                    if (y == 0) {
                        chunk.setBlock(x, y, z, Block.BEDROCK);
                    } else if (y < 63) {
                        chunk.setBlock(x, y, z, Block.STONE);
                    } else if (y == 63) {
                        chunk.setBlock(x, y, z, Block.GRASS);
                    }
                }
            }
        }
    }
    
    /**
     * Get the biome ID at the given coordinates.
     *
     * @param x X coordinate
     * @param z Z coordinate
     * @return Biome ID
     */
    private int getBiomeId(int x, int z) {
        if (biomeProvider == null) {
            return 1; // Plains biome ID is 1 in Nukkit
        }
        
        try {
            // Get a biome from the Terra provider
            // The actual implementation may vary depending on Terra's API
            com.dfsek.terra.api.world.biome.Biome terraBiome = biomeProvider.getBiome(x, 0, z, random.nextLong());
            
            if (terraBiome == null) {
                return 1; // Default to plains
            }
            
            // Use Terra Biome ID as identifier since getRegistryKey() might not be accessible
            return 1; // Default to plains for now until we properly resolve it
        } catch (Exception e) {
            LOGGER.error("Error getting biome at " + x + ", " + z, e);
            return 1; // Default to plains
        }
    }

    @Override
    public void populateChunk(int chunkX, int chunkZ) {
        // For now, just implement a basic version without using Terra's populate method
        // since we're still developing the Nukkit integration
        if (terraWorld == null) {
            return;
        }
        
        // In the future, this would use Terra's population methods
    }

    @Override
    public Map<String, Object> getSettings() {
        return options;
    }

    @Override
    public String getName() {
        return "terra";
    }

    @Override
    public Vector3 getSpawn() {
        // Try to find a suitable spawn location
        return new Vector3(0, 70, 0); // Default spawn position
    }

    @Override
    public ChunkManager getChunkManager() {
        return chunkManager;
    }
    
    /**
     * A specialized generator class that's bound to a specific config pack
     */
    public static class PackSpecificGenerator extends NukkitGenerator {
        private final String packId;
        private final ConfigPack pack;
        
        public PackSpecificGenerator(String packId, ConfigPack pack) {
            super(new HashMap<>()); // Initialize with empty options
            this.packId = packId;
            this.pack = pack;
        }
        
        public PackSpecificGenerator(Map<String, Object> options) {
            super(options);
            // Extract pack ID from options if available
            String id = options != null && options.containsKey("pack") ? options.get("pack").toString() : "default";
            this.packId = id;
            // The pack will be set later in init()
            this.pack = null;
        }
        
        @Override
        public ConfigPack getConfigPack() {
            // Return our specific pack if available
            if (pack != null) {
                return pack;
            }
            
            // Otherwise try to look it up by ID from the platform
            if (TerraNukkitPlugin.PLATFORM != null && packId != null) {
                return TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                      .getByID(packId)
                      .orElse(super.getConfigPack());
            }
            
            return super.getConfigPack();
        }
        
        @Override
        public String getName() {
            return "terra:" + packId;
        }
    }
} 