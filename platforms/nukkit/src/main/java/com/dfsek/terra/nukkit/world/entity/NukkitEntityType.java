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

import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.registry.key.RegistryKey;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of Terra's EntityType for Nukkit entities.
 */
public class NukkitEntityType implements EntityType {
    private final RegistryKey key;
    private final String nukkitId;

    public NukkitEntityType(RegistryKey key, String nukkitId) {
        this.key = key;
        this.nukkitId = nukkitId;
    }

    /**
     * Get the Nukkit entity ID string for this entity type.
     *
     * @return Nukkit entity ID
     */
    public String getNukkitId() {
        return nukkitId;
    }

    /**
     * Get the registry key for this entity type.
     *
     * @return Registry key
     */
    public @NotNull RegistryKey getRegistryKey() {
        return key;
    }

    @Override
    public Object getHandle() {
        return nukkitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NukkitEntityType that)) return false;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "NukkitEntityType{" +
                "key=" + key +
                ", nukkitId='" + nukkitId + '\'' +
                '}';
    }
} 