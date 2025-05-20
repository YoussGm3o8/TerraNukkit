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

package com.dfsek.terra.nukkit.mapping;

import cn.nukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a Terra block ID to a Nukkit block ID and data value.
 */
public class NukkitBlockMapping {
    private final int blockId;
    private final int defaultData;
    private final List<StateMapping> stateMappings;

    public NukkitBlockMapping(int blockId, int defaultData) {
        this.blockId = blockId;
        this.defaultData = defaultData;
        this.stateMappings = new ArrayList<>();
    }

    public NukkitBlockMapping(Block block) {
        this(block.getId(), block.getDamage());
    }

    /**
     * Add a state mapping for this block type.
     *
     * @param properties State properties
     * @param data Data value
     */
    public void addStateMapping(@NotNull Map<String, String> properties, int data) {
        stateMappings.add(new StateMapping(properties, data));
    }

    /**
     * Get the Nukkit block ID for this mapping.
     *
     * @return Block ID
     */
    public int getBlockId() {
        return blockId;
    }

    /**
     * Get the default Nukkit data value.
     *
     * @return Default data value
     */
    public int getDefaultData() {
        return defaultData;
    }

    /**
     * Get all state mappings for this block type.
     *
     * @return Unmodifiable list of state mappings
     */
    public List<StateMapping> getStateMappings() {
        return Collections.unmodifiableList(stateMappings);
    }

    /**
     * Represents a mapping from Terra block state properties to a Nukkit data value.
     */
    public static class StateMapping {
        private final Map<String, String> properties;
        private final int data;

        public StateMapping(@NotNull Map<String, String> properties, int data) {
            this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
            this.data = data;
        }

        /**
         * Get the properties for this state mapping.
         *
         * @return Map of property name to value
         */
        public Map<String, String> properties() {
            return properties;
        }

        /**
         * Get the Nukkit data value for this state mapping.
         *
         * @return Data value
         */
        public int data() {
            return data;
        }
    }
} 