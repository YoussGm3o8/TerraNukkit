/*
 * Copyright (c) 2020-2025 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.biome;

import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.registry.key.RegistryKey;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a platform biome type.
 */
public interface BiomeType extends Handle {
    /**
     * Get the registry key for this biome type.
     *
     * @return Registry key
     */
    @NotNull RegistryKey getRegistryKey();
} 