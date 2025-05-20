package com.dfsek.terra.nukkit.world.chunk;

import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.biome.EnumBiome;
import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.Entity;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.world.biome.Biome;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.world.WritableWorld;
import com.dfsek.terra.api.world.ReadableWorld;
import com.dfsek.terra.api.world.World;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.chunk.ChunkAccess;
import com.dfsek.terra.api.world.chunk.generation.ProtoChunk;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.world.NukkitWorld;
import com.dfsek.terra.nukkit.block.NukkitBlockState;
import com.dfsek.terra.nukkit.world.biome.NukkitPlatformBiome;
import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Efficient implementation of ProtoChunk/ProtoWorld for Nukkit chunk generation.
 */
public class NukkitProtoChunk implements ProtoWorld, ProtoChunk {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitProtoChunk.class);
    
    private final FullChunk delegate;
    private final NukkitWorld world;
    
    // Cache frequently used blocks for better performance
    private static final Map<String, cn.nukkit.block.Block> BLOCK_CACHE = new HashMap<>();
    
    // Pre-initialize common blocks to avoid repeated lookups
    static {
        initBlockCache();
    }
    
    private static void initBlockCache() {
        try {
            BLOCK_CACHE.put("stone", cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE));
            BLOCK_CACHE.put("dirt", cn.nukkit.block.Block.get(cn.nukkit.block.Block.DIRT));
            BLOCK_CACHE.put("grass_block", cn.nukkit.block.Block.get(cn.nukkit.block.Block.GRASS));
            BLOCK_CACHE.put("bedrock", cn.nukkit.block.Block.get(cn.nukkit.block.Block.BEDROCK));
            BLOCK_CACHE.put("air", cn.nukkit.block.Block.get(cn.nukkit.block.Block.AIR));
            BLOCK_CACHE.put("water", cn.nukkit.block.Block.get(cn.nukkit.block.Block.WATER));
            BLOCK_CACHE.put("sand", cn.nukkit.block.Block.get(cn.nukkit.block.Block.SAND));
            BLOCK_CACHE.put("gravel", cn.nukkit.block.Block.get(cn.nukkit.block.Block.GRAVEL));
            BLOCK_CACHE.put("coal_ore", cn.nukkit.block.Block.get(cn.nukkit.block.Block.COAL_ORE));
            BLOCK_CACHE.put("iron_ore", cn.nukkit.block.Block.get(cn.nukkit.block.Block.IRON_ORE));
            BLOCK_CACHE.put("gold_ore", cn.nukkit.block.Block.get(cn.nukkit.block.Block.GOLD_ORE));
            BLOCK_CACHE.put("diamond_ore", cn.nukkit.block.Block.get(cn.nukkit.block.Block.DIAMOND_ORE));
            BLOCK_CACHE.put("oak_log", cn.nukkit.block.Block.get(cn.nukkit.block.Block.LOG));
            BLOCK_CACHE.put("oak_leaves", cn.nukkit.block.Block.get(cn.nukkit.block.Block.LEAVES));
            BLOCK_CACHE.put("sandstone", cn.nukkit.block.Block.get(cn.nukkit.block.Block.SANDSTONE));
            BLOCK_CACHE.put("redstone_ore", cn.nukkit.block.Block.get(cn.nukkit.block.Block.REDSTONE_ORE));
            BLOCK_CACHE.put("obsidian", cn.nukkit.block.Block.get(cn.nukkit.block.Block.OBSIDIAN));
            BLOCK_CACHE.put("snow", cn.nukkit.block.Block.get(cn.nukkit.block.Block.SNOW_LAYER));
            BLOCK_CACHE.put("clay", cn.nukkit.block.Block.get(cn.nukkit.block.Block.CLAY_BLOCK));
        } catch (Exception e) {
            LOGGER.error("Error initializing block cache", e);
        }
    }

    public NukkitProtoChunk(FullChunk delegate, World world) {
        this.delegate = delegate;
        if (!(world instanceof NukkitWorld nukkitWorld)) {
            throw new IllegalArgumentException("NukkitProtoChunk requires a NukkitWorld instance");
        }
        this.world = nukkitWorld;
    }
    
    /**
     * Alternative constructor that accepts any FullChunk and tries to resolve the world itself.
     * This is useful for asynchronous generation where the world reference might be lost.
     *
     * @param delegate The FullChunk to wrap
     */
    public NukkitProtoChunk(FullChunk delegate) {
        this.delegate = delegate;
        
        NukkitWorld resolvedWorld = null;
        String worldName = null;
        
        // Try to get the world from the chunk
        try {
            if (delegate instanceof BaseFullChunk baseChunk && baseChunk.getProvider() != null && 
                baseChunk.getProvider().getLevel() != null) {
                
                worldName = baseChunk.getProvider().getLevel().getName();
                
                // Try to find a cached generator by world name
                resolvedWorld = findWorldByName(worldName);
                
                if (resolvedWorld == null) {
                    LOGGER.debug("Creating temporary NukkitWorld for chunk in world: {}", worldName);
                    // Create a temporary world if we can't find a cached one
                    resolvedWorld = new NukkitWorld(baseChunk.getProvider().getLevel(), null, 
                                             TerraNukkitPlugin.PLATFORM.getActiveConfig(), 
                                             TerraNukkitPlugin.PLATFORM);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve world from chunk: {}", e.getMessage());
        }
        
        // If we couldn't resolve from the chunk directly, try other methods
        if (resolvedWorld == null) {
            // Try to determine world from thread name or thread-local storage
            worldName = getWorldNameFromThread();
            
            if (worldName != null) {
                LOGGER.debug("Found world name from thread: {}", worldName);
                // Try to get level from server
                try {
                    cn.nukkit.level.Level level = TerraNukkitPlugin.INSTANCE.getServer().getLevelByName(worldName);
                    if (level != null) {
                        resolvedWorld = new NukkitWorld(level, null,
                                               TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                                   .getByID(TerraNukkitPlugin.INSTANCE.findPackIdForWorld(worldName))
                                                   .orElse(TerraNukkitPlugin.PLATFORM.getActiveConfig()),
                                               TerraNukkitPlugin.PLATFORM);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Error creating world from thread name: {}", e.getMessage());
                }
            }
            
            // If still no world, try the default level
            if (resolvedWorld == null) {
                try {
                    cn.nukkit.level.Level defaultLevel = TerraNukkitPlugin.INSTANCE.getServer().getDefaultLevel();
                    if (defaultLevel != null) {
                        worldName = defaultLevel.getName();
                        resolvedWorld = new NukkitWorld(defaultLevel, null,
                                               TerraNukkitPlugin.PLATFORM.getActiveConfig(),
                                               TerraNukkitPlugin.PLATFORM);
                        LOGGER.debug("Using default level for chunk: {}", worldName);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Error getting default level: {}", e.getMessage());
                }
            }
        }
        
        // If we still couldn't resolve the world, create a simple temporary one
        if (resolvedWorld == null) {
            LOGGER.warn("Creating dummy world for chunk since no world could be resolved");
            resolvedWorld = new NukkitWorld(null, null, 
                                   TerraNukkitPlugin.PLATFORM.getActiveConfig(),
                                   TerraNukkitPlugin.PLATFORM);
        }
        
        this.world = resolvedWorld;
    }
    
    /**
     * Try to find a NukkitWorld instance by name from existing generators
     *
     * @param worldName The world name to look for
     * @return A NukkitWorld instance if found, null otherwise
     */
    private NukkitWorld findWorldByName(String worldName) {
        // This is a best-effort method to find a world by name
        try {
            for (cn.nukkit.level.Level level : TerraNukkitPlugin.INSTANCE.getServer().getLevels().values()) {
                if (level.getName().equals(worldName)) {
                    return new NukkitWorld(level, null, 
                                     TerraNukkitPlugin.PLATFORM.getActiveConfig(),
                                     TerraNukkitPlugin.PLATFORM);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error looking up world by name: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get world name from thread information
     *
     * @return The world name, or null if not found
     */
    private String getWorldNameFromThread() {
        String threadName = Thread.currentThread().getName();
        
        // Check if we can find a world name from the thread using NukkitGenerator's registry
        try {
            // This class is used by our public API for looking up thread-based world names
            Class<?> generatorClass = Class.forName("com.dfsek.terra.nukkit.generator.NukkitGenerator");
            java.lang.reflect.Method method = generatorClass.getDeclaredMethod("getWorldNameFromThreadName");
            method.setAccessible(true);
            Object result = method.invoke(null);
            if (result instanceof String worldName && worldName != null && !worldName.isEmpty()) {
                return worldName;
            }
        } catch (Exception e) {
            // Failed to access method, try other ways to determine
            LOGGER.debug("Failed to get world name from thread via reflection: {}", e.getMessage());
        }
        
        // Try to extract from thread name directly
        // Common formats like "Level [worldName]" or "AsyncTask [worldName]"
        if (threadName.startsWith("Level ")) {
            String possibleWorld = threadName.substring("Level ".length()).trim();
            if (!possibleWorld.isEmpty() && !possibleWorld.contains("Thread") && !possibleWorld.contains("Handler")) {
                return possibleWorld;
            }
        }
        
        // Try checking thread-local storage in NukkitGenerator if we can access it
        // This is a bit of a hack, but necessary to access thread-local information
        try {
            Class<?> generatorClass = Class.forName("com.dfsek.terra.nukkit.generator.NukkitGenerator");
            java.lang.reflect.Field field = generatorClass.getDeclaredField("ASYNC_WORLD_NAMES");
            field.setAccessible(true);
            Object threadLocal = field.get(null);
            if (threadLocal != null) {
                java.lang.reflect.Method getMethod = threadLocal.getClass().getMethod("get");
                getMethod.setAccessible(true);
                Object map = getMethod.invoke(threadLocal);
                if (map instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<Long, String> threadNames = (Map<Long, String>) map;
                    String worldName = threadNames.get(Thread.currentThread().getId());
                    if (worldName != null && !worldName.isEmpty()) {
                        return worldName;
                    }
                }
            }
        } catch (Exception e) {
            // Failed to access thread-local, ignore
            LOGGER.debug("Failed to access thread-local storage: {}", e.getMessage());
        }
        
        // If we're on an async handler and there's only one active world, use it
        if (threadName.contains("Async") && threadName.contains("Task")) {
            try {
                java.util.Map<Integer, cn.nukkit.level.Level> levels = TerraNukkitPlugin.INSTANCE.getServer().getLevels();
                if (levels.size() == 1) {
                    return levels.values().iterator().next().getName();
                }
            } catch (Exception e) {
                // Failed to get levels, ignore
            }
        }
        
        return null;
    }

    @Override
    public int centerChunkX() {
        return delegate.getX();
    }

    @Override
    public int centerChunkZ() {
        return delegate.getZ();
    }

    @Override
    public ServerWorld getWorld() {
        return (ServerWorld) this.world;
    }

    @Override
    public int getMaxHeight() {
        return world.getMaxHeight();
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull BlockState blockState) {
        try {
            if (y < getMinHeight() || y >= getMaxHeight()) return;
            
            // Log unmapped blocks BEFORE we apply any replacements
            String blockString = blockState.getAsString();
            
            // Handle all y-coordinates - no special handling for negative y
            // Instead, we'll remap them to the positive range in our internal storage
            
            // Fast path for NukkitBlockState
            if (blockState instanceof NukkitBlockState nukkitState) {
                cn.nukkit.block.Block nukkitBlock = nukkitState.getNukkitBlock();
                // Store negative coords shifted into the positive range (e.g., -1 → 0, -2 → 0, etc.)
                // Place bedrock at actual minimum height, not hardcoded to y=0
                if (y == getMinHeight() && nukkitBlock.getId() == cn.nukkit.block.Block.BEDROCK) {
                    int minY = Math.max(0, getMinHeight()); // Use 0 if getMinHeight() returns negative values
                    delegate.setBlock(x, minY, z, nukkitBlock.getId(), nukkitBlock.getDamage());
                    return;
                } else if (y < 0) {
                    // Skip placing blocks in negative y-space for now
                    // This allows Terra to generate without trying to place blocks below y=0
                    return;
                }
                // Normal placement for positive y values
                delegate.setBlock(x, y, z, nukkitBlock.getId(), nukkitBlock.getDamage());
                return;
            }
            
            // Standard handling for all other blocks
            // Extract block name
            if (blockString.equals("minecraft:air") || blockString.equals("air")) {
                if (y >= 0) {
                    delegate.setBlock(x, y, z, cn.nukkit.block.Block.AIR, 0);
                }
                return;
            }

            String blockName = blockString;
            
            // Extract just the block name without properties more efficiently
            int bracketIndex = blockString.indexOf('[');
            if (bracketIndex > 0) {
                blockName = blockString.substring(0, bracketIndex);
            }
            
            // Remove minecraft: prefix if present
            if (blockName.startsWith("minecraft:")) {
                blockName = blockName.substring(10);
            }
            
            // Place bedrock ONLY at the absolute bottom level
            if (y == getMinHeight() && blockName.equals("bedrock")) {
                int minY = Math.max(0, getMinHeight()); // Use 0 if getMinHeight() returns negative values
                delegate.setBlock(x, minY, z, cn.nukkit.block.Block.BEDROCK, 0);
                return;
            }
            
            // Skip block placement for negative y-coordinates
            if (y < 0) {
                return;
            }
            
            // Try to get from cache first for common blocks
            cn.nukkit.block.Block nukkitBlock = BLOCK_CACHE.get(blockName);
            if (nukkitBlock != null) {
                delegate.setBlock(x, y, z, nukkitBlock.getId(), nukkitBlock.getDamage());
                return;
            }
            
            // If not in cache, try to map the block
            nukkitBlock = mapToNukkitBlock(blockName);
            if (nukkitBlock != null) {
                // Add to cache for future lookups
                if (BLOCK_CACHE.size() < 1000) { // Limit cache size
                    BLOCK_CACHE.put(blockName, nukkitBlock);
                }
                delegate.setBlock(x, y, z, nukkitBlock.getId(), nukkitBlock.getDamage());
            } else {
                // Default to diamond block if mapping fails - makes unmapped blocks easily visible
                System.out.println("[Terra] Unmapped block detected: " + blockName + " at position " + x + "," + y + "," + z);
                delegate.setBlock(x, y, z, cn.nukkit.block.Block.DIAMOND_BLOCK, 0);
            }
        } catch (Exception e) {
            // Use diamond block as fallback for errors too
            System.out.println("[Terra] Error setting block: " + e.getMessage() + " at " + x + "," + y + "," + z);
            if (y >= 0 && y < getMaxHeight()) {
                delegate.setBlock(x, y, z, cn.nukkit.block.Block.DIAMOND_BLOCK, 0);
            }
        }
    }
    
    /**
     * Map a block name to a Nukkit block
     * 
     * @param blockName The block name (without minecraft: prefix)
     * @return The mapped Nukkit block, or null if no mapping exists
     */
    private cn.nukkit.block.Block mapToNukkitBlock(String blockName) {
        // Common blocks mapping
        switch (blockName.toLowerCase()) {
            case "stone": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE);
            case "grass_block": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.GRASS);
            case "dirt": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.DIRT);
            case "cobblestone": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.COBBLESTONE);
            case "oak_planks": case "planks": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.PLANKS);
            case "bedrock": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.BEDROCK);
            case "sand": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.SAND);
            case "gravel": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.GRAVEL);
            case "gold_ore": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.GOLD_ORE);
            case "iron_ore": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.IRON_ORE);
            case "coal_ore": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.COAL_ORE);
            case "oak_log": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.LOG);
            case "oak_leaves": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.LEAVES);
            case "grass": case "tall_grass": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.TALL_GRASS);
            case "water": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.WATER);
            case "lava": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.LAVA);
            
            // Additional blocks that might be common in terrain generation
            case "andesite": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE, 5);
            case "granite": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE, 1);
            case "diorite": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE, 3);
            case "sandstone": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.SANDSTONE);
            case "red_sand": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.SAND, 1);
            
            // Additional mappings for common blocks
            case "deepslate": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE, 3); // Map to diorite
            case "tuff": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE, 5); // Map to andesite
            case "calcite": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.QUARTZ_BLOCK);
            case "smooth_basalt": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE, 6);
            case "amethyst_block": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.PURPUR_BLOCK);
            case "copper_ore": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.GOLD_ORE);
            case "raw_copper_block": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.GOLD_BLOCK);
            case "powder_snow": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.SNOW_BLOCK);

            default:
                // Print debug info about unmapped blocks to console
                System.out.println("[Terra] Unmapped block: " + blockName + " - replacing with DIAMOND_BLOCK");
                return null;
        }
    }

    @Override
    @NotNull
    public BlockState getBlock(int x, int y, int z) {
        if (y >= getMinHeight() && y < getMaxHeight()) {
            cn.nukkit.block.Block nukkitBlock = cn.nukkit.block.Block.get(delegate.getBlockId(x, y, z), delegate.getBlockData(x, y, z));
            return TerraNukkitPlugin.PLATFORM.getWorldHandle().createBlockState(nukkitBlock.getName());
        } else {
            return TerraNukkitPlugin.PLATFORM.getWorldHandle().air();
        }
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        setBlock(x, y, z, data);
    }

    @Override
    public Entity spawnEntity(double x, double y, double z, EntityType entityType) {
        return world.spawnEntity(x, y, z, entityType);
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        return getBlock(x, y, z);
    }

    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        return world.getBlockEntity(x, y, z);
    }

    @Override
    public ChunkGenerator getGenerator() {
        return world.getGenerator();
    }

    @Override
    public BiomeProvider getBiomeProvider() {
        return world.getBiomeProvider();
    }

    @Override
    public ConfigPack getPack() {
        return world.getPack();
    }

    @Override
    public long getSeed() {
        return world.getSeed();
    }

    @Override
    public int getMinHeight() {
        return world.getMinHeight();
    }

    @Override
    public Object getHandle() {
        return delegate;
    }

    /**
     * Set the biome at a specific column in this chunk
     * 
     * @param x X coordinate (0-15)
     * @param z Z coordinate (0-15)
     * @param biome The biome to set
     */
    public void setBiome(int x, int z, com.dfsek.terra.api.world.biome.PlatformBiome biome) {
        if (x < 0 || x >= 16 || z < 0 || z >= 16) return;
        
        // Fast path for NukkitPlatformBiome
        if (biome instanceof NukkitPlatformBiome nukkitPlatformBiome) {
            EnumBiome enumBiome = nukkitPlatformBiome.getHandle();
            delegate.setBiomeId(x, z, enumBiome.id);
            return;
        }
        
        // Try to get the Nukkit biome ID from handle
        Object handle = biome.getHandle();
        int biomeId = 1; // Default to plains (biome ID 1)
        
        if (handle instanceof Integer id) {
            biomeId = id;
        } else if (handle instanceof EnumBiome enumBiome) {
            biomeId = enumBiome.id;
        }
        
        delegate.setBiomeId(x, z, biomeId);
    }
} 