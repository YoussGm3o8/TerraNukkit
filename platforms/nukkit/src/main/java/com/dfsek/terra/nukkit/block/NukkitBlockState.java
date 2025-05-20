package com.dfsek.terra.nukkit.block;

import cn.nukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import com.dfsek.terra.api.block.BlockType;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.block.state.properties.Property;

/**
 * Nukkit implementation of Terra's BlockState.
 * Similar to Allay's implementation but adapted for Nukkit.
 */
public final class NukkitBlockState implements BlockState {

    public static final NukkitBlockState AIR = new NukkitBlockState(Block.get(Block.AIR),
        NukkitJeBlockState.fromString("minecraft:air"));

    private final Block nukkitBlock;
    private final NukkitJeBlockState jeBlockState;
    private final boolean containsWater;

    public NukkitBlockState(Block nukkitBlock, NukkitJeBlockState jeBlockState) {
        this.nukkitBlock = nukkitBlock != null ? nukkitBlock : Block.get(Block.AIR);
        this.jeBlockState = jeBlockState;
        this.containsWater = "true".equals(jeBlockState.getPropertyValue("waterlogged"));
    }

    @Override
    public boolean matches(BlockState o) {
        if (!(o instanceof NukkitBlockState)) return false;
        NukkitBlockState other = (NukkitBlockState) o;
        return other.nukkitBlock.getId() == this.nukkitBlock.getId()
            && other.nukkitBlock.getDamage() == this.nukkitBlock.getDamage()
            && other.containsWater == this.containsWater;
    }

    @Override
    public <T extends Comparable<T>> boolean has(Property<T> property) {
        return false;
    }

    @Override
    public <T extends Comparable<T>> T get(Property<T> property) {
        return null;
    }

    @Override
    public <T extends Comparable<T>> BlockState set(Property<T> property, T value) {
        return this;
    }

    @Override
    public BlockType getBlockType() {
        return new NukkitBlockType(nukkitBlock);
    }

    @Override
    public String getAsString(boolean properties) {
        return jeBlockState.toString(properties);
    }

    @Override
    public boolean isAir() {
        return nukkitBlock.getId() == Block.AIR;
    }

    @Override
    public Block getHandle() {
        return nukkitBlock;
    }

    /**
     * Get the Nukkit block.
     */
    public Block getNukkitBlock() {
        return nukkitBlock;
    }
    
    /**
     * Check if this block contains water (is waterlogged).
     */
    public boolean containsWater() { 
        return containsWater; 
    }
    
    /**
     * Get the Java Edition block state representation.
     */
    public NukkitJeBlockState getJeBlockState() { 
        return jeBlockState; 
    }
    
    @Override
    public String toString() {
        return "NukkitBlockState{" +
            "block=" + nukkitBlock.getId() + ":" + nukkitBlock.getDamage() +
            ", jeBlockState=" + jeBlockState +
            ", waterlogged=" + containsWater +
            '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NukkitBlockState)) return false;
        
        NukkitBlockState other = (NukkitBlockState) obj;
        return matches(other);
    }
    
    @Override
    public int hashCode() {
        int result = nukkitBlock.getId();
        result = 31 * result + nukkitBlock.getDamage();
        result = 31 * result + (containsWater ? 1 : 0);
        return result;
    }
} 