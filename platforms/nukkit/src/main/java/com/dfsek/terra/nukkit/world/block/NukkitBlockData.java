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

package com.dfsek.terra.nukkit.world.block;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.util.vector.Vector3Int;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.nukkit.NukkitPlatform;

/**
 * Represents a Nukkit block data at a location.
 */
public class NukkitBlockData {
    private final Block block;
    private final Level level;
    private final Vector3Int position;
    private final NukkitPlatform platform;

    public NukkitBlockData(Level level, Vector3Int position, NukkitPlatform platform, Block block) {
        this.level = level;
        this.position = position;
        this.platform = platform;
        this.block = block;
    }

    /**
     * Get the Nukkit Block represented by this block data.
     *
     * @return Nukkit Block
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Get the BlockState for this block.
     *
     * @return BlockState
     */
    public @NotNull BlockState getBlockState() {
        return new NukkitBlockState(block);
    }
} 