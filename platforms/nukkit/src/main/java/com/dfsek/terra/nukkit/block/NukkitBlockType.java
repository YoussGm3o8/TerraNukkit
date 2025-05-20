package com.dfsek.terra.nukkit.block;

import cn.nukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.block.BlockType;
import com.dfsek.terra.api.block.state.BlockState;

/**
 * Nukkit implementation of BlockType.
 */
public record NukkitBlockType(Block nukkitBlock) implements BlockType {

    @Override
    public BlockState getDefaultState() {
        return new NukkitBlockState(nukkitBlock, NukkitMapping.blockStateNukkitToJe(nukkitBlock));
    }

    @Override
    public boolean isSolid() {
        return nukkitBlock.isSolid();
    }

    @Override
    public boolean isWater() {
        return nukkitBlock.getId() == Block.WATER || nukkitBlock.getId() == Block.STILL_WATER;
    }

    @Override
    public Object getHandle() {
        return nukkitBlock;
    }
} 