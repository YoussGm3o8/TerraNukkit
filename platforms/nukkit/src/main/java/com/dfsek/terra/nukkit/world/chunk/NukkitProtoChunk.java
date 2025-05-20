package com.dfsek.terra.nukkit.world.chunk;

import cn.nukkit.level.format.generic.BaseFullChunk;
import com.dfsek.terra.api.Handle;
import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.Entity;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.world.biome.Biome;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.world.WritableWorld;
import com.dfsek.terra.api.world.ReadableWorld;
import com.dfsek.terra.api.world.World;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.chunk.ChunkAccess;
import com.dfsek.terra.api.world.chunk.generation.ProtoChunk;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.world.NukkitWorld;
import com.dfsek.terra.nukkit.block.NukkitJeBlockState;
import com.dfsek.terra.nukkit.block.NukkitMapping;
import org.jetbrains.annotations.NotNull;

public class NukkitProtoChunk implements ProtoWorld, ProtoChunk {
    private final BaseFullChunk delegate;
    private final NukkitWorld world;

    public NukkitProtoChunk(BaseFullChunk delegate, World world) {
        this.delegate = delegate;
        if (!(world instanceof NukkitWorld nukkitWorld)) {
            throw new IllegalArgumentException("NukkitProtoChunk requires a NukkitWorld instance");
        }
        this.world = nukkitWorld;
    }

    @Override
    public int centerChunkX() {
        return delegate.getX();
    }

    @Override
    public int centerChunkZ() {
        return delegate.getZ();
    }

    @Override
    public ServerWorld getWorld() {
        return (ServerWorld) this.world;
    }

    @Override
    public int getMaxHeight() {
        return world.getMaxHeight();
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull BlockState blockState) {
        try {
            Object nukkitHandle = blockState.getHandle();
            if (y >= getMinHeight() && y < getMaxHeight()) {
                if (nukkitHandle instanceof cn.nukkit.block.Block nukkitBlock) {
                    // Use the block directly if it's a Nukkit block
                    delegate.setBlock(x, y, z, nukkitBlock.getId(), nukkitBlock.getDamage());
                } else {
                    // Try to parse the block name from the block state
                    String blockString = blockState.getAsString();
                    String blockName = blockString;
                    
                    // Extract just the block name without properties
                    if (blockString.contains("[")) {
                        blockName = blockString.substring(0, blockString.indexOf("["));
                    }
                    
                    // Remove minecraft: prefix if present
                    if (blockName.startsWith("minecraft:")) {
                        blockName = blockName.substring(10);
                    }
                    
                    // Try to map to a known Nukkit block
                    cn.nukkit.block.Block mappedBlock = mapToNukkitBlock(blockName);
                    if (mappedBlock != null) {
                        delegate.setBlock(x, y, z, mappedBlock.getId(), mappedBlock.getDamage());
                    } else {
                        // Default to stone if mapping fails
                        delegate.setBlock(x, y, z, cn.nukkit.block.Block.STONE, 0);
                        System.err.println("Failed to map block state in NukkitProtoChunk: " + blockState.getAsString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error setting block at " + x + "," + y + "," + z + ": " + e.getMessage());
            // Default to stone if an error occurs
            if (y >= getMinHeight() && y < getMaxHeight()) {
                delegate.setBlock(x, y, z, cn.nukkit.block.Block.STONE, 0);
            }
        }
    }
    
    /**
     * Map a block name to a Nukkit block
     * 
     * @param blockName The block name (without minecraft: prefix)
     * @return The mapped Nukkit block, or null if no mapping exists
     */
    private cn.nukkit.block.Block mapToNukkitBlock(String blockName) {
        // Try direct mapping first
        try {
            // Common blocks mapping
            switch (blockName.toLowerCase()) {
                case "stone": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.STONE);
                case "grass_block": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.GRASS);
                case "dirt": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.DIRT);
                case "cobblestone": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.COBBLESTONE);
                case "oak_planks": case "planks": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.PLANKS);
                case "bedrock": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.BEDROCK);
                case "sand": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.SAND);
                case "gravel": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.GRAVEL);
                case "gold_ore": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.GOLD_ORE);
                case "iron_ore": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.IRON_ORE);
                case "coal_ore": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.COAL_ORE);
                case "oak_log": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.LOG);
                case "oak_leaves": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.LEAVES);
                case "grass": case "tall_grass": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.TALL_GRASS);
                case "water": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.WATER);
                case "lava": return cn.nukkit.block.Block.get(cn.nukkit.block.Block.LAVA);
                default: return null;
            }
        } catch (Exception e) {
            System.err.println("Error mapping block name " + blockName + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    @NotNull
    public BlockState getBlock(int x, int y, int z) {
        if (y >= getMinHeight() && y < getMaxHeight()) {
            cn.nukkit.block.Block nukkitBlock = cn.nukkit.block.Block.get(delegate.getBlockId(x, y, z), delegate.getBlockData(x, y, z));
            return TerraNukkitPlugin.PLATFORM.getWorldHandle().createBlockState(nukkitBlock.getName());
        } else {
            return TerraNukkitPlugin.PLATFORM.getWorldHandle().air();
        }
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        world.setBlockState(x, y, z, data, physics);
    }

    @Override
    public Entity spawnEntity(double x, double y, double z, EntityType entityType) {
        return world.spawnEntity(x, y, z, entityType);
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        return world.getBlockState(x, y, z);
    }

    @Override
    public BlockEntity getBlockEntity(int x, int y, int z) {
        return world.getBlockEntity(x, y, z);
    }

    @Override
    public ChunkGenerator getGenerator() {
        return world.getGenerator();
    }

    @Override
    public BiomeProvider getBiomeProvider() {
        return world.getBiomeProvider();
    }

    @Override
    public ConfigPack getPack() {
        return world.getPack();
    }

    @Override
    public long getSeed() {
        return world.getSeed();
    }

    @Override
    public int getMinHeight() {
        return world.getMinHeight();
    }

    @Override
    public Object getHandle() {
        return delegate;
    }

    /**
     * Set the biome at a specific column in this chunk
     * 
     * @param x X coordinate (0-15)
     * @param z Z coordinate (0-15)
     * @param biome The biome to set
     */
    public void setBiome(int x, int z, com.dfsek.terra.api.world.biome.PlatformBiome biome) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16) {
            // Try to get the Nukkit biome ID
            Object handle = biome.getHandle();
            int biomeId = 1; // Default to plains (biome ID 1)
            
            if (handle instanceof Integer id) {
                biomeId = id;
            } else if (handle instanceof cn.nukkit.level.biome.EnumBiome enumBiome) {
                biomeId = enumBiome.id;
            }
            
            delegate.setBiomeId(x, z, biomeId);
        }
    }
} 