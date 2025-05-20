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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

public class TerraNukkitPlugin extends PluginBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerraNukkitPlugin.class);
    public static TerraNukkitPlugin INSTANCE;
    public static NukkitPlatform PLATFORM;
    private final Map<String, Class<? extends NukkitGenerator>> registeredPacks = new HashMap<>();

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
        
        // No initialization needed for this simple addon holder
        // @Override public void initialize() { }
    };

    @Override
    public void onLoad() {
        INSTANCE = this;
        getLogger().info("Loading Terra for Nukkit...");

        // Initialize the platform first so we can access config packs
        getLogger().info("Initializing Nukkit platform...");
        PLATFORM = new NukkitPlatform(this);
        
        // Register the generic Terra generator type first as a fallback
        Generator.addGenerator(NukkitGenerator.class, "terra", Generator.TYPE_INFINITE);
        getLogger().info("Registered generic Terra generator type.");
        
        // Register pack-specific generators after platform initialization
        getLogger().info("Terra Nukkit Plugin Loaded.");
    }

    @Override
    public void onEnable() {
        getLogger().info("Enabling Terra Nukkit Plugin v" + getDescription().getVersion() + "...");

        // Platform is already initialized in onLoad(), don't create a new instance here
        
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
        
        // Register pack-specific generators
        registerPackGenerators();
        
        getLogger().info("Terra Nukkit Plugin Enabled.");
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
            String generatorId = "terra:" + packId;
            
            try {
                // Create generator options with the pack ID
                Map<String, Object> options = new HashMap<>();
                options.put("pack", packId);
                
                // Register with Nukkit
                Generator.addGenerator(NukkitGenerator.PackSpecificGenerator.class, generatorId, Generator.TYPE_INFINITE);
                registeredPacks.put(packId, NukkitGenerator.PackSpecificGenerator.class);
                
                getLogger().info("Registered generator for pack '" + packId + "' as '" + generatorId + "'");
                count++;
            } catch (Exception e) {
                getLogger().error("Failed to register generator for pack '" + packId + "'", e);
            }
        }
        
        getLogger().info("Registered " + count + " pack-specific generators.");
    }
    
    /**
     * Creates a specialized NukkitGenerator class for a specific config pack
     * 
     * @param pack The config pack to create a generator for
     * @return A generator class specifically for the given pack
     */
    private Class<? extends NukkitGenerator> createGeneratorClass(ConfigPack pack) {
        // Return the PackSpecificGenerator class instead of an instance
        return NukkitGenerator.PackSpecificGenerator.class;
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Terra Nukkit Plugin...");
        // Perform any necessary cleanup
        getLogger().info("Terra Nukkit Plugin Disabled.");
    }
} 