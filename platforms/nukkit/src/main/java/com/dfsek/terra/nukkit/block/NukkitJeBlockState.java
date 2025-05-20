package com.dfsek.terra.nukkit.block;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a Java Edition block state for mapping to Nukkit blocks.
 */
public class NukkitJeBlockState {
    protected final String identifier;
    protected final TreeMap<String, String> properties;
    protected int hash = Integer.MAX_VALUE;

    public static NukkitJeBlockState fromString(String data) {
        return new NukkitJeBlockState(data);
    }

    public static NukkitJeBlockState create(String identifier, TreeMap<String, String> properties) {
        return new NukkitJeBlockState(identifier, properties);
    }

    private NukkitJeBlockState(String data) {
        String[] strings = data.replace("[", ",").replace("]", ",").replace(" ", "").split(",");
        this.identifier = strings[0];
        this.properties = new TreeMap<>();
        if (strings.length > 1) {
            for (int i = 1; i < strings.length; i++) {
                final String tmp = strings[i];
                final int index = tmp.indexOf("=");
                if (index > 0) {
                    properties.put(tmp.substring(0, index), tmp.substring(index + 1));
                }
            }
        }
        completeMissingProperties();
    }

    public String getPropertyValue(String key) {
        return properties.get(key);
    }

    private void completeMissingProperties() {
        Map<String, String> defaultProperties = NukkitMapping.getJeBlockDefaultProperties(identifier);
        if(properties.size() == defaultProperties.size()) {
            return;
        }
        defaultProperties.entrySet().stream().filter(entry -> !properties.containsKey(entry.getKey())).forEach(
            entry -> properties.put(entry.getKey(), entry.getValue()));
    }

    private NukkitJeBlockState(String identifier, TreeMap<String, String> properties) {
        this.identifier = identifier;
        this.properties = properties;
        completeMissingProperties();
    }

    /**
     * Get the block state as a string in the format minecraft:block[property=value,property=value]
     */
    public String getAsString() {
        if(properties.isEmpty()) return identifier;
        
        StringBuilder result = new StringBuilder(identifier);
        result.append("[");
        
        boolean first = true;
        for(Map.Entry<String, String> entry : properties.entrySet()) {
            if(!first) {
                result.append(",");
            }
            result.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        result.append("]");
        return result.toString();
    }

    /**
     * Get the block state as a string, with properties if requested.
     * Also calculates hash if needed.
     * 
     * @param includeProperties Whether to include properties
     * @return String representation
     */
    public String toString(boolean includeProperties) {
        if(!includeProperties) return identifier;
        return getAsString();
    }

    /**
     * Get a hash code for this block state, including properties.
     * Used for mapping lookups.
     * 
     * @return Hash code
     */
    public int getHash() {
        if (hash == Integer.MAX_VALUE) {
            hash = getAsString().hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        return getAsString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NukkitJeBlockState)) return false;
        
        NukkitJeBlockState that = (NukkitJeBlockState) o;
        return getHash() == that.getHash();
    }
    
    @Override
    public int hashCode() {
        return getHash();
    }
} 