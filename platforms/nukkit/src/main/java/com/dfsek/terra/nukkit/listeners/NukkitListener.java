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

package com.dfsek.terra.nukkit.listeners;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntitySpawnEvent;
import cn.nukkit.event.level.LevelLoadEvent;
import cn.nukkit.event.level.LevelInitEvent;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.generator.NukkitGenerator;
import com.dfsek.terra.nukkit.NukkitPlatform;

/**
 * Event listener for Nukkit-specific events.
 */
public class NukkitListener implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitListener.class);
    private final NukkitPlatform platform;

    public NukkitListener(NukkitPlatform platform) {
        this.platform = platform;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntitySpawn(EntitySpawnEvent e) {
        // Handle entity spawn events if needed
        // For now this is a placeholder for future implementation
    }
    
    /**
     * Handle a world being initialized.
     * This is where we should register any world-specific info.
     */
    @EventHandler
    public void onWorldInit(LevelInitEvent event) {
        Level level = event.getLevel();
        if (level == null) return;
        
        String worldName = level.getName();
        
        // Register the level's name in the cross-thread registry
        NukkitGenerator.registerLevelName(level);
        
        // Register thread prefixes for this world to help async chunk generation
        registerThreadPrefixes(worldName);
        
        // Print world initialization message
        System.out.println("[INFO] Terra world initialization for: " + worldName);
    }
    
    /**
     * Handle a world being loaded.
     * This ensures we have the world name registered for async operations.
     */
    @EventHandler
    public void onWorldLoad(LevelLoadEvent event) {
        Level level = event.getLevel();
        if (level == null) return;
        
        String worldName = level.getName();
        
        // Register the level's name in the cross-thread registry
        NukkitGenerator.registerLevelName(level);
        
        // Register thread prefixes for this world to help async chunk generation
        registerThreadPrefixes(worldName);
    }
    
    /**
     * Register standard thread name prefixes for a world.
     * This helps the generator determine which world a thread is operating on.
     */
    private void registerThreadPrefixes(String worldName) {
        if (worldName == null || worldName.isEmpty()) return;
        
        // Register common async task thread prefixes for Nukkit
        NukkitGenerator.registerAsyncThreadPrefix(worldName, "Nukkit Asynchronous Task Handler");
        
        // More specific prefixes that might be used for chunk generation
        NukkitGenerator.registerAsyncThreadPrefix(worldName, "Chunk Generation Thread");
        NukkitGenerator.registerAsyncThreadPrefix(worldName, "World Generation");
        NukkitGenerator.registerAsyncThreadPrefix(worldName, "Level " + worldName);
    }
} 