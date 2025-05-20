package com.dfsek.terra.nukkit.block;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a Java Edition block state for mapping purposes.
 * This is similar to the JeBlockState in Allay but simplified for Nukkit.
 */
public class NukkitJeBlockState {
    protected final String identifier;
    protected final TreeMap<String, String> properties;
    
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
                if (tmp.isEmpty()) continue;
                final int index = tmp.indexOf("=");
                if (index > 0) {
                    properties.put(tmp.substring(0, index), tmp.substring(index + 1));
                }
            }
        }
        completeMissingProperties();
    }

    private NukkitJeBlockState(String identifier, TreeMap<String, String> properties) {
        this.identifier = identifier;
        this.properties = properties != null ? properties : new TreeMap<>();
    }

    public String getPropertyValue(String key) {
        return properties.get(key);
    }

    private void completeMissingProperties() {
        // For now, we're not implementing default properties mapping
        // This can be enhanced later with a proper mapping system
    }

    public String toString(boolean includeProperties) {
        if (!includeProperties || properties.isEmpty()) return identifier;
        
        StringBuilder builder = new StringBuilder(identifier).append('[');
        boolean first = true;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public String toString() {
        return toString(true);
    }
} 