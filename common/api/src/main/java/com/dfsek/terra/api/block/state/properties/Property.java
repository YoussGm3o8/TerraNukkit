/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.block.state.properties;

import java.util.Collection;

import com.dfsek.terra.api.registry.key.StringIdentifiable;


/**
 * Represents a property on a block state.
 *
 * @param <T> Type of value this property holds
 */
public interface Property<T extends Comparable<T>> extends StringIdentifiable {
    /**
     * Get the name of this property.
     *
     * @return Property name
     */
    String getName();
    
    /**
     * Get the class of values this property holds.
     *
     * @return Value class
     */
    Class<T> getValueClass();
    
    /**
     * Get the possible values this property can hold.
     *
     * @return Possible values
     */
    Iterable<T> getValues();
    
    /**
     * Get the default value of this property.
     *
     * @return Default value
     */
    T getDefaultValue();

    /**
     * Get all possible values of this property
     *
     * @return All values of this property
     */
    Collection<T> values();

    /**
     * Get the type of this property.
     *
     * @return {@link Class} instance representing the type of this property
     */
    Class<T> getType();
}
