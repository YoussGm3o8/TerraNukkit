package com.dfsek.terra.nukkit.mapping;

import cn.nukkit.block.Block;
import cn.nukkit.item.Item;
import cn.nukkit.level.biome.Biome;
import cn.nukkit.level.biome.EnumBiome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.api.block.BlockType;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.block.NukkitBlockState;
import com.dfsek.terra.nukkit.block.NukkitBlockType;
import com.dfsek.terra.nukkit.block.NukkitJeBlockState;
import com.dfsek.terra.nukkit.inventory.NukkitItem;
import com.dfsek.terra.nukkit.world.biome.NukkitBiome;
import com.dfsek.terra.nukkit.world.biome.NukkitPlatformBiome;
import com.dfsek.terra.nukkit.world.entity.NukkitEntityType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified mapping registry for Nukkit that handles mappings between Terra and Nukkit concepts.
 */
public class NukkitMappingRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitMappingRegistry.class);
    private static final Gson GSON = new Gson();

    // Resource file paths
    private static final String BLOCK_MAPPING_FILE = "nukkit_mappings/blocks.json";
    private static final String ITEM_MAPPING_FILE = "nukkit_mappings/items.json";
    private static final String BIOME_MAPPING_FILE = "nukkit_mappings/biomes.json";
    private static final String ENTITY_MAPPING_FILE = "nukkit_mappings/entities.json";

    // Block mappings
    private final Map<RegistryKey, BlockMapping> terraToNukkitBlockMap = new ConcurrentHashMap<>();
    private final Map<String, RegistryKey> nukkitToTerraBlockMap = new ConcurrentHashMap<>();
    private final Map<RegistryKey, NukkitBlockType> blockTypeCache = new ConcurrentHashMap<>();

    // Item mappings
    private final Map<RegistryKey, ItemMapping> terraToNukkitItemMap = new ConcurrentHashMap<>();
    private final Map<String, RegistryKey> nukkitToTerraItemMap = new ConcurrentHashMap<>();
    private final Map<RegistryKey, NukkitItem> itemCache = new ConcurrentHashMap<>();

    // Biome mappings
    private final Map<RegistryKey, Integer> terraToBiomeIdMap = new ConcurrentHashMap<>();
    private final Map<Integer, NukkitBiome> biomeIdToTerraBiomeMap = new ConcurrentHashMap<>();
    private final Map<Integer, NukkitPlatformBiome> nukkitIdToPlatformBiome = new ConcurrentHashMap<>();

    // Entity mappings
    private final Map<RegistryKey, String> terraToNukkitEntityMap = new ConcurrentHashMap<>();
    private final Map<String, RegistryKey> nukkitToTerraEntityMap = new ConcurrentHashMap<>();
    private final Map<RegistryKey, NukkitEntityType> entityTypeCache = new ConcurrentHashMap<>();

    // Default instances
    private final BlockState airState;
    private final NukkitBiome defaultBiome;
    private final NukkitEntityType defaultEntityType;

    /**
     * Creates a new NukkitMappingRegistry and loads all mappings.
     */
    public NukkitMappingRegistry() {
        // Use the built-in air state from NukkitBlockState
        airState = NukkitBlockState.AIR;

        // Load mappings from files
        loadBlockMappings();
        loadItemMappings();
        loadBiomeMappings();
        loadEntityMappings();

        // Initialize default biome (plains)
        Biome nukkitPlainsBiome = Biome.getBiome(EnumBiome.PLAINS.id);
        RegistryKey plainsKey = RegistryKey.of("minecraft", "plains");
        defaultBiome = new NukkitBiome(nukkitPlainsBiome, plainsKey);
        
        // Initialize default entity type (zombie)
        RegistryKey zombieKey = RegistryKey.of("minecraft", "zombie");
        defaultEntityType = new NukkitEntityType(zombieKey, "Zombie");
        
        // Initialize biome registry
        initBiomes();
    }

    /**
     * Initialize biome registry with Nukkit biomes.
     */
    private void initBiomes() {
        try {
            for (EnumBiome enumBiome : EnumBiome.values()) {
                Biome nukkitBiome = Biome.getBiome(enumBiome.id);
                if (nukkitBiome != null) {
                    // Create a key based on biome name
                    String biomeName = enumBiome.name().toLowerCase();
                    RegistryKey biomeKey = RegistryKey.of("minecraft", biomeName);
                    
                    // Create Terra biome wrapper
                    NukkitBiome terraBiome = new NukkitBiome(nukkitBiome, biomeKey);
                    biomeIdToTerraBiomeMap.put(enumBiome.id, terraBiome);
                    terraToBiomeIdMap.put(biomeKey, enumBiome.id);
                    
                    // Create platform biome wrapper
                    NukkitPlatformBiome platformBiome = new NukkitPlatformBiome(enumBiome);
                    nukkitIdToPlatformBiome.put(enumBiome.id, platformBiome);
                }
            }
            LOGGER.info("Registered {} Nukkit biomes", biomeIdToTerraBiomeMap.size());
        } catch (Exception e) {
            LOGGER.error("Failed to initialize biomes", e);
        }
    }

    // ==================== Loading Methods ====================

    /**
     * Load block mappings from JSON file.
     */
    private void loadBlockMappings() {
        try (InputStream stream = getResourceStream(BLOCK_MAPPING_FILE);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            
            Type type = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
            Map<String, Map<String, Integer>> mappings = GSON.fromJson(reader, type);
            
            if (mappings == null) {
                LOGGER.warn("No block mappings found in {}", BLOCK_MAPPING_FILE);
                return;
            }
            
            for (Map.Entry<String, Map<String, Integer>> entry : mappings.entrySet()) {
                String terraId = entry.getKey();
                RegistryKey terraKey = parseRegistryKey(terraId);
                
                if (terraKey == null) continue;
                
                Map<String, Integer> nukkitData = entry.getValue();
                Integer id = nukkitData.get("id");
                Integer data = nukkitData.get("data");
                
                if (id == null || data == null) {
                    LOGGER.warn("Missing id or data for block {}", terraId);
                    continue;
                }
                
                BlockMapping mapping = new BlockMapping(id, data);
                terraToNukkitBlockMap.put(terraKey, mapping);
                nukkitToTerraBlockMap.put(id + ":" + data, terraKey);
                
                // Create a Nukkit block and JE block state representation for this mapping
                Block nukkitBlock = Block.get(id.intValue(), data.intValue());
                NukkitJeBlockState jeBlockState = NukkitJeBlockState.fromString(terraId);
                
                // Create and cache block type
                NukkitBlockType blockType = new NukkitBlockType(nukkitBlock);
                blockTypeCache.put(terraKey, blockType);
            }
            
            LOGGER.info("Loaded {} block mappings", terraToNukkitBlockMap.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load block mappings", e);
        }
    }

    /**
     * Load item mappings from JSON file.
     */
    private void loadItemMappings() {
        try (InputStream stream = getResourceStream(ITEM_MAPPING_FILE);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            
            Type type = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
            Map<String, Map<String, Integer>> mappings = GSON.fromJson(reader, type);
            
            if (mappings == null) {
                LOGGER.warn("No item mappings found in {}", ITEM_MAPPING_FILE);
                return;
            }
            
            for (Map.Entry<String, Map<String, Integer>> entry : mappings.entrySet()) {
                String terraId = entry.getKey();
                RegistryKey terraKey = parseRegistryKey(terraId);
                
                if (terraKey == null) continue;
                
                Map<String, Integer> nukkitData = entry.getValue();
                Integer id = nukkitData.get("id");
                Integer data = nukkitData.get("data");
                
                if (id == null || data == null) {
                    LOGGER.warn("Missing id or data for item {}", terraId);
                    continue;
                }
                
                ItemMapping mapping = new ItemMapping(id, data);
                terraToNukkitItemMap.put(terraKey, mapping);
                nukkitToTerraItemMap.put(id + ":" + data, terraKey);
                
                // Create and cache item
                Item nukkitItem = Item.get(id, data);
                itemCache.put(terraKey, new NukkitItem(nukkitItem, terraKey));
            }
            
            LOGGER.info("Loaded {} item mappings", terraToNukkitItemMap.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load item mappings", e);
        }
    }

    /**
     * Load biome mappings from JSON file.
     */
    private void loadBiomeMappings() {
        try (InputStream stream = getResourceStream(BIOME_MAPPING_FILE);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> mappings = GSON.fromJson(reader, type);
            
            if (mappings == null) {
                LOGGER.warn("No biome mappings found in {}", BIOME_MAPPING_FILE);
                return;
            }
            
            for (Map.Entry<String, Integer> entry : mappings.entrySet()) {
                String terraId = entry.getKey();
                RegistryKey terraKey = RegistryKey.of("minecraft", terraId); // Biomes are usually in minecraft namespace
                Integer biomeId = entry.getValue();
                
                if (biomeId == null) {
                    LOGGER.warn("Missing biome ID for {}", terraId);
                    continue;
                }
                
                terraToBiomeIdMap.put(terraKey, biomeId);
                
                // Create and cache biome
                Biome nukkitBiome = Biome.getBiome(biomeId);
                if (nukkitBiome != null) {
                    NukkitBiome terraBiome = new NukkitBiome(nukkitBiome, terraKey);
                    biomeIdToTerraBiomeMap.put(biomeId, terraBiome);
                }
            }
            
            LOGGER.info("Loaded {} biome mappings", terraToBiomeIdMap.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load biome mappings", e);
        }
    }

    /**
     * Load entity mappings from JSON file.
     */
    private void loadEntityMappings() {
        try (InputStream stream = getResourceStream(ENTITY_MAPPING_FILE);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> mappings = GSON.fromJson(reader, type);
            
            if (mappings == null) {
                LOGGER.warn("No entity mappings found in {}", ENTITY_MAPPING_FILE);
                return;
            }
            
            for (Map.Entry<String, String> entry : mappings.entrySet()) {
                String terraId = entry.getKey();
                RegistryKey terraKey = parseRegistryKey(terraId);
                
                if (terraKey == null) continue;
                
                String nukkitId = entry.getValue();
                if (nukkitId == null || nukkitId.isEmpty()) {
                    LOGGER.warn("Missing entity ID for {}", terraId);
                    continue;
                }
                
                terraToNukkitEntityMap.put(terraKey, nukkitId);
                nukkitToTerraEntityMap.put(nukkitId, terraKey);
                
                // Create and cache entity type
                entityTypeCache.put(terraKey, new NukkitEntityType(terraKey, nukkitId));
            }
            
            LOGGER.info("Loaded {} entity mappings", terraToNukkitEntityMap.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load entity mappings", e);
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Parse a string into a RegistryKey.
     */
    @Nullable
    private RegistryKey parseRegistryKey(String key) {
        if (key == null || key.isEmpty()) return null;
        
        String[] parts = key.split(":", 2);
        if (parts.length == 2) {
            return RegistryKey.of(parts[0], parts[1]);
        } else {
            return RegistryKey.of("minecraft", parts[0]); // Assume minecraft namespace
        }
    }

    /**
     * Get a resource stream from the class loader.
     */
    private InputStream getResourceStream(String path) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        
        if (stream == null) {
            stream = ClassLoader.getSystemResourceAsStream(path);
        }
        
        if (stream == null) {
            throw new Exception("Resource not found: " + path);
        }
        
        return stream;
    }

    // ==================== Block Methods ====================

    /**
     * Get a Terra BlockState from a Nukkit block.
     */
    @NotNull
    public BlockState getBlockState(@NotNull Block nukkitBlock) {
        String key = nukkitBlock.getId() + ":" + nukkitBlock.getDamage();
        RegistryKey terraKey = nukkitToTerraBlockMap.get(key);
        
        if (terraKey == null) {
            // Try with damage 0 as fallback
            terraKey = nukkitToTerraBlockMap.get(nukkitBlock.getId() + ":0");
        }
        
        if (terraKey == null) {
            LOGGER.debug("No Terra mapping for Nukkit block {}", key);
            return airState;
        }
        
        // Create a JE block state for the Terra key
        NukkitJeBlockState jeBlockState = NukkitJeBlockState.fromString(terraKey.toString());
        return new NukkitBlockState(nukkitBlock, jeBlockState);
    }

    /**
     * Get a Nukkit block from a Terra BlockState.
     */
    @NotNull
    public Block getNukkitBlock(@NotNull BlockState terraState) {
        if (terraState instanceof NukkitBlockState) {
            return ((NukkitBlockState) terraState).getNukkitBlock();
        }
        
        // Try to get registry key through the Keyed interface if implemented
        RegistryKey terraKey = null;
        if (terraState instanceof com.dfsek.terra.api.registry.key.Keyed) {
            terraKey = ((com.dfsek.terra.api.registry.key.Keyed<?>) terraState).getRegistryKey();
        }
        
        if (terraKey == null) {
            // Last resort, try to parse from the string representation
            String stateString = terraState.getAsString(false);
            terraKey = parseRegistryKey(stateString);
        }
        
        if (terraKey == null) {
            LOGGER.debug("Unable to get registry key from BlockState {}", terraState);
            return Block.get(Block.AIR);
        }
        
        BlockMapping mapping = terraToNukkitBlockMap.get(terraKey);
        
        if (mapping == null) {
            LOGGER.debug("No Nukkit mapping for Terra block {}", terraKey);
            return Block.get(Block.AIR);
        }
        
        return Block.get(mapping.id, mapping.data);
    }

    /**
     * Get a Terra BlockType from a registry key.
     */
    @Nullable
    public BlockType getBlockType(@NotNull RegistryKey terraKey) {
        return blockTypeCache.get(terraKey);
    }

    /**
     * Get a Terra BlockType from a Terra BlockState.
     */
    @Nullable
    public BlockType getBlockType(@NotNull BlockState terraState) {
        if (terraState instanceof NukkitBlockState) {
            return ((NukkitBlockState) terraState).getBlockType();
        }
        
        // Try to get registry key through the Keyed interface if implemented
        RegistryKey key = null;
        if (terraState instanceof com.dfsek.terra.api.registry.key.Keyed) {
            key = ((com.dfsek.terra.api.registry.key.Keyed<?>) terraState).getRegistryKey();
            return getBlockType(key);
        }
        
        return null;
    }

    // ==================== Biome Methods ====================

    /**
     * Get a Terra biome from a Nukkit biome ID.
     */
    @NotNull
    public com.dfsek.terra.api.world.biome.PlatformBiome getBiome(int nukkitBiomeId) {
        NukkitBiome terraBiome = biomeIdToTerraBiomeMap.get(nukkitBiomeId);
        return terraBiome != null ? terraBiome : defaultBiome;
    }

    /**
     * Get a Nukkit biome ID from a Terra biome key.
     */
    public int getBiomeId(@NotNull RegistryKey terraKey) {
        return terraToBiomeIdMap.getOrDefault(terraKey, EnumBiome.PLAINS.id);
    }

    /**
     * Get a Terra platform biome from a Nukkit biome ID.
     */
    @Nullable
    public NukkitPlatformBiome getPlatformBiome(int nukkitBiomeId) {
        return nukkitIdToPlatformBiome.getOrDefault(nukkitBiomeId, nukkitIdToPlatformBiome.get(EnumBiome.PLAINS.id));
    }

    /**
     * Get the default biome.
     */
    @NotNull
    public NukkitBiome getDefaultBiome() {
        return defaultBiome;
    }

    // ==================== Entity Methods ====================

    /**
     * Get a Terra EntityType from a Nukkit entity ID.
     */
    @NotNull
    public EntityType getEntityType(@NotNull String nukkitEntityId) {
        RegistryKey terraKey = nukkitToTerraEntityMap.get(nukkitEntityId);
        
        if (terraKey == null) {
            return defaultEntityType;
        }
        
        NukkitEntityType entityType = entityTypeCache.get(terraKey);
        return entityType != null ? entityType : defaultEntityType;
    }

    /**
     * Get a Nukkit entity ID from a Terra EntityType.
     */
    @Nullable
    public String getNukkitEntityId(@NotNull EntityType terraEntityType) {
        if (terraEntityType instanceof NukkitEntityType) {
            return ((NukkitEntityType) terraEntityType).getNukkitId();
        }
        
        try {
            if (terraEntityType instanceof com.dfsek.terra.api.registry.key.Keyed) {
                RegistryKey terraKey = ((com.dfsek.terra.api.registry.key.Keyed<?>) terraEntityType).getRegistryKey();
                return terraToNukkitEntityMap.get(terraKey);
            }
        } catch (Exception e) {
            // Silently fail and return null
        }
        
        return null;
    }

    /**
     * Get the default entity type.
     */
    @NotNull
    public NukkitEntityType getDefaultEntityType() {
        return defaultEntityType;
    }

    // ==================== Item Methods ====================

    /**
     * Get a Terra Item from a Nukkit item.
     */
    @NotNull
    public com.dfsek.terra.api.inventory.Item getItem(@NotNull Item nukkitItem) {
        String key = nukkitItem.getId() + ":" + nukkitItem.getDamage();
        RegistryKey terraKey = nukkitToTerraItemMap.get(key);
        
        if (terraKey == null) {
            // Try with damage 0 as fallback
            terraKey = nukkitToTerraItemMap.get(nukkitItem.getId() + ":0");
        }
        
        if (terraKey == null) {
            // Return air item as fallback
            RegistryKey airKey = RegistryKey.of("minecraft", "air");
            NukkitItem airItem = itemCache.get(airKey);
            if (airItem == null) {
                // Create air item if not cached
                airItem = new NukkitItem(Item.get(Item.AIR), airKey);
                itemCache.put(airKey, airItem);
            }
            return airItem;
        }
        
        NukkitItem terraItem = itemCache.get(terraKey);
        if (terraItem == null) {
            // Create item if not cached
            terraItem = new NukkitItem(nukkitItem, terraKey);
            itemCache.put(terraKey, terraItem);
        }
        
        return terraItem;
    }

    /**
     * Get a Nukkit item from a Terra Item key.
     */
    @NotNull
    public Item getNukkitItem(@NotNull RegistryKey terraKey) {
        ItemMapping mapping = terraToNukkitItemMap.get(terraKey);
        
        if (mapping == null) {
            return Item.get(Item.AIR);
        }
        
        return Item.get(mapping.id, mapping.data);
    }

    // ==================== Helper Classes ====================

    /**
     * Simple mapping class for blocks.
     */
    private static class BlockMapping {
        final int id;
        final int data;
        
        BlockMapping(int id, int data) {
            this.id = id;
            this.data = data;
        }
    }

    /**
     * Simple mapping class for items.
     */
    private static class ItemMapping {
        final int id;
        final int data;
        
        ItemMapping(int id, int data) {
            this.id = id;
            this.data = data;
        }
    }

    /**
     * Initialize the registry with the plugin instance.
     */
    public void initialize(TerraNukkitPlugin plugin) {
        // This method is kept for compatibility
        LOGGER.info("NukkitMappingRegistry initialized with {} blocks, {} items, {} biomes, {} entities",
                terraToNukkitBlockMap.size(), terraToNukkitItemMap.size(), 
                terraToBiomeIdMap.size(), terraToNukkitEntityMap.size());
    }
} 