package com.dfsek.terra.nukkit.test;

import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.level.ChunkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.generator.NukkitGenerator;
import com.dfsek.terra.nukkit.world.NukkitWorld;
import com.dfsek.terra.nukkit.world.chunk.NukkitProtoChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple test class to benchmark Terra chunk generation in Nukkit.
 * This can be used to verify that optimizations are working as expected.
 */
public class PerformanceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTest.class);
    
    /**
     * Run a performance test for chunk generation.
     * 
     * @param level The level to test in
     * @param packId The config pack ID to use
     * @param chunkCount The number of chunks to generate
     */
    public static void runTest(Level level, String packId, int chunkCount) {
        if (level == null) {
            LOGGER.error("Cannot run performance test: Level is null");
            return;
        }
        
        LOGGER.info("Starting Terra chunk generation performance test...");
        LOGGER.info("Testing with pack: {}, generating {} chunks", packId, chunkCount);
        
        // Get config pack
        ConfigPack pack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                            .getByID(packId)
                            .orElse(null);
        
        if (pack == null) {
            LOGGER.error("Config pack '{}' not found", packId);
            return;
        }
        
        try {
            // Create generator components
            Map<String, Object> options = new HashMap<>();
            options.put("pack", packId);
            options.put("worldName", level.getName());
            options.put("seed", String.valueOf(level.getSeed()));
            
            // Create generator with a direct reference to the level
            LOGGER.info("Creating test generator for level: {}", level.getName());
            TestGenerator generator = new TestGenerator(options, level);
            generator.setConfigPack(pack);
            generator.init(level, new NukkitRandom(level.getSeed()));
            
            // Create Terra world
            NukkitWorld terraWorld = new NukkitWorld(level, null, pack, TerraNukkitPlugin.PLATFORM);
            
            // Get Terra generator and biome provider
            ChunkGenerator terraGenerator = pack.getGeneratorProvider().newInstance(pack);
            BiomeProvider biomeProvider = pack.getBiomeProvider();
            
            // Set the generator in the world
            terraWorld.setGenerator(terraGenerator);
            
            // Run the test
            long totalTime = 0;
            int successfulChunks = 0;
            
            // Use a consistent random seed for biome selection
            Random random = new Random(level.getSeed());
            
            // Keep track of total generated chunks
            AtomicInteger totalGenerated = new AtomicInteger(0);
            
            // Generate chunks around spawn in a spiral pattern
            LOGGER.info("Generating {} chunks around spawn...", chunkCount);
            
            for (int i = 0; i < chunkCount; i++) {
                // Use a spiral pattern around 0,0
                int chunkX = (i / 4) * ((i % 4 >= 2) ? 1 : -1);
                int chunkZ = (i / 4) * (((i % 4) % 2 == 0) ? 1 : -1);
                
                // Prefetch the chunk first to ensure it's loaded
                level.loadChunk(chunkX, chunkZ);
                FullChunk chunk = level.getChunk(chunkX, chunkZ);
                
                if (chunk != null) {
                    // Measure generation time
                    long startTime = System.nanoTime();
                    
                    try {
                        // Create proto chunk and explicitly set the level
                        NukkitProtoChunk protoChunk = new NukkitProtoChunk(chunk, terraWorld);
                        
                        // Set biomes
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                int worldX = chunkX * 16 + x;
                                int worldZ = chunkZ * 16 + z;
                                
                                com.dfsek.terra.api.world.biome.Biome terraBiome = 
                                    biomeProvider.getBiome(worldX, 0, worldZ, random.nextLong());
                                
                                if (terraBiome instanceof com.dfsek.terra.api.world.biome.PlatformBiome platformBiome) {
                                    protoChunk.setBiome(x, z, platformBiome);
                                }
                            }
                        }
                        
                        // Generate terrain
                        terraGenerator.generateChunkData(protoChunk, terraWorld, biomeProvider, chunkX, chunkZ);
                        
                        long endTime = System.nanoTime();
                        long chunkTime = (endTime - startTime) / 1_000_000; // Convert to ms
                        
                        totalTime += chunkTime;
                        successfulChunks++;
                        
                        if (i % 5 == 0 || i == chunkCount - 1) {
                            LOGGER.info("Generated chunk {},{} in {} ms (avg: {} ms)", 
                                     chunkX, chunkZ, chunkTime, totalTime / (successfulChunks > 0 ? successfulChunks : 1));
                        }
                        
                        totalGenerated.incrementAndGet();
                    } catch (Exception e) {
                        LOGGER.error("Error generating chunk {},{}: {}", chunkX, chunkZ, e.getMessage(), e);
                    }
                } else {
                    LOGGER.warn("Failed to get chunk {},{} from level", chunkX, chunkZ);
                }
            }
            
            // Calculate and log results
            double avgTime = successfulChunks > 0 ? totalTime / (double) successfulChunks : 0;
            LOGGER.info("Performance test complete!");
            LOGGER.info("Generated {} chunks with pack '{}' in {} ms", successfulChunks, packId, totalTime);
            LOGGER.info("Average generation time: {} ms per chunk", avgTime);
            LOGGER.info("Chunks per second: {}", avgTime > 0 ? 1000.0 / avgTime : 0);
            
        } catch (Exception e) {
            LOGGER.error("Error during performance test: {}", e.getMessage(), e);
        }
    }
    
    /**
     * A specialized generator that keeps a strong reference to the level
     * to prevent it from being lost during async operations
     */
    private static class TestGenerator extends NukkitGenerator {
        private final Level levelReference;
        private final String worldName;
        private final long startTime;
        
        public TestGenerator(Map<String, Object> options, Level level) {
            super(options);
            this.levelReference = level;
            this.worldName = level != null ? level.getName() : "unknown";
            this.startTime = System.currentTimeMillis();
            
            // Force set the world name for async operations
            forceWorldName(this.worldName);
            
            // Make sure options are properly set
            if (options != null) {
                options.put("worldName", this.worldName);
                options.put("seed", String.valueOf(level != null ? level.getSeed() : 0));
            }
            
            LOGGER.info("TestGenerator created for world: {} at time: {}", worldName, startTime);
        }
        
        @Override
        public void init(ChunkManager chunkManager, NukkitRandom nukkitRandom) {
            // Always make sure we have the level reference
            ChunkManager effectiveManager = (levelReference != null) ? levelReference : chunkManager;
            
            // Force world name for async operations
            forceWorldName(worldName);
            
            // Call parent init with our verified manager
            super.init(effectiveManager, nukkitRandom);
        }
        
        @Override
        public void generateChunk(int chunkX, int chunkZ) {
            // Force world name before generation to ensure thread-local state
            forceWorldName(worldName);
            
            // Set the level before delegating if it's not already set
            if (super.getChunkManager() instanceof Level) {
                super.generateChunk(chunkX, chunkZ);
            } else {
                // Use our stored level reference
                LOGGER.debug("Using stored level reference for chunk generation");
                super.init(levelReference, new NukkitRandom(levelReference.getSeed()));
                super.generateChunk(chunkX, chunkZ);
            }
        }
    }
} 