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

package com.dfsek.terra.nukkit.world.entity;

import cn.nukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.util.vector.Vector3;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.world.NukkitWorld;

/**
 * Implementation of Terra's Entity interface for Nukkit.
 */
public class NukkitEntity implements com.dfsek.terra.api.entity.Entity {
    private final Entity nukkit;
    private ServerWorld world;

    public NukkitEntity(Entity nukkit) {
        this.nukkit = nukkit;
    }

    @Override
    public Vector3 position() {
        return Vector3.of(nukkit.getX(), nukkit.getY(), nukkit.getZ());
    }

    @Override
    public void position(Vector3 position) {
        nukkit.teleport(new cn.nukkit.math.Vector3(position.getX(), position.getY(), position.getZ()));
    }

    @Override
    public void world(ServerWorld world) {
        this.world = world;
    }

    @Override
    public ServerWorld world() {
        if (world == null && nukkit.level != null) {
            // Create a simple world wrapper without relying on platform methods
            // The real implementation would need to find or create a proper Terra world
            return new NukkitWorld(
                nukkit.level,  
                null, // We don't have access to a generator here
                null, // We don't have access to a config pack here
                TerraNukkitPlugin.PLATFORM);
        }
        return world;
    }

    @Override
    public Object getHandle() {
        return nukkit;
    }
} 