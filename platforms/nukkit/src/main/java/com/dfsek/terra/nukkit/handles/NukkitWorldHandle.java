package com.dfsek.terra.nukkit.handles;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.handle.WorldHandle;
import com.dfsek.terra.nukkit.block.NukkitBlockState;
import com.dfsek.terra.nukkit.block.NukkitJeBlockState;
import com.dfsek.terra.nukkit.block.NukkitMapping;

import cn.nukkit.block.Block;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Nukkit implementation of WorldHandle.
 */
public class NukkitWorldHandle implements WorldHandle {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitWorldHandle.class);

    @NotNull
    @Contract("_ -> new")
    @Override
    public BlockState createBlockState(@NotNull String data) {
        try {
            // Parse the input string to a JE block state
            NukkitJeBlockState jeBlockState = NukkitJeBlockState.fromString(data);
            
            // Convert to a Nukkit block
            Block nukkitBlock = NukkitMapping.blockStateJeToNukkit(jeBlockState);
            
            // Create and return the Terra block state
            return new NukkitBlockState(nukkitBlock, jeBlockState);
        } catch (Exception e) {
            LOGGER.error("Failed to parse block state spec: {}", data, e);
            return NukkitBlockState.AIR;
        }
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public BlockState air() {
        return NukkitBlockState.AIR;
    }

    @NotNull
    @Override
    public EntityType getEntity(@NotNull String id) {
        // Simple EntityType implementation
        LOGGER.debug("Creating entity type for ID: {}", id);
        return new EntityType() {
            private final Object handle = new Object(); // Dummy handle
            
            @Override
            public Object getHandle() {
                return handle;
            }
        };
    }
} 