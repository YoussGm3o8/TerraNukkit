package com.dfsek.terra.nukkit.world.block;

import cn.nukkit.block.Block;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a mapping between a Terra block ID and a Nukkit block.
 */
public class NukkitBlockMapping {
    private final int id;
    private final int defaultData;
    private final List<StateMapping> states;

    /**
     * Creates a new NukkitBlockMapping instance from a Nukkit Block.
     *
     * @param block Nukkit Block
     */
    public NukkitBlockMapping(Block block) {
        this.id = block.getId();
        this.defaultData = block.getDamage();
        this.states = Collections.emptyList();
    }

    /**
     * Creates a new NukkitBlockMapping instance.
     *
     * @param id         Block ID
     * @param defaultData Default data value
     */
    public NukkitBlockMapping(int id, int defaultData) {
        this.id = id;
        this.defaultData = defaultData;
        this.states = Collections.emptyList();
    }

    /**
     * Creates a new NukkitBlockMapping instance with states.
     *
     * @param id         Block ID
     * @param defaultData Default data value
     * @param states     List of state mappings
     */
    public NukkitBlockMapping(int id, int defaultData, List<StateMapping> states) {
        this.id = id;
        this.defaultData = defaultData;
        this.states = states != null ? states : Collections.emptyList();
    }

    /**
     * Get the Nukkit block ID.
     *
     * @return Block ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the default data value.
     *
     * @return Default data value
     */
    public int getDefaultData() {
        return defaultData;
    }

    /**
     * Get the list of state mappings.
     *
     * @return State mappings
     */
    public List<StateMapping> getStates() {
        return states;
    }

    /**
     * Represents a mapping between a Terra block state and a Nukkit block data value.
     */
    public static class StateMapping {
        private final int data;
        private final List<PropertyMapping> properties;

        /**
         * Creates a new StateMapping instance.
         *
         * @param data       Data value
         * @param properties Property mappings
         */
        public StateMapping(int data, List<PropertyMapping> properties) {
            this.data = data;
            this.properties = properties != null ? properties : Collections.emptyList();
        }

        /**
         * Get the data value.
         *
         * @return Data value
         */
        public int data() {
            return data;
        }

        /**
         * Get the property mappings.
         *
         * @return Property mappings
         */
        public List<PropertyMapping> properties() {
            return properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StateMapping that = (StateMapping) o;
            return data == that.data && Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, properties);
        }
    }

    /**
     * Represents a mapping between a Terra block property and a Nukkit block property.
     */
    public static class PropertyMapping {
        private final String key;
        private final String value;

        /**
         * Creates a new PropertyMapping instance.
         *
         * @param key   Property key
         * @param value Property value
         */
        public PropertyMapping(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Get the property key.
         *
         * @return Property key
         */
        public String key() {
            return key;
        }

        /**
         * Get the property value.
         *
         * @return Property value
         */
        public String value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PropertyMapping that = (PropertyMapping) o;
            return Objects.equals(key, that.key) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }
} 