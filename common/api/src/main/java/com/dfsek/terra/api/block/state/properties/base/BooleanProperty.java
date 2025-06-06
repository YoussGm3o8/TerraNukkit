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


public interface BooleanProperty extends Property<Boolean> {
    static BooleanProperty of(String name) {
        return new BooleanProperty() {
            private static final Collection<Boolean> BOOLEANS = Arrays.asList(true, false);

            @Override
            public Collection<Boolean> values() {
                return BOOLEANS;
            }

            @Override
            public String getID() {
                return name;
            }
            
            @Override
            public Boolean getDefaultValue() {
                return false; // Default to false
            }
            
            @Override
            public String getName() {
                return name;
            }
            
            @Override
            public Class<Boolean> getValueClass() {
                return Boolean.class;
            }
            
            @Override
            public Iterable<Boolean> getValues() {
                return BOOLEANS;
            }
        };
    }

    @Override
    default Class<Boolean> getType() {
        return Boolean.class;
    }
}
