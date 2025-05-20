package com.dfsek.terra.nukkit;

import cn.nukkit.level.biome.EnumBiome;

import com.dfsek.tectonic.api.TypeRegistry;
import com.dfsek.tectonic.api.depth.DepthTracker;
import com.dfsek.tectonic.api.exception.LoadException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import com.dfsek.terra.AbstractPlatform;
import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.handle.ItemHandle;
import com.dfsek.terra.api.handle.WorldHandle;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.world.biome.PlatformBiome;
import com.dfsek.terra.nukkit.block.NukkitMapping;
import com.dfsek.terra.nukkit.handles.NukkitItemHandle;
import com.dfsek.terra.nukkit.handles.NukkitWorldHandle;
import com.dfsek.terra.nukkit.world.biome.NukkitPlatformBiome;

/**
 * Nukkit implementation of Terra's Platform.
 */
public class NukkitPlatform extends AbstractPlatform {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitPlatform.class);
    
    private final TerraNukkitPlugin plugin;
    private final NukkitWorldHandle worldHandle;
    private final NukkitItemHandle itemHandle;
    
    public NukkitPlatform(TerraNukkitPlugin plugin) {
        this.plugin = plugin;
        this.worldHandle = new NukkitWorldHandle();
        this.itemHandle = new NukkitItemHandle();
        
        LOGGER.info("Terra Nukkit Platform initializing...");
        
        // Initialize the block mapping system
        NukkitMapping.init();
        
        // Load the platform
        load();
        
        LOGGER.info("Terra Nukkit Platform initialized successfully.");
    }
    
    public TerraNukkitPlugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean reload() {
        LOGGER.info("Reloading Terra configuration and packs...");
        getTerraConfig().load(this);
        getRawConfigRegistry().clear();
        boolean success = getRawConfigRegistry().loadAll(this);
        
        if (success) {
            LOGGER.info("Terra reload complete.");
        } else {
            LOGGER.error("Terra reload failed. Check previous logs for errors.");
        }
        return success;
    }

    @Override
    public @NotNull String platformName() {
        return "Nukkit";
    }

    @Override
    public void runPossiblyUnsafeTask(@NotNull Runnable runnable) {
        // Run on the main server thread
        plugin.getServer().getScheduler().scheduleTask(plugin, runnable);
    }

    @Override
    protected Iterable<BaseAddon> platformAddon() {
        // Return any Nukkit-specific internal addons if needed
        return Collections.emptyList();
    }

    @Override
    public @NotNull WorldHandle getWorldHandle() {
        return worldHandle;
    }

    @Override
    public @NotNull File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public @NotNull ItemHandle getItemHandle() {
        return itemHandle;
    }

    @Override
    public int getGenerationThreads() {
        // Start with a reasonable default for a Nukkit server
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(4, cores / 2));
    }

    @Override
    public void register(TypeRegistry registry) {
        super.register(registry);
        LOGGER.info("Registering Nukkit-specific types with Tectonic...");
        try {
            // Register loader for BlockState using the WorldHandle
            registry.registerLoader(BlockState.class, (type, o, loader, depthTracker) -> 
                worldHandle.createBlockState((String) o));

            // Register loader for PlatformBiome using Nukkit's biome system
            registry.registerLoader(PlatformBiome.class, (type, o, loader, depthTracker) -> 
                parseNukkitBiome((String) o, depthTracker));

            LOGGER.info("Nukkit type registration complete.");
        } catch (Exception e) {
            LOGGER.error("Failed to register Nukkit types with Tectonic", e);
            throw new RuntimeException("Tectonic type registration failed for Nukkit", e);
        }
    }

    /**
     * Parses a biome identifier string (e.g., "minecraft:plains") into a NukkitPlatformBiome.
     */
    private NukkitPlatformBiome parseNukkitBiome(String id, DepthTracker depthTracker) throws LoadException {
        String biomeName = id;
        if (id.startsWith("minecraft:")) {
            biomeName = id.substring(10); // Remove "minecraft:" prefix
        }

        try {
            // First try direct mapping
            try {
                EnumBiome nukkitBiome = EnumBiome.valueOf(biomeName.toUpperCase(Locale.ROOT));
                return new NukkitPlatformBiome(nukkitBiome);
            } catch (IllegalArgumentException e) {
                // If direct mapping fails, try fallback mapping
                EnumBiome fallbackBiome = getFallbackBiome(biomeName);
                if (fallbackBiome != null) {
                    LOGGER.info("Mapped modern biome '{}' to Nukkit biome '{}'", biomeName, fallbackBiome.name());
                    return new NukkitPlatformBiome(fallbackBiome);
                }
                throw new LoadException("Invalid Nukkit biome identifier: '" + id + "'. Consider adding a mapping for this biome.", depthTracker);
            }
        } catch (Exception e) {
            if (e instanceof LoadException) throw e;
            throw new LoadException("Error mapping biome: '" + id + "': " + e.getMessage(), depthTracker);
        }
    }
    
    /**
     * Get a fallback biome for modern Minecraft biomes that don't exist in Nukkit.
     * 
     * @param biomeName The modern biome name (without minecraft: prefix)
     * @return A suitable Nukkit biome equivalent, or null if no mapping exists
     */
    private EnumBiome getFallbackBiome(String biomeName) {
        // Map modern biomes to their closest Nukkit equivalents
        switch (biomeName.toLowerCase()) {
            // Forest biomes
            case "windswept_forest": return EnumBiome.EXTREME_HILLS_PLUS;
            case "flower_forest": return EnumBiome.FLOWER_FOREST;
            case "dark_forest": return EnumBiome.ROOFED_FOREST;
            case "old_growth_birch_forest": return EnumBiome.BIRCH_FOREST_M;
            case "old_growth_pine_taiga": return EnumBiome.MEGA_TAIGA;
            case "old_growth_spruce_taiga": return EnumBiome.MEGA_SPRUCE_TAIGA;
            
            // Snowy biomes
            case "snowy_plains": return EnumBiome.ICE_PLAINS;
            case "snowy_taiga": return EnumBiome.COLD_TAIGA;
            case "snowy_beach": return EnumBiome.COLD_BEACH;
            case "snowy_slopes": return EnumBiome.ICE_MOUNTAINS;
            case "grove": return EnumBiome.COLD_TAIGA;
            case "frozen_peaks": return EnumBiome.ICE_MOUNTAINS;
            case "jagged_peaks": return EnumBiome.EXTREME_HILLS_M;
            case "frozen_river": return EnumBiome.FROZEN_RIVER;
            case "ice_spikes": return EnumBiome.ICE_PLAINS_SPIKES;
            
            // Desert/savanna biomes
            case "desert": return EnumBiome.DESERT;
            case "badlands": return EnumBiome.MESA;
            case "eroded_badlands": return EnumBiome.MESA_BRYCE;
            case "wooded_badlands": return EnumBiome.MESA_PLATEAU_F;
            
            // Mountain biomes
            case "stony_peaks": return EnumBiome.STONE_BEACH;
            case "stony_shore": return EnumBiome.STONE_BEACH;
            case "windswept_hills": return EnumBiome.EXTREME_HILLS;
            case "windswept_gravelly_hills": return EnumBiome.EXTREME_HILLS_M;
            case "windswept_savanna": return EnumBiome.SAVANNA_M;
            case "meadow": return EnumBiome.FLOWER_FOREST;
            
            // Jungle biomes
            case "sparse_jungle": return EnumBiome.JUNGLE_EDGE;
            case "bamboo_jungle": return EnumBiome.BAMBOO_JUNGLE;
            
            // Swamp biomes
            case "swamp": return EnumBiome.SWAMP;
            case "mangrove_swamp": return EnumBiome.SWAMP;
            
            // Ocean biomes
            case "warm_ocean": return EnumBiome.WARM_OCEAN;
            case "lukewarm_ocean": return EnumBiome.LUKEWARM_OCEAN;
            case "deep_lukewarm_ocean": return EnumBiome.DEEP_LUKEWARM_OCEAN;
            case "deep_ocean": return EnumBiome.DEEP_OCEAN;
            case "deep_frozen_ocean": return EnumBiome.DEEP_FROZEN_OCEAN;
            case "cold_ocean": return EnumBiome.COLD_OCEAN;
            case "deep_cold_ocean": return EnumBiome.DEEP_COLD_OCEAN;
            case "frozen_ocean": return EnumBiome.FROZEN_OCEAN;
            
            // River/beach biomes
            case "river": return EnumBiome.RIVER;
            case "beach": return EnumBiome.BEACH;
            
            // Mushroom biomes
            case "mushroom_fields": return EnumBiome.MUSHROOM_ISLAND;
            
            // Nether biomes
            case "nether_wastes": return EnumBiome.HELL;
            case "soul_sand_valley": return EnumBiome.SOULSAND_VALLEY;
            case "crimson_forest": return EnumBiome.CRIMSON_FOREST;
            case "warped_forest": return EnumBiome.WARPED_FOREST;
            case "basalt_deltas": return EnumBiome.BASALT_DELTAS;
            
            // End biomes
            case "the_end": return EnumBiome.END;
            case "end_highlands": return EnumBiome.END;
            case "end_midlands": return EnumBiome.END;
            case "small_end_islands": return EnumBiome.END;
            case "end_barrens": return EnumBiome.END;
            
            // Other biomes
            case "dripstone_caves": return EnumBiome.EXTREME_HILLS;
            case "lush_caves": return EnumBiome.JUNGLE;
            case "deep_dark": return EnumBiome.ROOFED_FOREST;
            case "cherry_grove": return EnumBiome.FLOWER_FOREST;
            
            default: return null;
        }
    }

    /**
     * Get the currently active config pack.
     *
     * @return Current active ConfigPack or null if none is set
     */
    public ConfigPack getActiveConfig() {
        try {
            // Try to get the default pack first
            Optional<ConfigPack> defaultPack = getConfigRegistry().getByID("default");
            if (defaultPack.isPresent()) {
                return defaultPack.get();
            }
            
            // If no default pack, get the first available pack
            for (ConfigPack pack : getConfigRegistry().entries()) {
                return pack; // Return the first one
            }
        } catch (Exception e) {
            LOGGER.error("Error getting active config pack", e);
        }
        
        return null;
    }
} 