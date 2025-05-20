/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.block.state.properties.base;

import java.util.Arrays;
import java.util.Collection;

import com.dfsek.terra.api.block.state.properties.Property;


public interface EnumProperty<T extends Enum<T>> extends Property<T> {
    static <T extends Enum<T>> EnumProperty<T> of(String name, Class<T> clazz) {
        return new EnumProperty<>() {
            private final Collection<T> values = Arrays.asList(clazz.getEnumConstants());

            @Override
            public Collection<T> values() {
                return values;
            }

            @Override
            public String getID() {
                return name;
            }
            
            @Override
            public T getDefaultValue() {
                return values.iterator().next(); // Default to first enum value
            }
            
            @Override
            public String getName() {
                return name;
            }
            
            @Override
            public Class<T> getValueClass() {
                return clazz;
            }
            
            @Override
            public Iterable<T> getValues() {
                return values;
            }
        };
    }

    @Override
    default Class<T> getType() {
        return getValueClass();
    }
}
