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
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Nukkit generator implementation for Terra.
 */
public class NukkitGenerator extends Generator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitGenerator.class);
    
    // Constants for option keys
    private static final String OPTION_PACK = "pack";
    private static final String OPTION_PRESET = "preset";
    private static final String OPTION_WORLD_NAME = "worldName";
    private static final String OPTION_SEED = "seed";
    
    private ChunkManager chunkManager;
    private Level level; // Store the level reference once we have it
    private Random random;
    private NukkitRandom nukkitRandom;
    private String worldName;
    private final Map<String, Object> options;
    
    // Shared registry of all active generators to help with pack updates
    private static final Map<String, NukkitGenerator> ACTIVE_GENERATORS = new HashMap<>();
    
    // Static cache of generators by world name to avoid re-initialization
    private static final Map<String, GeneratorCache> GENERATOR_CACHE = new ConcurrentHashMap<>();
    
    // Global lock objects to prevent concurrent initialization of the same world
    private static final Map<String, Object> WORLD_INIT_LOCKS = new ConcurrentHashMap<>();
    
    // Track global initialization status per world, independent of thread
    private static final Map<String, AtomicBoolean> WORLD_INITIALIZED = new ConcurrentHashMap<>();
    
    // Private static atomic flag to prevent a world from initializing multiple times in a row
    private static final Map<String, AtomicBoolean> WORLD_INITIALIZING_FLAGS = new ConcurrentHashMap<>();
    
    private volatile ConfigPack configPack;
    private volatile boolean initialized = false;
    
    // Thread-local recursion detection to prevent recursive generator creation
    private static final ThreadLocal<AtomicInteger> RECURSION_COUNTER = ThreadLocal.withInitial(AtomicInteger::new);
    private static final int MAX_RECURSION_DEPTH = 3;
    
    // Track whether we're currently in the constructor to prevent recursion
    private static final ThreadLocal<Boolean> IN_CONSTRUCTOR = ThreadLocal.withInitial(() -> Boolean.FALSE);
    
    // Thread-local state to track world names for async generation
    private static final ThreadLocal<Map<Long, String>> ASYNC_WORLD_NAMES = ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    // Add a thread-local flag to track if we're currently initializing a generator
    private static final ThreadLocal<Boolean> INITIALIZING_GENERATOR = ThreadLocal.withInitial(() -> Boolean.FALSE);
    
    // Add a ThreadLocal map to cache generators by thread ID for async operations
    private static final ThreadLocal<Map<String, GeneratorCache>> THREAD_LOCAL_CACHE = 
        ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    // Track which generators have been initialized on each thread
    private static final ThreadLocal<Set<String>> THREAD_INITIALIZED_WORLDS = 
        ThreadLocal.withInitial(HashSet::new);
    
    // Static registry mapping level instances to world names for cross-thread access
    private static final Map<Level, String> LEVEL_TO_WORLD_NAME = new ConcurrentHashMap<>();
    
    // Static registry to associate a generator instance with a world name
    // This field is thread-safe and guaranteed to be the same across all threads
    private static final Map<String, String> ASYNC_THREAD_NAME_PREFIX_MAP = new ConcurrentHashMap<>();
    
    // Holds the initialized generator components for this instance
    private static class GeneratorCache {
        final ChunkGenerator terraGenerator;
        final BiomeProvider biomeProvider;
        final NukkitWorld terraWorld;
        final long lastUsed;
        final String worldName; // Add world name for better tracking
        
        GeneratorCache(ChunkGenerator generator, BiomeProvider provider, NukkitWorld world, String worldName) {
            this.terraGenerator = generator;
            this.biomeProvider = provider;
            this.terraWorld = world;
            this.lastUsed = System.currentTimeMillis();
            this.worldName = worldName;
        }
        
        // Get a fresh timestamp for cache updates
        GeneratorCache refreshUsage() {
            return new GeneratorCache(terraGenerator, biomeProvider, terraWorld, worldName);
        }
    }

    public NukkitGenerator(Map<String, Object> options) {
        // Check if we're already in this constructor to prevent recursion
        if (IN_CONSTRUCTOR.get()) {
            LOGGER.error("Recursive generator creation detected! Using empty options to break recursion.");
            this.options = new HashMap<>();
            return;
        }
        
        try {
            IN_CONSTRUCTOR.set(true);
            AtomicInteger counter = RECURSION_COUNTER.get();
            int depth = counter.incrementAndGet();
            
            if (depth > MAX_RECURSION_DEPTH) {
                LOGGER.error("Excessive recursion depth ({}) detected during NukkitGenerator creation! Using empty options.", depth);
                this.options = new HashMap<>();
                return;
            }
            
            this.options = options != null ? new HashMap<>(options) : new HashMap<>(); // Create a defensive copy
            
            // Try to get worldName from thread name first for async threads
            String threadBasedWorldName = getWorldNameFromThreadName();
            if (threadBasedWorldName != null) {
                System.out.println("[DEBUG] Found world name '" + threadBasedWorldName + "' based on thread name: " + 
                                 Thread.currentThread().getName());
                this.worldName = threadBasedWorldName;
                // Store this in the options too
                this.options.put(OPTION_WORLD_NAME, threadBasedWorldName);
                // Store for async access
                ASYNC_WORLD_NAMES.get().put(Thread.currentThread().getId(), threadBasedWorldName);
            }
            // Otherwise try options
            else if (options != null && options.containsKey(OPTION_WORLD_NAME)) {
                String worldName = options.get(OPTION_WORLD_NAME).toString();
                if (worldName != null && !worldName.isEmpty()) {
                    // Store the world name for this thread keyed by thread ID
                    ASYNC_WORLD_NAMES.get().put(Thread.currentThread().getId(), worldName);
                    this.worldName = worldName;
                }
            }
            
            // Check for pack ID as early as possible, but avoid triggering more generator creation
            if (depth <= 1) {
                String packId = getPackIdFromOptions(this.options);
                if (packId != null && !packId.isEmpty() && TerraNukkitPlugin.PLATFORM != null) {
                    ConfigPack optionsPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                        .getByID(packId)
                                        .orElse(null);
                    if (optionsPack != null) {
                        LOGGER.debug("Using config pack '{}' from initial options", packId);
                        this.configPack = optionsPack;
                    }
                }
            }
            
            // Add this generator to the global registry if we have a world name
            if (this.worldName != null && !this.worldName.isEmpty()) {
                // Avoid replacing an existing generator unless it's our first recursion level
                if (depth <= 1 || !ACTIVE_GENERATORS.containsKey(this.worldName)) {
                    ACTIVE_GENERATORS.put(this.worldName, this);
                    LOGGER.debug("Registered generator for world: {}", this.worldName);
                }
            }
        } finally {
            // Ensure counter is always properly decremented, even in case of exceptions
            RECURSION_COUNTER.get().decrementAndGet();
            IN_CONSTRUCTOR.set(false);
        }
    }
    
    /**
     * Helper method to extract pack ID from options map
     * Handles both "pack" and "preset" keys for compatibility
     */
    private static String getPackIdFromOptions(Map<String, Object> options) {
        if (options == null) {
            return null;
        }
        
        // First try the "pack" option (our standard)
        if (options.containsKey(OPTION_PACK)) {
            Object packObj = options.get(OPTION_PACK);
            if (packObj != null) {
                String packId = packObj.toString();
                if (packId != null && !packId.isEmpty()) {
                    return packId;
                }
            }
        }
        
        // Then try "preset" option (for backward compatibility)
        if (options.containsKey(OPTION_PRESET)) {
            Object presetObj = options.get(OPTION_PRESET);
            if (presetObj != null) {
                String packId = presetObj.toString();
                if (packId != null && !packId.isEmpty()) {
                    return packId;
                }
            }
        }
        
        // If we get here, no valid pack ID was found
        return null;
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
        // Clear cache when changing pack
        if (worldName != null) {
            GENERATOR_CACHE.remove(worldName);
            initialized = false;
        }
    }

    @Override
    public int getId() {
        return Generator.TYPE_INFINITE;
    }

    @Override
    public void init(ChunkManager chunkManager, NukkitRandom nukkitRandom) {
        // Try to get the world name first
        String worldId = null;
        
        // If chunkManager is a Level, store the reference and get the name
        if (chunkManager instanceof Level) {
            this.level = (Level) chunkManager;
            
            // Check both the Level's getName() and our registry
            worldId = level.getName();
            
            // Always register the level name for other threads
            registerLevelName(level);
        }
        
        // Try options if we don't have a name from the level
        if (worldId == null || worldId.isEmpty()) {
            if (options.containsKey(OPTION_WORLD_NAME)) {
                worldId = options.get(OPTION_WORLD_NAME).toString();
            } else if (this.worldName != null && !this.worldName.equals("unknown")) {
                worldId = this.worldName;
            } else {
                Map<Long, String> threadNames = ASYNC_WORLD_NAMES.get();
                worldId = threadNames.get(Thread.currentThread().getId());
            }
        }
        
        // If we have a valid ID, check if it's already being initialized
        if (worldId != null && !worldId.isEmpty() && !worldId.equals("unknown")) {
            // Keep track of the world name for this thread
            ASYNC_WORLD_NAMES.get().put(Thread.currentThread().getId(), worldId);
            this.worldName = worldId; // Make sure worldName is set correctly
            
            // Register in global mapping
            ACTIVE_GENERATORS.put(worldId, this);
            
            // Check if this world is already in the initialization process
            AtomicBoolean isInitializing = WORLD_INITIALIZING_FLAGS.computeIfAbsent(worldId, k -> new AtomicBoolean(false));
            
            if (isInitializing.get()) {
                // This world is already being initialized, just set basic fields and return
                LOGGER.debug("Skipping redundant initialization for world {} - already in progress on thread {}", 
                          worldId, Thread.currentThread().getName());
                this.chunkManager = chunkManager;
                this.nukkitRandom = nukkitRandom;
                return;
            }
            
            try {
                // Set the flag to prevent concurrent initialization of the same world
                isInitializing.set(true);
                
                // Now proceed with normal initialization
                System.out.println("[DEBUG] Terra initializing world: " + worldId + " on thread: " + Thread.currentThread().getName());
                
                // Set basic fields
                this.chunkManager = chunkManager;
                this.nukkitRandom = nukkitRandom;
                
                // Check if we're already initialized
                Set<String> initializedWorlds = THREAD_INITIALIZED_WORLDS.get();
                if (initialized && initializedWorlds.contains(worldId)) {
                    LOGGER.debug("Generator already initialized for world: {}", worldId);
                    return;
                }
                
                // Call into the normal initialization routine
                doInitialization(initializedWorlds);
                
                // Register this generator for the world
                ACTIVE_GENERATORS.put(worldId, this);
            } finally {
                // Clear the initialization flag
                isInitializing.set(false);
            }
        } else {
            // For unknown worlds, always proceed
            this.chunkManager = chunkManager;
            this.nukkitRandom = nukkitRandom;
            doInitialization(THREAD_INITIALIZED_WORLDS.get());
        }
    }
    
    /**
     * Perform the actual initialization after acquiring locks and checking cache
     * 
     * @param initializedWorlds Thread-local set of initialized worlds
     */
    private void doInitialization(Set<String> initializedWorlds) {
        // Create a random with the seed from the level or options
        long seed = 0;
        if (level != null) {
            seed = level.getSeed();
        } else if (options.containsKey(OPTION_SEED)) {
            try {
                seed = Long.parseLong(options.get(OPTION_SEED).toString());
                LOGGER.debug("Using seed from options: {}", seed);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid seed in options: {}", options.get(OPTION_SEED));
            }
        }
        this.random = new Random(seed);
        
        LOGGER.info("Initializing Terra generator for world: {}", worldName);
        
        // Try to get a config pack for this specific generator
        try {
            // First check if this generator has a specific pack
            if (this.configPack == null) {
                // If options contain a pack ID, use that (highest priority)
                if (options.containsKey(OPTION_PACK)) {
                    String packId = options.get(OPTION_PACK).toString();
                    if (packId != null && !packId.isEmpty() && TerraNukkitPlugin.PLATFORM != null) {
                        ConfigPack optionsPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                                 .getByID(packId)
                                                 .orElse(null);
                        if (optionsPack != null) {
                            LOGGER.info("Using config pack '{}' from options", packId);
                            this.configPack = optionsPack;
                        } else {
                            LOGGER.warn("Could not find config pack with ID '{}' specified in options", packId);
                        }
                    }
                }
            }
            
            // Check if the world name matches a config pack name
            if (this.configPack == null && TerraNukkitPlugin.PLATFORM != null && worldName != null) {
                // Special case for "origen" world
                if (worldName.equalsIgnoreCase("origen") || worldName.equalsIgnoreCase("origin")) {
                    this.configPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                         .getByID("ORIGEN")
                                         .orElse(null);
                    if (this.configPack != null) {
                        LOGGER.info("Found config pack 'ORIGEN' matching world name: {}", worldName);
                        // Update options to include pack ID
                        options.put(OPTION_PACK, "ORIGEN");
                    }
                } 
                // Try to find any pack with a similar name
                else {
                    String normalizedWorldName = worldName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    
                    for (ConfigPack pack : TerraNukkitPlugin.PLATFORM.getConfigRegistry().entries()) {
                        String packId = pack.getID();
                        String normalizedPackId = packId.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                        
                        // Check for name similarity
                        if (normalizedWorldName.equals(normalizedPackId) || 
                            normalizedWorldName.contains(normalizedPackId) || 
                            normalizedPackId.contains(normalizedWorldName)) {
                            LOGGER.info("Found config pack '{}' matching world name: {}", packId, worldName);
                            this.configPack = pack;
                            // Update options to include pack ID
                            options.put(OPTION_PACK, packId);
                            break;
                        }
                    }
                }
            }
            
            // If no specific pack, try to get one from the platform
            if (this.configPack == null && TerraNukkitPlugin.PLATFORM != null) {
                // If still null, use default/first available pack
                this.configPack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                if (this.configPack != null) {
                    LOGGER.info("Using default config pack: {}", this.configPack.getID());
                    // Update options to include pack ID
                    options.put(OPTION_PACK, this.configPack.getID());
                }
            }
            
            LOGGER.info("Using config pack: {}", 
                       (configPack != null ? configPack.getID() : "none (using fallback)"));
            
            // Make sure options are updated with all necessary information
            if (configPack != null) {
                options.put(OPTION_PACK, configPack.getID());
                options.put(OPTION_WORLD_NAME, worldName);
                if (level != null) {
                    options.put(OPTION_SEED, String.valueOf(level.getSeed()));
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize generator: " + e.getMessage(), e);
        }
        
        initialized = true;
        
        // Mark as initialized on this thread
        if (worldName != null) {
            initializedWorlds.add(worldName);
        }
        
        LOGGER.info("Terra Nukkit generator ready for world: {}", worldName);
    }

    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        // Get world name with optimizations for speed
        String currentWorldName = this.worldName;
        
        // Fast path for common case - if world name is already known, skip expensive lookups
        if (currentWorldName == null || currentWorldName.equals("unknown")) {
            // Use thread-local storage first (fastest)
            Map<Long, String> threadNames = ASYNC_WORLD_NAMES.get();
            currentWorldName = threadNames.get(Thread.currentThread().getId());
            
            if (currentWorldName == null) {
                // Fall back to other methods only if necessary
                currentWorldName = resolveWorldName();
                if (currentWorldName != null) {
                    // Store for future use
                    this.worldName = currentWorldName;
                    threadNames.put(Thread.currentThread().getId(), currentWorldName);
                }
            }
            
            // If still unknown after all attempts, use fallback
            if (currentWorldName == null || currentWorldName.equals("unknown")) {
                LOGGER.warn("Cannot determine world name for chunk generation at {}, {} - using fallback", chunkX, chunkZ);
                generateFallbackChunk(chunkX, chunkZ);
                return;
            }
        }
        
        // Use thread-local cache for generator components (faster)
        GeneratorCache cache = THREAD_LOCAL_CACHE.get().get(currentWorldName);
        
        // Check thread-local cache first (fastest)
        if (cache == null) {
            // Only if missing in thread-local, try global cache
            cache = GENERATOR_CACHE.get(currentWorldName);
            
            // If found in global but not thread-local, store in thread-local for next use
            if (cache != null) {
                THREAD_LOCAL_CACHE.get().put(currentWorldName, cache);
            } else {
                // If not found anywhere, initialize (expensive)
                cache = initializeGeneratorComponents(currentWorldName);
                if (cache == null) {
                    // Fallback if initialization failed
                    generateFallbackChunk(chunkX, chunkZ);
                    return;
                }
            }
        }
        
        // Only continue if we have valid components
        if (cache.terraWorld == null || cache.terraGenerator == null || cache.biomeProvider == null) {
            LOGGER.warn("Invalid generator cache for world {}", currentWorldName);
            generateFallbackChunk(chunkX, chunkZ);
            return;
        }
        
        // Get the chunk - directly from manager if possible
        BaseFullChunk chunk = null;
        try {
            // Try chunk manager first (faster)
            if (chunkManager != null) {
                chunk = (BaseFullChunk) chunkManager.getChunk(chunkX, chunkZ);
            }
            
            // If not found, try level
            if (chunk == null && level != null) {
                chunk = level.getChunk(chunkX, chunkZ);
            }
            
            // If still null, we can't generate
            if (chunk == null) {
                LOGGER.warn("Couldn't get chunk at {},{} for generation", chunkX, chunkZ);
                return;
            }
            
            // Create proto chunk in the most efficient way
            NukkitProtoChunk protoChunk = new NukkitProtoChunk(chunk, cache.terraWorld);
            
            // Generate terrain efficiently
            generateTerrain(chunkX, chunkZ, protoChunk, cache);
            
        } catch (Exception e) {
            LOGGER.error("Error generating terrain for chunk {}, {}: {}", 
                      chunkX, chunkZ, e.getMessage());
            // Try fallback if main generation fails
            try {
                generateFallbackChunk(chunkX, chunkZ);
            } catch (Exception ignored) {
                // Last resort - if even the fallback fails, just continue
            }
        }
    }
    
    /**
     * Fast resolution of world name without expensive operations
     */
    private String resolveWorldName() {
        // First try level name from registry
        if (level != null) {
            String name = LEVEL_TO_WORLD_NAME.get(level);
            if (name != null) return name;
            
            // If not in registry, get from level
            name = level.getName();
            if (name != null && !name.isEmpty()) {
                LEVEL_TO_WORLD_NAME.put(level, name);
                return name;
            }
        }
        
        // Then try thread name
        String threadName = Thread.currentThread().getName();
        for (Map.Entry<String, String> entry : ASYNC_THREAD_NAME_PREFIX_MAP.entrySet()) {
            if (threadName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Then try default level from server as last resort
        try {
            if (TerraNukkitPlugin.INSTANCE != null) {
                Level defaultLevel = TerraNukkitPlugin.INSTANCE.getServer().getDefaultLevel();
                if (defaultLevel != null) {
                    return defaultLevel.getName();
                }
            }
        } catch (Exception ignored) {}
        
        return null;
    }
    
    /**
     * Optimized initialization of generator components
     */
    private GeneratorCache initializeGeneratorComponents(String worldName) {
        // Use a synchronized block to prevent multiple threads from initializing the same world
        Object worldLock = WORLD_INIT_LOCKS.computeIfAbsent(worldName, k -> new Object());
        
        synchronized (worldLock) {
            // Double-check cache inside lock
            GeneratorCache globalCache = GENERATOR_CACHE.get(worldName);
            if (globalCache != null) {
                // Another thread initialized it while we were waiting
                return globalCache;
            }
            
            // Fast check for config pack
            if (configPack == null && TerraNukkitPlugin.PLATFORM != null) {
                configPack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                if (configPack == null) {
                    LOGGER.error("No config pack available");
                    return null;
                }
            }
            
            try {
                // Create Terra world
                NukkitWorld terraWorld = new NukkitWorld(level, null, configPack, TerraNukkitPlugin.PLATFORM);
                
                // Create generator and providers
                ChunkGenerator terraGenerator = configPack.getGeneratorProvider().newInstance(configPack);
                BiomeProvider biomeProvider = configPack.getBiomeProvider();
                
                if (terraGenerator == null || biomeProvider == null) {
                    LOGGER.error("Failed to create generator components");
                    return null;
                }
                
                terraWorld.setGenerator(terraGenerator);
                
                // Create and store the generator cache
                GeneratorCache cache = new GeneratorCache(terraGenerator, biomeProvider, terraWorld, worldName);
                
                // Store in global and thread-local caches
                GENERATOR_CACHE.put(worldName, cache);
                THREAD_LOCAL_CACHE.get().put(worldName, cache);
                
                return cache;
            } catch (Exception e) {
                LOGGER.error("Failed to initialize generator components: {}", e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Optimized terrain generation
     */
    private void generateTerrain(int chunkX, int chunkZ, NukkitProtoChunk protoChunk, GeneratorCache cache) {
        try {
            // Add memory optimization: explicitly request garbage collection before a big operation
            // This helps prevent out-of-memory errors during complex terrain generation
            if ((chunkX % 16 == 0 && chunkZ % 16 == 0) || Thread.currentThread().getName().contains("main")) {
                System.gc(); // Request garbage collection to free memory before generating a region
            }

            // Inform Terra's biome provider about our full height range (-64 to 256)
            // This enables deeper terrain generation
            int minHeight = -64;
            int maxHeight = 256;

            // Pre-generate biomes for the whole chunk at once for efficiency
            BiomeProvider biomeProvider = cache.biomeProvider;
            long biomeSeed = random != null ? random.nextLong() : System.currentTimeMillis();
            
            // Batch biome assignment - more efficient implementation
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int worldX = chunkX * 16 + x;
                    int worldZ = chunkZ * 16 + z;
                    
                    // Get biome from Terra's biome provider efficiently
                    com.dfsek.terra.api.world.biome.Biome terraBiome = biomeProvider.getBiome(worldX, 0, worldZ, biomeSeed);
                    
                    // Convert to platform biome and set in chunk
                    if (terraBiome instanceof com.dfsek.terra.api.world.biome.PlatformBiome platformBiome) {
                        protoChunk.setBiome(x, z, platformBiome);
                    }
                }
            }
            
            // Then generate the terrain with optimized ChunkGenerator call
            try {
                // Let Terra know we support negative y-coordinates
                cache.terraWorld.setMinHeight(minHeight);
                cache.terraWorld.setMaxHeight(maxHeight);
                
                // Generate chunk data with full height range
                cache.terraGenerator.generateChunkData(protoChunk, cache.terraWorld, biomeProvider, chunkX, chunkZ);
            } catch (OutOfMemoryError e) {
                // Handle memory issues gracefully - emergency GC and simpler generation
                LOGGER.error("Out of memory during generation of chunk {},{} - using fallback", chunkX, chunkZ);
                System.gc();
                generateFallbackChunk((BaseFullChunk) protoChunk.getHandle());
            }
        } catch (Exception e) {
            LOGGER.error("Error in terrain generation: {}", e.getMessage());
            generateFallbackChunk((BaseFullChunk) protoChunk.getHandle());
        }
    }

    /**
     * Generate a simple fallback chunk if Terra generation fails.
     * Original method that works with chunk coordinates.
     *
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     */
    private void generateFallbackChunk(int chunkX, int chunkZ) {
        try {
            BaseFullChunk chunk = (BaseFullChunk) chunkManager.getChunk(chunkX, chunkZ);
            if (chunk == null) return;
            
            // Delegate to the implementation that takes a chunk directly
            generateFallbackChunk(chunk);
        } catch (Exception e) {
            LOGGER.error("Failed to get chunk for fallback generation: {}", e.getMessage());
        }
    }

    /**
     * Generate a simple fallback chunk directly on the chunk object
     * Much faster than the original method
     *
     * @param chunk The chunk to generate into
     */
    private void generateFallbackChunk(BaseFullChunk chunk) {
        if (chunk == null) return;
        
        try {
            // Set all biomes at once (more efficient)
            byte biomeId = 1; // Plains biome ID is 1 in Nukkit
            
            // Fill biome data (2D)
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBiomeId(x, z, biomeId);
                }
            }
            
            // Fill solid bedrock at y=-64 (virtual, actually stored at y=0)
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlock(x, 0, z, Block.BEDROCK);
                }
            }
            
            // Fill stone from y=1 to y=63 (real blocks)
            for (int y = 1; y < 63; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        chunk.setBlock(x, y, z, Block.STONE);
                    }
                }
            }
            
            // Set grass top layer at y=63
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    chunk.setBlock(x, 63, z, Block.GRASS);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed even during simple fallback generation", e);
        }
    }

    @Override
    public void populateChunk(int chunkX, int chunkZ) {
        // For now, just implement a basic version without using Terra's populate method
        // since we're still developing the Nukkit integration
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
        private ConfigPack pack;  // Changed from final to allow updating if null
        private static final ThreadLocal<Boolean> IN_PACK_CONSTRUCTOR = ThreadLocal.withInitial(() -> Boolean.FALSE);
        
        /**
         * Create a map of options for this generator with the pack ID set
         * 
         * @param packId The pack ID to set
         * @return A map with the pack ID set
         */
        private static Map<String, Object> createOptionsWithPack(String packId) {
            Map<String, Object> options = new HashMap<>();
            options.put(OPTION_PACK, packId);
            options.put(OPTION_PRESET, packId);  // Also set preset for compatibility
            return options;
        }
        
        public PackSpecificGenerator(String packId, ConfigPack pack) {
            super(createOptionsWithPack(packId)); // Initialize with pack ID in options
            
            if (IN_PACK_CONSTRUCTOR.get()) {
                LOGGER.error("Recursive PackSpecificGenerator creation detected! Using null pack to break recursion.");
                this.packId = packId;
                this.pack = null;
                return;
            }
            
            try {
                IN_PACK_CONSTRUCTOR.set(true);
                this.packId = packId;
                this.pack = pack;
                
                LOGGER.debug("Creating PackSpecificGenerator with packId: {}", packId);
                
                // Set the config pack right away
                if (pack != null) {
                    setConfigPack(pack);
                    LOGGER.debug("Created PackSpecificGenerator for pack: {}", packId);
                } else {
                    // Try to get the pack from the platform
                    ConfigPack foundPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                        .getByID(packId)
                                        .orElse(null);
                                        
                    if (foundPack != null) {
                        this.pack = foundPack;
                        setConfigPack(foundPack);
                        LOGGER.debug("Created PackSpecificGenerator for pack (recovered): {}", packId);
                    } else {
                        // Try to use default pack
                        ConfigPack defaultPack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                        if (defaultPack != null) {
                            this.pack = defaultPack;
                            setConfigPack(defaultPack);
                            LOGGER.debug("Created PackSpecificGenerator with default pack: {}", defaultPack.getID());
                        }
                    }
                }
            } finally {
                IN_PACK_CONSTRUCTOR.set(false);
            }
        }
        
        public PackSpecificGenerator(Map<String, Object> options) {
            // Constructor call must be the first statement
            super(options);
            
            if (IN_PACK_CONSTRUCTOR.get()) {
                LOGGER.error("Recursive PackSpecificGenerator creation detected! Using null pack to break recursion.");
                this.packId = "unknown";
                this.pack = null;
                return;
            }
            
            try {
                IN_PACK_CONSTRUCTOR.set(true);
                
                // Get pack ID from options or infer from generator name
                String extractedPackId = null;
                
                // First check if options contain a pack ID
                extractedPackId = getPackIdFromOptions(options);
                
                // If no pack ID found in options, try to extract from generator name if available
                if (extractedPackId == null && options != null && options.containsKey("generator")) {
                    String generatorName = options.get("generator").toString();
                    extractedPackId = extractPackIdFromGeneratorName(generatorName);
                    LOGGER.debug("Extracted pack ID from generator name: {} -> {}", generatorName, extractedPackId);
                }
                
                this.packId = extractedPackId != null ? extractedPackId : "unknown";
                
                // Try to get the pack from the platform only if not in recursive call
                ConfigPack configPack = null;
                if (this.packId != null && !this.packId.isEmpty() && !this.packId.equals("unknown") && 
                    TerraNukkitPlugin.PLATFORM != null) {
                    configPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                .getByID(this.packId)
                                .orElse(null);
                }
                
                this.pack = configPack;
                
                // Set the config pack right away
                if (configPack != null) {
                    setConfigPack(configPack);
                    LOGGER.debug("Created PackSpecificGenerator with pack ID '{}' from options", packId);
                } else {
                    // Try to use default config pack if available
                    if (TerraNukkitPlugin.PLATFORM != null) {
                        ConfigPack defaultPack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                        if (defaultPack != null) {
                            this.pack = defaultPack;
                            LOGGER.debug("Using default config pack as fallback: {}", defaultPack.getID());
                            setConfigPack(defaultPack);
                            // Update packId to match the default pack
                            if (options != null) {
                                options.put(OPTION_PACK, defaultPack.getID());
                            }
                        }
                    }
                }
            } finally {
                IN_PACK_CONSTRUCTOR.set(false);
            }
        }
        
        /**
         * Extract pack ID from generator name
         * 
         * @param generatorName Generator name to extract from
         * @return Pack ID or null if not found
         */
        private static String extractPackIdFromGeneratorName(String generatorName) {
            if (generatorName == null || generatorName.isEmpty()) {
                return null;
            }
            
            // Check for "terra:<packId>" format
            if (generatorName.toLowerCase().startsWith("terra:")) {
                String packId = generatorName.substring("terra:".length());
                if (!packId.isEmpty()) {
                    LOGGER.debug("Extracted pack ID from generator name: {} -> {}", generatorName, packId);
                    return packId;
                }
            }
            
            return null;
        }
        
        @Override
        public void init(ChunkManager chunkManager, NukkitRandom nukkitRandom) {
            // Call super init first
            super.init(chunkManager, nukkitRandom);
            
            // Handle case where pack might be null but we have a packId
            if (pack == null && packId != null && !packId.isEmpty() && !packId.equals("unknown") && 
                TerraNukkitPlugin.PLATFORM != null) {
                // Try to get the pack from the platform
                ConfigPack foundPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                     .getByID(packId)
                                     .orElse(null);
                
                if (foundPack != null) {
                    this.pack = foundPack;
                    setConfigPack(foundPack);
                    LOGGER.debug("Recovered pack during initialization: {}", packId);
                }
            }
            
            // Make sure the pack is set correctly after initialization
            if (pack != null) {
                setConfigPack(pack);
                
                // Update options with pack ID to ensure it's available
                getSettings().put("pack", packId != null ? packId : pack.getID());
                
                // If we have a level, update options with world name and seed too
                Level currentLevel = null;
                if (chunkManager instanceof Level) {
                    currentLevel = (Level) chunkManager;
                    getSettings().put("worldName", currentLevel.getName());
                    getSettings().put("seed", String.valueOf(currentLevel.getSeed()));
                }
            }
        }
        
        @Override
        public ConfigPack getConfigPack() {
            return pack;
        }
        
        @Override
        public String getName() {
            return "terra:" + (packId != null ? packId : "unknown");
        }
    }

    /**
     * Update a generator with a new config pack after reload
     * 
     * @param newPack The new config pack to use
     */
    public void updateConfigPack(ConfigPack newPack) {
        if (newPack == null) {
            LOGGER.warn("Attempted to update generator with null config pack");
            return;
        }
        
        LOGGER.info("Updating generator for world {} with new config pack: {}", 
                  worldName, newPack.getID());
        
        // Update the config pack reference
        this.configPack = newPack;
        
        // Update options
        options.put(OPTION_PACK, newPack.getID());
        
        // Clear cache to force reinitialization with new pack
        if (worldName != null) {
            GENERATOR_CACHE.remove(worldName);
        }
        
        // Reset initialization flag
        initialized = false;
        
        LOGGER.info("Generator updated successfully for world: {}", worldName);
    }
    
    /**
     * Update all active generators with new config packs after a config reload
     */
    public static void updateAllGenerators() {
        LOGGER.info("Updating all active generators with new config packs...");
        
        for (Map.Entry<String, NukkitGenerator> entry : ACTIVE_GENERATORS.entrySet()) {
            String worldName = entry.getKey();
            NukkitGenerator generator = entry.getValue();
            
            if (generator.configPack != null) {
                String packId = generator.configPack.getID();
                
                // Try to get the updated config pack
                ConfigPack updatedPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                       .getByID(packId)
                                       .orElse(null);
                
                if (updatedPack != null) {
                    generator.updateConfigPack(updatedPack);
                }
            }
        }
        
        // Clear all generator caches to force fresh initialization
        GENERATOR_CACHE.clear();
        
        LOGGER.info("Generator update complete.");
    }

    /**
     * Force set the world name for this generator instance.
     * This is useful for asynchronous generation when the level might not be accessible.
     * 
     * @param name The name to set
     */
    public void forceWorldName(String name) {
        if (name != null && !name.isEmpty()) {
            // Set the local field
            this.worldName = name;
            
            // Store for async access via thread-local
            ASYNC_WORLD_NAMES.get().put(Thread.currentThread().getId(), name);
            
            // Add to active generators global registry
            ACTIVE_GENERATORS.put(name, this);
            
            // If we have a level reference, register it in the cross-thread mapping
            if (level != null) {
                LEVEL_TO_WORLD_NAME.put(level, name);
            }
            
            LOGGER.debug("Forced world name set to: {}", name);
        }
    }
    
    /**
     * Get a generator for a specific world from the cache.
     * This is useful for finding existing generators during async operations.
     * 
     * @param worldName The name of the world to get a generator for
     * @return A generator for the given world, or null if none exists
     */
    public static NukkitGenerator getCachedGenerator(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return null;
        }
        
        // Try to get directly from active generators map
        NukkitGenerator generator = ACTIVE_GENERATORS.get(worldName);
        if (generator != null) {
            LOGGER.debug("Found cached generator for world: {}", worldName);
            return generator;
        }
        
        // If not found, try to find one with matching generator components
        GeneratorCache cache = GENERATOR_CACHE.get(worldName);
        if (cache != null) {
            // Try to find a generator that might match this cache
            for (NukkitGenerator gen : ACTIVE_GENERATORS.values()) {
                if (gen.worldName != null && gen.worldName.equals(worldName)) {
                    LOGGER.debug("Found active generator for world: {}", worldName);
                    return gen;
                }
            }
            
            // If no matching generator, try to create one
            try {
                LOGGER.debug("Creating new generator from cache for world: {}", worldName);
                Map<String, Object> options = new HashMap<>();
                options.put("worldName", worldName);
                options.put("pack", cache.terraWorld.getPack().getID());
                
                NukkitGenerator newGen = new NukkitGenerator(options);
                newGen.initialized = true;
                
                // Add to active generators
                ACTIVE_GENERATORS.put(worldName, newGen);
                
                return newGen;
            } catch (Exception e) {
                LOGGER.error("Failed to create generator from cache: {}", e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Reset the recursion counter for the current thread.
     * This should be called when serious errors occur to prevent negative depth
     */
    public static void resetRecursionCounter() {
        RECURSION_COUNTER.get().set(0);
        LOGGER.debug("Reset recursion counter for thread: {}", Thread.currentThread().getName());
    }

    /**
     * Reset the initialization state for the current thread.
     * This can be useful if you need to force reinitialization.
     */
    public static void resetThreadInitState() {
        THREAD_INITIALIZED_WORLDS.get().clear();
        THREAD_LOCAL_CACHE.get().clear();
        INITIALIZING_GENERATOR.set(false);
        IN_CONSTRUCTOR.set(false);
        resetRecursionCounter();
        LOGGER.info("Reset generator initialization state for thread: {}", Thread.currentThread().getName());
    }

    /**
     * Reset all world-specific initialization flags.
     * This is useful when the server is reloading or shutting down.
     */
    public static void resetAllWorldState() {
        WORLD_INITIALIZING_FLAGS.clear();
        WORLD_INITIALIZED.clear();
        WORLD_INIT_LOCKS.clear();
        GENERATOR_CACHE.clear();
        
        // Also clear thread-local states
        THREAD_LOCAL_CACHE.remove();
        THREAD_INITIALIZED_WORLDS.remove();
        ASYNC_WORLD_NAMES.remove();
        
        LOGGER.info("Reset all world initialization state");
        System.out.println("[INFO] Terra has reset all world initialization state");
    }

    /**
     * Reset the initialization state for a specific world on the current thread.
     * 
     * @param worldName The name of the world to reset
     */
    public static void resetWorldInitState(String worldName) {
        if (worldName != null && !worldName.isEmpty()) {
            // First remove from thread-local storage
            THREAD_INITIALIZED_WORLDS.get().remove(worldName);
            THREAD_LOCAL_CACHE.get().remove(worldName);
            
            // Also remove any thread ID-based cache entries
            THREAD_LOCAL_CACHE.get().remove("thread-" + Thread.currentThread().getId());
            
            // Clear initialization flags for this world
            WORLD_INITIALIZING_FLAGS.remove(worldName);
            WORLD_INITIALIZED.remove(worldName);
            WORLD_INIT_LOCKS.remove(worldName);
            
            // Also remove from global cache to force re-initialization
            GENERATOR_CACHE.remove(worldName);
            
            System.out.println("[INFO] Terra has reset generator state for world: " + worldName + 
                             " on thread: " + Thread.currentThread().getName());
        }
    }

    /**
     * Register a world name with its Level instance for cross-thread access.
     * This should be called when a level is loaded.
     * 
     * @param level The Level instance
     */
    public static void registerLevelName(Level level) {
        if (level != null) {
            String worldName = level.getName();
            if (worldName != null && !worldName.isEmpty()) {
                LEVEL_TO_WORLD_NAME.put(level, worldName);
                System.out.println("[INFO] Terra registered world name '" + worldName + "' for cross-thread access");
            }
        }
    }

    /**
     * Register a thread name prefix for a world.
     * This is used to identify async threads for a specific world.
     * 
     * @param worldName The world name
     * @param threadNamePrefix The thread name prefix (e.g., "Nukkit Asynchronous Task Handler")
     */
    public static void registerAsyncThreadPrefix(String worldName, String threadNamePrefix) {
        if (worldName != null && !worldName.isEmpty() && threadNamePrefix != null && !threadNamePrefix.isEmpty()) {
            ASYNC_THREAD_NAME_PREFIX_MAP.put(threadNamePrefix, worldName);
            System.out.println("[INFO] Terra registered async thread prefix '" + threadNamePrefix + 
                             "' for world '" + worldName + "'");
        }
    }
    
    /**
     * Get world name for the current thread based on thread name.
     * 
     * @return The associated world name or null if not found
     */
    public static String getWorldNameFromThreadName() {
        String threadName = Thread.currentThread().getName();
        
        // Check if thread name starts with any registered prefix
        for (Map.Entry<String, String> entry : ASYNC_THREAD_NAME_PREFIX_MAP.entrySet()) {
            if (threadName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Check if the thread name contains any world name clues
        // Common formats:
        // 1. "Level [worldName]"
        // 2. "AsyncTask [worldName]"
        // 3. "WorldGen [worldName]"
        
        // Check for direct level name formats
        if (threadName.startsWith("Level ")) {
            String possibleWorld = threadName.substring("Level ".length()).trim();
            // Validate that this isn't just generic "Level Thread" or similar
            if (!possibleWorld.isEmpty() && !possibleWorld.contains("Thread") && !possibleWorld.contains("Handler")) {
                return possibleWorld;
            }
        }
        
        // If it's an async task handler, see if we have any active worlds registered
        if (threadName.contains("Async") && threadName.contains("Task")) {
            // If only one world is loaded on the server, it's likely to be that one
            if (ACTIVE_GENERATORS.size() == 1) {
                return ACTIVE_GENERATORS.keySet().iterator().next();
            }
            
            // If there's a default world on the server, try that
            if (TerraNukkitPlugin.INSTANCE != null && TerraNukkitPlugin.INSTANCE.getServer() != null) {
                Level defaultLevel = TerraNukkitPlugin.INSTANCE.getServer().getDefaultLevel();
                if (defaultLevel != null) {
                    return defaultLevel.getName();
                }
            }
            
            // Parse async task thread number
            if (threadName.contains("#")) {
                try {
                    // Extract thread number like "Nukkit Asynchronous Task Handler #4"
                    int threadNumber = Integer.parseInt(threadName.substring(threadName.lastIndexOf("#") + 1).trim());
                    
                    // If we have exactly that many worlds loaded, use a simple mapping
                    if (ACTIVE_GENERATORS.size() > threadNumber) {
                        // Map thread number to world in the order they were registered
                        // Not perfect, but better than nothing
                        String[] worldNames = ACTIVE_GENERATORS.keySet().toArray(new String[0]);
                        return worldNames[threadNumber % worldNames.length];
                    }
                } catch (NumberFormatException e) {
                    // Not a number, ignore
                }
            }
        }
        
        return null;
    }
} 