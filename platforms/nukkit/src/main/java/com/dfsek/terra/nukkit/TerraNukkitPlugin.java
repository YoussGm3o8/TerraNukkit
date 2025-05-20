package com.dfsek.terra.nukkit;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.level.generator.Generator;
import com.dfsek.terra.api.event.events.platform.PlatformInitializationEvent;
import com.dfsek.terra.api.event.events.platform.CommandRegistrationEvent;
import com.dfsek.terra.api.event.functional.FunctionalEventHandler;
import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.inject.annotations.Inject;
import com.dfsek.terra.api.registry.Registry;
import com.dfsek.terra.nukkit.commands.NukkitCommands;
import com.dfsek.terra.nukkit.generator.NukkitGenerator;
import com.dfsek.terra.nukkit.listeners.NukkitListener;
import ca.solostudios.strata.version.Version;
import ca.solostudios.strata.Versions;
import com.dfsek.terra.nukkit.block.NukkitMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import cn.nukkit.level.Level;
import java.util.concurrent.ConcurrentHashMap;

public class TerraNukkitPlugin extends PluginBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerraNukkitPlugin.class);
    public static TerraNukkitPlugin INSTANCE;
    public static NukkitPlatform PLATFORM;
    
    // Cache for faster lookup of pack-specific generators
    private final Map<String, Class<? extends NukkitGenerator>> registeredPacks = new ConcurrentHashMap<>();
    
    // Cache for looking up packs by world name
    private final Map<String, String> worldToPackCache = new ConcurrentHashMap<>();
    
    // Track registered generator names to avoid duplicates
    private final Set<String> registeredGeneratorNames = new HashSet<>();

    private final BaseAddon addon = new BaseAddon() {
        @Override
        public String getID() {
            return "terra-nukkit-platform"; // Simple ID
        }
        
        @Override
        public Version getVersion() {
             // <<< Simplify version handling >>>
             // Return fixed version for now, parsing seems problematic
             return Versions.getVersion(1, 0, 0); 
        }
    };

    @Override
    public void onLoad() {
        INSTANCE = this;
        getLogger().info("Loading Terra for Nukkit...");

        // Initialize block mappings first
        getLogger().info("Initializing block mappings...");
        NukkitMapping.init();
        
        // Initialize the platform so we can access config packs
        getLogger().info("Initializing Nukkit platform...");
        PLATFORM = new NukkitPlatform(this);
        
        // Don't register any generators until the plugin is fully enabled
        // This ensures all config packs are loaded first
        getLogger().info("Terra Nukkit Plugin Loaded. Will register generators during onEnable.");
    }

    @Override
    public void onEnable() {
        getLogger().info("Enabling Terra Nukkit Plugin v" + getDescription().getVersion() + "...");

        // Register event listeners
        getLogger().info("Registering event listeners...");
        getServer().getPluginManager().registerEvents(new NukkitListener(PLATFORM), this);
        
        // Register commands
        getLogger().info("Registering commands...");
        NukkitCommands commands = new NukkitCommands(PLATFORM);
        commands.register();
        
        // Fire the platform initialization event
        getLogger().info("Firing initialization event...");
        PLATFORM.getEventManager().callEvent(new PlatformInitializationEvent());
        
        // Register all generators (main Terra generator and pack-specific ones)
        getLogger().info("Registering Terra generators...");
        registerGenerators();
        
        // Set up existing worlds with Terra generator
        setupExistingWorlds();
        
        getLogger().info("Terra Nukkit Plugin Enabled.");
    }
    
    /**
     * Register all Terra generators with Nukkit
     */
    private void registerGenerators() {
        // Clear all previously registered generator names
        registeredGeneratorNames.clear();
        
        // Register the generic Terra generator type first as a fallback
        try {
            if (Generator.addGenerator(NukkitGenerator.class, "terra", Generator.TYPE_INFINITE)) {
                registeredGeneratorNames.add("terra");
                getLogger().info("Registered generic Terra generator type.");
            } else {
                getLogger().warning("Failed to register generic Terra generator - may already be registered.");
            }
        } catch (Exception e) {
            getLogger().error("Error registering generic Terra generator: " + e.getMessage());
        }
        
        // Register pack-specific generators
        registerPackGenerators();
    }
    
    /**
     * Set up existing worlds with Terra generator
     */
    private void setupExistingWorlds() {
        getLogger().info("Setting up existing worlds with Terra generator...");
        
        for (Level level : getServer().getLevels().values()) {
            String worldName = level.getName();
            Generator generator = level.getGenerator();
            String generatorName = generator != null ? generator.getName() : "unknown";
            
            // First register this level's name for cross-thread access
            NukkitGenerator.registerLevelName(level);
            
            // Register standard async thread prefixes for this world
            registerAsyncThreadPrefixesForWorld(worldName);
            
            getLogger().debug("World: " + worldName + " using generator: " + generatorName);
            
            // Check if this world is using a Terra generator
            if (generatorName.equals("terra") || generatorName.startsWith("terra:")) {
                // Extract the pack ID if it's a specific Terra generator
                String packId = null;
                if (generatorName.startsWith("terra:")) {
                    packId = generatorName.substring("terra:".length());
                    
                    // Cache the mapping for faster lookups
                    worldToPackCache.put(worldName, packId);
                } else {
                    // Try to match the world name to a pack
                    packId = findPackIdForWorld(worldName);
                    
                    // If found, cache this mapping
                    if (packId != null) {
                        worldToPackCache.put(worldName, packId);
                    } else if (PLATFORM.getActiveConfig() != null) {
                        // Use default config pack
                        packId = PLATFORM.getActiveConfig().getID();
                        worldToPackCache.put(worldName, packId);
                    }
                }
                
                if (packId != null) {
                    ConfigPack pack = PLATFORM.getConfigRegistry().getByID(packId).orElse(null);
                    if (pack != null && generator instanceof NukkitGenerator) {
                        // Set the config pack directly
                        ((NukkitGenerator)generator).setConfigPack(pack);
                        getLogger().info("Assigned pack '" + packId + "' to existing world: " + worldName);
                    }
                }
            } else if (worldName.equalsIgnoreCase("origen") || worldName.equalsIgnoreCase("origin")) {
                // Special case for worlds named origen/origin - try to assign the ORIGEN pack
                ConfigPack pack = PLATFORM.getConfigRegistry().getByID("ORIGEN").orElse(null);
                if (pack != null) {
                    // Cache this mapping
                    worldToPackCache.put(worldName, "ORIGEN");
                    
                    if (generator instanceof NukkitGenerator) {
                        ((NukkitGenerator)generator).setConfigPack(pack);
                        getLogger().info("Assigned ORIGEN pack to world: " + worldName);
                    }
                }
            }
        }
    }
    
    /**
     * Register standard async thread prefixes for a world.
     * This helps async chunk generation identify which world it's working with.
     */
    public void registerAsyncThreadPrefixesForWorld(String worldName) {
        if (worldName == null || worldName.isEmpty()) return;
        
        // Register common Nukkit thread name prefixes
        NukkitGenerator.registerAsyncThreadPrefix(worldName, "Nukkit Asynchronous Task Handler");
        
        // Register more specific prefixes for this world
        NukkitGenerator.registerAsyncThreadPrefix(worldName, "Chunk Generation Thread");
        NukkitGenerator.registerAsyncThreadPrefix(worldName, "World Generation");
        NukkitGenerator.registerAsyncThreadPrefix(worldName, "Level " + worldName);
        
        // Register async task handler threads with numbers
        for (int i = 0; i < 10; i++) {  // Register some reasonable number of thread handlers
            NukkitGenerator.registerAsyncThreadPrefix(worldName, "Nukkit Asynchronous Task Handler #" + i);
        }
    }
    
    /**
     * Find a pack ID that best matches a world name
     * 
     * @param worldName The name of the world
     * @return The ID of the matching pack, or null if no match found
     */
    public String findPackIdForWorld(String worldName) {
        // Check cache first for better performance
        if (worldToPackCache.containsKey(worldName)) {
            return worldToPackCache.get(worldName);
        }
        
        // Remove non-alphanumeric characters and convert to lowercase for comparison
        String normalizedWorldName = worldName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        
        // If the registry is empty, we can't match
        Registry<ConfigPack> registry = PLATFORM.getConfigRegistry();
        if (registry == null) return null;
        
        String foundPackId = null;
        
        // First, try exact matches
        for (ConfigPack pack : registry.entries()) {
            String packId = pack.getID();
            String normalizedPackId = packId.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            
            // Check for exact match
            if (normalizedWorldName.equals(normalizedPackId)) {
                foundPackId = packId;
                break;
            }
        }
        
        // If no exact match, try case-insensitive partial matches
        if (foundPackId == null) {
            for (ConfigPack pack : registry.entries()) {
                String packId = pack.getID();
                String normalizedPackId = packId.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                
                // Check if world name contains pack ID or vice versa
                if (normalizedWorldName.contains(normalizedPackId) || normalizedPackId.contains(normalizedWorldName)) {
                    foundPackId = packId;
                    break;
                }
            }
        }
        
        // Cache the result for future lookups, even if null
        if (foundPackId != null) {
            worldToPackCache.put(worldName, foundPackId);
            getLogger().info("Found pack match: " + foundPackId + " for world: " + worldName);
        }
        
        return foundPackId;
    }
    
    /**
     * Register all pack-specific generators with Nukkit
     */
    private void registerPackGenerators() {
        Registry<ConfigPack> configRegistry = PLATFORM.getConfigRegistry();
        if (configRegistry == null) {
            getLogger().warning("No config registry found. No generators will be registered.");
            return;
        }
        
        int count = 0;
        for (ConfigPack pack : configRegistry.entries()) {
            String packId = pack.getID();
            
            // Skip empty or "unknown" pack IDs
            if (packId == null || packId.isEmpty() || packId.equalsIgnoreCase("unknown")) {
                continue;
            }
            
            // Register with lowercase name for consistency
            String generatorId = "terra:" + packId.toLowerCase();
            
            // Skip if already registered
            if (registeredGeneratorNames.contains(generatorId)) {
                LOGGER.debug("Skipping already registered generator: {}", generatorId);
                continue;
            }
            
            try {
                // Register the generator with Nukkit
                boolean success = Generator.addGenerator(NukkitGenerator.PackSpecificGenerator.class, generatorId, Generator.TYPE_INFINITE);
                if (success) {
                    registeredPacks.put(packId.toLowerCase(), NukkitGenerator.PackSpecificGenerator.class);
                    registeredGeneratorNames.add(generatorId);
                    
                    getLogger().info("Registered generator for pack '" + packId + "' as '" + generatorId + "'");
                    count++;
                    
                    // Also register with case insensitivity for better matching
                    if (!packId.equals(packId.toLowerCase())) {
                        String altGeneratorId = "terra:" + packId;
                        
                        // Skip if already registered
                        if (!registeredGeneratorNames.contains(altGeneratorId)) {
                            success = Generator.addGenerator(NukkitGenerator.PackSpecificGenerator.class, altGeneratorId, Generator.TYPE_INFINITE);
                            if (success) {
                                registeredPacks.put(packId, NukkitGenerator.PackSpecificGenerator.class);
                                registeredGeneratorNames.add(altGeneratorId);
                                count++;
                            }
                        }
                    }
                } else {
                    getLogger().warning("Failed to register generator for pack '" + packId + "' - may already be registered.");
                }
            } catch (Exception e) {
                getLogger().error("Failed to register generator for pack '" + packId + "': " + e.getMessage());
            }
        }
        
        // Also register a special generator for test worlds
        try {
            ConfigPack defaultPack = PLATFORM.getActiveConfig();
            if (defaultPack != null) {
                String defaultPackId = defaultPack.getID();
                
                // Register with "terra:test" ID for convenience if not already registered
                if (!registeredGeneratorNames.contains("terra:test")) {
                    boolean success = Generator.addGenerator(NukkitGenerator.PackSpecificGenerator.class, "terra:test", Generator.TYPE_INFINITE);
                    if (success) {
                        registeredPacks.put("test", NukkitGenerator.PackSpecificGenerator.class);
                        registeredGeneratorNames.add("terra:test");
                        count++;
                    }
                }
                
                // Also register a special "test" generator without the terra: prefix for better compatibility
                if (!registeredGeneratorNames.contains("test")) {
                    boolean success = Generator.addGenerator(NukkitGenerator.PackSpecificGenerator.class, "test", Generator.TYPE_INFINITE);
                    if (success) {
                        registeredGeneratorNames.add("test");
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            getLogger().error("Failed to register test generator: " + e.getMessage());
        }
        
        getLogger().info("Registered " + count + " pack-specific generators.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Terra Nukkit Plugin...");
        // Clean up caches
        worldToPackCache.clear();
        registeredPacks.clear();
        registeredGeneratorNames.clear();
        getLogger().info("Terra Nukkit Plugin Disabled.");
    }

    /**
     * Handle plugin reloading to update config packs
     */
    public void reload() {
        getLogger().info("Reloading Terra config packs...");
        
        // Clear caches
        worldToPackCache.clear();
        
        // Reload the platform's config registry
        boolean success = PLATFORM.reload();
        
        if (success) {
            getLogger().info("Config packs reloaded successfully.");
            
            // Update all active generators with the new config packs
            NukkitGenerator.updateAllGenerators();
            
            // Re-register pack-specific generators to ensure all packs are available
            registerPackGenerators();
            
            getLogger().info("Terra reload complete.");
        } else {
            getLogger().error("Failed to reload Terra config packs. Using existing configurations.");
        }
    }

    /**
     * Handle generator errors and reset the necessary state to prevent issues
     * This should be called when errors occur during world creation
     * 
     * @param worldName The name of the world being created
     * @param error The error that occurred
     */
    public void handleGeneratorError(String worldName, Throwable error) {
        getLogger().error("Error during world generation for world '" + worldName + "': " + error.getMessage(), error);
        
        System.out.println("[TERRA ERROR] Error during world generation for world '" + worldName + "': " + error.getMessage());
        
        // If the error is related to initialization or recursion, reset all world state
        if (error instanceof IllegalStateException 
            || (error.getMessage() != null && error.getMessage().contains("recursion"))) {
            getLogger().warning("Detected serious error - resetting all generator state");
            NukkitGenerator.resetAllWorldState();
        } else {
            // Otherwise just reset specific world state
            NukkitGenerator.resetWorldInitState(worldName);
            
            // Also reset thread-local state
            NukkitGenerator.resetThreadInitState();
            
            // Check for recursion issues and reset counter if needed
            if (error.getMessage() != null && error.getMessage().contains("Recursion depth became negative")) {
                getLogger().warning("Detected recursion tracking issue. Resetting recursion counter.");
                NukkitGenerator.resetRecursionCounter();
            }
            
            // Try to get a generator for this world and reset its state
            NukkitGenerator generator = NukkitGenerator.getCachedGenerator(worldName);
            if (generator != null) {
                generator.forceWorldName(worldName);
            }
        }
        
        getLogger().info("Generator state reset completed for world: " + worldName);
    }
    
    /**
     * Clean up the initialization lock for a world to prevent deadlocks
     * 
     * @param worldName The name of the world to clean up
     */
    private void cleanupWorldInitLock(String worldName) {
        try {
            // Use reflection to access the private WORLD_INIT_LOCKS map
            java.lang.reflect.Field locksField = NukkitGenerator.class.getDeclaredField("WORLD_INIT_LOCKS");
            locksField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> locks = (Map<String, Object>) locksField.get(null);
            
            if (locks != null && locks.containsKey(worldName)) {
                locks.remove(worldName);
                getLogger().debug("Removed initialization lock for world: " + worldName);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to clean up world initialization lock: " + e.getMessage());
        }
    }
} 