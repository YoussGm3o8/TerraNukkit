package com.dfsek.terra.nukkit.world.biome;

import cn.nukkit.level.biome.EnumBiome;

import com.dfsek.terra.api.world.biome.PlatformBiome;

/**
 * Nukkit implementation of PlatformBiome.
 */
public record NukkitPlatformBiome(EnumBiome nukkitBiome) implements PlatformBiome {
    @Override
    public EnumBiome getHandle() {
        return nukkitBiome;
    }
} 