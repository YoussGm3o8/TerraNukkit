package com.dfsek.terra.nukkit.block;

import cn.nukkit.block.Block;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Mapping utility for converting between Java Edition and Nukkit block states.
 */
public class NukkitMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitMapping.class);
    private static final Map<String, Block> BLOCK_CACHE = new HashMap<>();
    private static boolean FALLBACK_ENABLED = true; // Set to true to enable fallback mappings
    
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapterFactory(new IgnoreFailureTypeAdapterFactory())
        .create();

    private static final Map<String, Map<String, String>> JE_BLOCK_DEFAULT_PROPERTIES = new HashMap<>();
    private static final Map<Integer, Block> JE_BLOCK_STATE_HASH_TO_NUKKIT = new HashMap<>();
    private static final Map<Block, NukkitJeBlockState> NUKKIT_TO_JE_BLOCK_STATE = new HashMap<>();

    // Fallback block for completely unknown blocks
    private static final Block FALLBACK_BLOCK = Block.get(Block.DIAMOND_BLOCK);

    /**
     * Convert a Java Edition block state string to a Nukkit block.
     * 
     * @param jeBlockState Java Edition block state
     * @return Nukkit block
     */
    public static Block blockStateJeToNukkit(NukkitJeBlockState jeBlockState) {
        // First, check mapping using the hash code (for exact matches with properties)
        Block block = JE_BLOCK_STATE_HASH_TO_NUKKIT.get(jeBlockState.getHash());
        if (block != null) {
            return block.clone();
        }
        
        // Next, try to get by just the identifier (without properties)
        String id = jeBlockState.identifier;
        
        // Remove minecraft: prefix if present
        if (id.startsWith("minecraft:")) {
            id = id.substring(10);
        }
        
        // Check cache first for better performance
        if (BLOCK_CACHE.containsKey(id)) {
            Block cachedBlock = BLOCK_CACHE.get(id);
            if (cachedBlock != null) {
                return applyProperties(cachedBlock.clone(), jeBlockState);
            }
        }
        
        // If not in mapping, try to get by name
        try {
            block = getBlockByCommonName(id);
        } catch (Exception e) {
            LOGGER.warn("Error getting block by name: {}", id, e);
        }
        
        // Log any unknown blocks we couldn't map directly
        if (block == null) {
            LOGGER.debug("No direct mapping for block: {}", id);
        }
        
        if (block == null && FALLBACK_ENABLED) {
            // Apply intelligent fallbacks for unknown blocks
            block = getFallbackBlock(id);
        }
        
        if (block == null) {
            LOGGER.warn("Failed to map block state: {}", jeBlockState);
            LOGGER.warn("CRITICAL: Failed to map block: {} - using fallback block", id);
            block = FALLBACK_BLOCK; // Last resort fallback
        }
        
        // Add to cache for future lookups
        BLOCK_CACHE.put(id, block);
        
        return applyProperties(block.clone(), jeBlockState);
    }
    
    /**
     * Apply properties to a block clone.
     */
    private static Block applyProperties(Block block, NukkitJeBlockState jeBlockState) {
        // Waterlogged blocks
        if ("true".equals(jeBlockState.getPropertyValue("waterlogged"))) {
            // Nukkit doesn't support waterlogging directly
            LOGGER.debug("Waterlogged property detected but not supported in Nukkit: {}", jeBlockState);
        }
        
        // For blocks where specific properties can be mapped to damage values
        applyCommonProperties(block, jeBlockState);
        
        return block;
    }
    
    /**
     * Get a fallback block for unknown block types based on naming patterns.
     */
    private static Block getFallbackBlock(String name) {
        // Print warning about using fallback
        LOGGER.info("Using fallback mapping for block: {}", name);
        
        // Handle common prefixes and suffixes
        if (name.endsWith("_log") || name.endsWith("_wood")) {
            return Block.get(Block.WOOD); // Wood/log fallback
        } else if (name.endsWith("_leaves")) {
            return Block.get(Block.LEAVES); // Leaves fallback
        } else if (name.endsWith("_stairs")) {
            return Block.get(Block.COBBLESTONE_STAIRS); // Stairs fallback
        } else if (name.endsWith("_slab") || name.contains("_slab_")) {
            return Block.get(Block.STONE_SLAB); // Slab fallback
        } else if (name.endsWith("_fence")) {
            return Block.get(Block.FENCE); // Fence fallback
        } else if (name.endsWith("_wall")) {
            return Block.get(Block.COBBLESTONE_WALL); // Wall fallback
        } else if (name.contains("_door")) {
            return Block.get(Block.WOODEN_DOOR_BLOCK); // Door fallback
        } else if (name.contains("_button")) {
            return Block.get(Block.WOODEN_BUTTON); // Button fallback
        } else if (name.contains("_pressure_plate")) {
            return Block.get(Block.WOODEN_PRESSURE_PLATE); // Pressure plate fallback
        } else if (name.contains("_trapdoor")) {
            return Block.get(Block.TRAPDOOR); // Trapdoor fallback
        } else if (name.contains("_sign")) {
            return Block.get(Block.SIGN_POST); // Sign fallback
        } else if (name.contains("_carpet")) {
            return Block.get(Block.CARPET); // Carpet fallback
        } else if (name.contains("_bed")) {
            return Block.get(Block.BED_BLOCK); // Bed fallback
        } else if (name.contains("_sapling")) {
            return Block.get(Block.SAPLING); // Sapling fallback
        } else if (name.startsWith("potted_")) {
            return Block.get(Block.FLOWER_POT_BLOCK); // Flower pot fallback
        } else if (name.endsWith("_ore")) {
            return Block.get(Block.DIAMOND_BLOCK); // Ore fallback shows as diamond block
        } else if (name.endsWith("_terracotta")) {
            return Block.get(Block.TERRACOTTA); // Terracotta fallback
        } else if (name.endsWith("_coral") || name.endsWith("_fan")) {
            return Block.get(Block.AIR); // Coral fallbacks to air (not well supported in Nukkit)
        } else if (name.contains("_plant") || name.contains("_bush") || 
                   name.endsWith("_fern") || name.endsWith("_rose") || 
                   name.endsWith("_tulip") || name.endsWith("_orchid") || 
                   name.equals("grass") || name.equals("tall_grass") || 
                   name.equals("seagrass") || name.equals("kelp") ||
                   name.equals("vine") || name.equals("lily_pad") ||
                   name.endsWith("_fungus") || name.equals("sugar_cane")) {
            return Block.get(Block.TALL_GRASS); // Plant/vegetation fallback
        } else if (name.endsWith("_mushroom") || name.equals("mushroom_stem") || 
                   name.endsWith("_mushroom_block")) {
            return Block.get(Block.BROWN_MUSHROOM_BLOCK); // Mushroom fallback
        }
        
        // Final fallback is the fallback block (diamond)
        LOGGER.info("Using generic fallback block for: {}", name);
        return FALLBACK_BLOCK;
    }

    /**
     * Helper method to get a block by common name.
     */
    private static Block getBlockByCommonName(String name) {
        // This mapping covers common base blocks - will be supplemented by JSON mappings
        switch (name.toLowerCase()) {
            // Basic blocks
            case "stone": return Block.get(Block.STONE);
            case "grass_block": return Block.get(Block.GRASS);
            case "dirt": return Block.get(Block.DIRT);
            case "cobblestone": return Block.get(Block.COBBLESTONE);
            case "oak_planks": case "planks": return Block.get(Block.PLANKS);
            case "bedrock": return Block.get(Block.BEDROCK);
            case "flowing_water": case "water": return Block.get(Block.WATER);
            case "flowing_lava": case "lava": return Block.get(Block.LAVA);
            case "sand": return Block.get(Block.SAND);
            case "gravel": return Block.get(Block.GRAVEL);
            case "oak_log": case "log": return Block.get(Block.LOG);
            case "oak_leaves": case "leaves": return Block.get(Block.LEAVES);
            case "glass": return Block.get(Block.GLASS);
            case "oak_stairs": case "wooden_stairs": return Block.get(Block.OAK_WOODEN_STAIRS);
            case "chest": return Block.get(Block.CHEST);
            case "crafting_table": return Block.get(Block.CRAFTING_TABLE);
            case "furnace": return Block.get(Block.FURNACE);
            case "air": case "cave_air": case "void_air": return Block.get(Block.AIR);
            
            // We'll mostly use JSON mappings, so just log and return null for others
            default:
                LOGGER.debug("No direct mapping for block: {}", name);
                return null;
        }
    }
    
    /**
     * Convert Nukkit block to Java Edition block state.
     * 
     * @param block Nukkit block
     * @return Java Edition block state
     */
    public static NukkitJeBlockState blockStateNukkitToJe(Block block) {
        // Check cache first
        NukkitJeBlockState cached = NUKKIT_TO_JE_BLOCK_STATE.get(block);
        if (cached != null) {
            return cached;
        }
        
        String name = block.getName().toLowerCase().replace(" ", "_");
        
        // Use classic Minecraft namespace
        String identifier = "minecraft:" + name;
        
        // Create base block state
        TreeMap<String, String> properties = new TreeMap<>();
        
        // Add properties based on block damage value where applicable
        addPropertiesFromDamage(block, properties);
        
        NukkitJeBlockState result = NukkitJeBlockState.create(identifier, properties);
        NUKKIT_TO_JE_BLOCK_STATE.put(block, result);
        return result;
    }
    
    /**
     * Apply common Java Edition properties to a Nukkit block.
     */
    private static void applyCommonProperties(Block block, NukkitJeBlockState jeBlockState) {
        // Apply properties that map to damage values in Nukkit
        
        // Handle facing direction for applicable blocks
        String facing = jeBlockState.getPropertyValue("facing");
        if (facing != null) {
            int damageValue = mapFacingToDamage(block.getId(), facing);
            if (damageValue >= 0) {
                block.setDamage(damageValue);
            }
        }
        
        // Handle half property for slabs and stairs
        String half = jeBlockState.getPropertyValue("half");
        if (half != null && (isStairs(block.getId()) || isSlab(block.getId()))) {
            if ("top".equals(half)) {
                // For stairs, this needs to be combined with facing
                if (isStairs(block.getId())) {
                    block.setDamage(block.getDamage() | 0x4); // Set the 4-bit for top half
                } else {
                    block.setDamage(block.getDamage() | 0x8); // Set the 8-bit for top half slabs
                }
            }
        }
        
        // Handle axis property for logs
        String axis = jeBlockState.getPropertyValue("axis");
        if (axis != null && isLog(block.getId())) {
            int damage = block.getDamage() & 0x3; // Keep the wood type (first 2 bits)
            if ("y".equals(axis)) {
                // No change needed for y-axis (0)
            } else if ("x".equals(axis)) {
                damage |= 0x4; // Set bits for east-west (4)
            } else if ("z".equals(axis)) {
                damage |= 0x8; // Set bits for north-south (8)
            } else if ("none".equals(axis)) {
                damage |= 0xC; // Set bits for bark block (12)
            }
            block.setDamage(damage);
        }
    }
    
    private static boolean isStairs(int id) {
        return id == Block.OAK_WOODEN_STAIRS || id == Block.COBBLESTONE_STAIRS || 
               id == Block.BRICK_STAIRS || id == Block.STONE_BRICK_STAIRS ||
               id == Block.NETHER_BRICKS_STAIRS || id == Block.SANDSTONE_STAIRS ||
               id == Block.SPRUCE_WOOD_STAIRS || id == Block.BIRCH_WOOD_STAIRS ||
               id == Block.JUNGLE_WOOD_STAIRS || id == Block.QUARTZ_STAIRS ||
               id == Block.ACACIA_WOOD_STAIRS || id == Block.DARK_OAK_WOOD_STAIRS ||
               id == Block.RED_SANDSTONE_STAIRS;
    }
    
    private static boolean isSlab(int id) {
        return id == Block.STONE_SLAB || id == Block.WOODEN_SLAB || 
               id == Block.DOUBLE_SLAB || id == Block.DOUBLE_WOODEN_SLAB;
    }
    
    private static boolean isLog(int id) {
        return id == Block.LOG || id == Block.LOG2 || id == Block.WOOD || id == Block.WOOD2;
    }
    
    /**
     * Add Java Edition properties based on a Nukkit block's damage value.
     */
    private static void addPropertiesFromDamage(Block block, TreeMap<String, String> properties) {
        int id = block.getId();
        int damage = block.getDamage();
        
        // Logs - add axis property
        if (isLog(id)) {
            int axisValue = damage & 0xC; // Get bits 3-4 for axis
            String axis = "y"; // Default Y
            
            if (axisValue == 0x4) {
                axis = "x"; // East-West
            } else if (axisValue == 0x8) {
                axis = "z"; // North-South
            } else if (axisValue == 0xC) {
                axis = "none"; // Bark
            }
            
            properties.put("axis", axis);
        }
        
        // Stairs - add facing and half properties
        else if (isStairs(id)) {
            // Extract facing from bits 0-1
            int facingValue = damage & 0x3;
            String facing = "north"; // Default
            
            switch (facingValue) {
                case 0: facing = "east"; break;
                case 1: facing = "west"; break;
                case 2: facing = "south"; break;
                case 3: facing = "north"; break;
            }
            
            properties.put("facing", facing);
            
            // Extract half from bit 2
            boolean isTop = (damage & 0x4) != 0;
            properties.put("half", isTop ? "top" : "bottom");
        }
        
        // Slabs - add half property (bit 3)
        else if (isSlab(id)) {
            boolean isTop = (damage & 0x8) != 0;
            properties.put("type", isTop ? "top" : "bottom");
        }
        
        // Furnaces, dispensers, etc. - add facing property
        else if (id == Block.FURNACE || id == Block.BURNING_FURNACE || 
                id == Block.DISPENSER || id == Block.DROPPER) {
            int facingValue = damage & 0x7;
            String facing = "north"; // Default
            
            switch (facingValue) {
                case 2: facing = "north"; break;
                case 3: facing = "south"; break;
                case 4: facing = "west"; break;
                case 5: facing = "east"; break;
                case 0: facing = "down"; break;
                case 1: facing = "up"; break;
            }
            
            properties.put("facing", facing);
        }
    }
    
    /**
     * Map a facing direction to a damage value for applicable blocks.
     */
    private static int mapFacingToDamage(int blockId, String facing) {
        // Different block types use damage values differently
        
        // Directional blocks like furnaces
        if (blockId == Block.FURNACE || blockId == Block.BURNING_FURNACE || 
            blockId == Block.DISPENSER || blockId == Block.DROPPER) {
            if ("north".equals(facing)) return 2;
            if ("south".equals(facing)) return 3;
            if ("west".equals(facing)) return 4;
            if ("east".equals(facing)) return 5;
            if ("down".equals(facing)) return 0;
            if ("up".equals(facing)) return 1;
        }
        
        // Stairs
        else if (isStairs(blockId)) {
            if ("east".equals(facing)) return 0;
            if ("west".equals(facing)) return 1;
            if ("south".equals(facing)) return 2;
            if ("north".equals(facing)) return 3;
        }
        
        return -1; // No mapping found
    }
    
    /**
     * Get default properties for a JE block type.
     * 
     * @param blockIdentifier The block identifier (e.g. minecraft:stone)
     * @return Map of default properties, empty if none
     */
    public static Map<String, String> getJeBlockDefaultProperties(String blockIdentifier) {
        Map<String, String> result = JE_BLOCK_DEFAULT_PROPERTIES.get(blockIdentifier);
        return result != null ? result : new HashMap<>();
    }

    /**
     * Initialize the mapping system by loading JSON files
     */
    public static void init() {
        LOGGER.info("Initializing Nukkit block mappings");
        setFallbackEnabled(true);
        
        try {
            loadDefaultBlockProperties();
            loadBlockMappings();
        } catch (IOException e) {
            LOGGER.error("Failed to load block mappings", e);
        }
    }
    
    /**
     * Load default properties for Java Edition blocks from JSON
     */
    private static void loadDefaultBlockProperties() throws IOException {
        try (InputStream stream = NukkitMapping.class.getClassLoader().getResourceAsStream("je_block_default_states.json")) {
            if (stream == null) {
                LOGGER.warn("je_block_default_states.json not found, using empty default properties");
                return;
            }
            
            Map<String, Map<String, String>> properties = fromJson(stream, new TypeToken<Map<String, Map<String, String>>>() {});
            JE_BLOCK_DEFAULT_PROPERTIES.putAll(properties);
            LOGGER.info("Loaded {} default block properties", properties.size());
        }
    }
    
    /**
     * Load block mappings from JSON
     */
    private static void loadBlockMappings() throws IOException {
        try (InputStream stream = NukkitMapping.class.getClassLoader().getResourceAsStream("nukkit_mappings/blocks.json")) {
            if (stream == null) {
                LOGGER.warn("nukkit_mappings/blocks.json not found, using fallback mappings only");
                return;
            }
            
            Map<String, BlockMappingData> mappings = fromJson(stream, new TypeToken<Map<String, BlockMappingData>>() {});
            if (mappings == null) {
                LOGGER.warn("Invalid blocks.json format");
                return;
            }
            
            int mappingCount = 0;
            // Process each mapping
            for (Map.Entry<String, BlockMappingData> entry : mappings.entrySet()) {
                try {
                    String jeId = entry.getKey();
                    BlockMappingData data = entry.getValue();
                    
                    // Handle basic mapping without states
                    Block baseBlock = Block.get(data.id, data.default_data);
                    
                    // Create and cache a JE block state for this base block
                    NukkitJeBlockState jeBaseState = NukkitJeBlockState.create(jeId, new TreeMap<>());
                    JE_BLOCK_STATE_HASH_TO_NUKKIT.put(jeBaseState.getHash(), baseBlock);
                    NUKKIT_TO_JE_BLOCK_STATE.put(baseBlock, jeBaseState);
                    
                    // Also add to block cache for faster lookups
                    String id = jeId;
                    if (id.startsWith("minecraft:")) {
                        id = id.substring(10);
                    }
                    BLOCK_CACHE.put(id, baseBlock);
                    mappingCount++;
                    
                    // Process states if available
                    if (data.states != null && !data.states.isEmpty()) {
                        for (StateMapping stateMapping : data.states) {
                            if (stateMapping.properties != null && stateMapping.data >= 0) {
                                // Create block with the specific data value
                                Block stateBlock = Block.get(data.id, stateMapping.data);
                                
                                // Create a JE block state with these properties
                                NukkitJeBlockState jeStateBlock = NukkitJeBlockState.create(
                                    jeId, new TreeMap<>(stateMapping.properties));
                                    
                                // Add to mappings
                                JE_BLOCK_STATE_HASH_TO_NUKKIT.put(jeStateBlock.getHash(), stateBlock);
                                NUKKIT_TO_JE_BLOCK_STATE.put(stateBlock, jeStateBlock);
                                mappingCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to process mapping entry: {}", entry.getKey(), e);
                }
            }
            
            LOGGER.info("Loaded {} block mappings from JSON", mappingCount);
        }
    }
    
    /**
     * Enable or disable fallback mappings.
     */
    public static void setFallbackEnabled(boolean enabled) {
        FALLBACK_ENABLED = enabled;
        if (enabled) {
            LOGGER.info("Fallback block mappings are enabled");
        } else {
            LOGGER.info("Fallback block mappings are disabled");
        }
    }
    
    private static <T> T fromJson(InputStream inputStream, TypeToken<T> typeToken) {
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
            return GSON.fromJson(reader, typeToken.getType());
        } catch (Exception e) {
            LOGGER.error("Error parsing JSON", e);
            return null;
        }
    }
    
    // See https://stackoverflow.com/questions/59655279
    public static class IgnoreFailureTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, typeToken);
            return new TypeAdapter<>() {
                @Override
                public void write(JsonWriter writer, T value) throws IOException {
                    delegate.write(writer, value);
                }

                @Override
                public T read(JsonReader reader) throws IOException {
                    try {
                        return delegate.read(reader);
                    } catch(Exception e) {
                        reader.skipValue();
                        return null;
                    }
                }
            };
        }
    }
    
    // Mapping classes for JSON deserialization
    private static class BlockMappingData {
        int id;
        int default_data;
        List<StateMapping> states;
    }
    
    private static class StateMapping {
        Map<String, String> properties;
        int data;
    }
} 