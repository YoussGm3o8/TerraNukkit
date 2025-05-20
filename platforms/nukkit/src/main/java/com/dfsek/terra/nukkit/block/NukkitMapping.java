package com.dfsek.terra.nukkit.block;

import cn.nukkit.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mapping utility for converting between Java Edition and Nukkit block states.
 */
public class NukkitMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(NukkitMapping.class);
    private static final Map<String, Block> BLOCK_CACHE = new HashMap<>();
    private static boolean FALLBACK_ENABLED = true; // Set to true to enable fallback mappings
    
    /**
     * Convert a Java Edition block state string to a Nukkit block.
     * 
     * @param jeBlockState Java Edition block state
     * @return Nukkit block
     */
    public static Block blockStateJeToNukkit(NukkitJeBlockState jeBlockState) {
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
        
        // Map the ID to Nukkit blocks - try by name first
        Block block = null;
        try {
            // Try to get the block by name from Nukkit
            block = getBlockByCommonName(id);
        } catch (Exception e) {
            LOGGER.warn("Error getting block by name: {}", id, e);
        }
        
        if (block == null && FALLBACK_ENABLED) {
            // Apply intelligent fallbacks for unknown blocks
            block = getFallbackBlock(id);
        }
        
        if (block == null) {
            LOGGER.warn("Failed to map block state: {}", jeBlockState);
            block = Block.get(Block.AIR); // Last resort fallback is air
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
            return Block.get(Block.DIAMOND_BLOCK); // Ore fallback now shows as diamond block
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
        } else if (name.equals("tuff") || name.equals("calcite") || name.equals("smooth_basalt")) {
            return Block.get(Block.DIAMOND_BLOCK); // Stone-like blocks now show as diamond
        } else if (name.startsWith("flowering_") || name.equals("azalea") || 
                   name.equals("spore_blossom")) {
            return Block.get(Block.LEAVES); // Flowering blocks fallback to leaves
        } else if (name.endsWith("crops") || name.equals("wheat") || 
                   name.equals("carrots") || name.equals("potatoes") || 
                   name.equals("beetroots") || name.equals("melon_stem") || 
                   name.equals("pumpkin_stem")) {
            return Block.get(Block.WHEAT_BLOCK); // Crops fallback
        } else if (name.equals("snow") || name.equals("snow_block") || name.equals("powder_snow")) {
            return Block.get(Block.SNOW_BLOCK); // Snow fallback
        } else if (name.equals("packed_ice") || name.equals("blue_ice") || 
                   name.equals("frosted_ice")) {
            return Block.get(Block.ICE); // Ice variants fallback
        } else if (name.equals("air") || name.equals("cave_air") || name.equals("void_air")) {
            return Block.get(Block.AIR); // Air variants
        }
        
        // Generic fallbacks based on material type - now using diamond blocks for visibility
        if (name.contains("granite") || name.contains("diorite") || name.contains("andesite") ||
            name.contains("deepslate") || name.contains("blackstone") || name.contains("basalt")) {
            return Block.get(Block.DIAMOND_BLOCK); // Stone variants now show as diamond
        } else if (name.contains("cherry") || name.contains("bamboo") || name.contains("mangrove")) {
            // Newer wood types fallback to generic wood blocks
            if (name.contains("log")) return Block.get(Block.LOG);
            if (name.contains("plank")) return Block.get(Block.PLANKS);
            if (name.contains("leave")) return Block.get(Block.LEAVES);
            return Block.get(Block.PLANKS); // Default to planks for other wood items
        }
        
        // Last category fallbacks for common materials - changed to diamond blocks
        if (name.contains("stone")) {
            return Block.get(Block.DIAMOND_BLOCK);
        } else if (name.contains("dirt")) {
            return Block.get(Block.DIRT);
        } else if (name.contains("sand")) {
            return Block.get(Block.SAND);
        }
        
        // Final fallback is now diamond blocks
        LOGGER.debug("No specific fallback mapping for: {}. Using diamond block for visibility.", name);
        return Block.get(Block.DIAMOND_BLOCK);
    }

    /**
     * Helper method to get a block by common name since Nukkit doesn't have a direct method.
     */
    private static Block getBlockByCommonName(String name) {
        // This mapping covers common base blocks
        switch (name.toLowerCase()) {
            // Basic blocks
            case "stone": return Block.get(Block.STONE);
            case "grass_block": return Block.get(Block.GRASS);
            case "dirt": return Block.get(Block.DIRT);
            case "cobblestone": return Block.get(Block.COBBLESTONE);
            case "oak_planks": case "planks": return Block.get(Block.PLANKS);
            case "oak_sapling": case "sapling": return Block.get(Block.SAPLING);
            case "bedrock": return Block.get(Block.BEDROCK);
            case "flowing_water": case "water": return Block.get(Block.WATER);
            case "still_water": return Block.get(Block.STILL_WATER);
            case "flowing_lava": case "lava": return Block.get(Block.LAVA);
            case "still_lava": return Block.get(Block.STILL_LAVA);
            case "sand": return Block.get(Block.SAND);
            case "red_sand": return Block.get(Block.SAND, 1);
            case "gravel": return Block.get(Block.GRAVEL);
            case "gold_ore": return Block.get(Block.GOLD_ORE);
            case "iron_ore": return Block.get(Block.IRON_ORE);
            case "coal_ore": return Block.get(Block.COAL_ORE);
            case "oak_log": case "log": return Block.get(Block.LOG);
            case "oak_leaves": case "leaves": return Block.get(Block.LEAVES);
            case "glass": return Block.get(Block.GLASS);
            case "oak_stairs": case "wooden_stairs": return Block.get(Block.OAK_WOODEN_STAIRS);
            case "chest": return Block.get(Block.CHEST);
            case "crafting_table": return Block.get(Block.CRAFTING_TABLE);
            case "furnace": return Block.get(Block.FURNACE);
            case "oak_door": case "wooden_door": return Block.get(Block.WOODEN_DOOR_BLOCK);
            case "ladder": return Block.get(Block.LADDER);
            case "cobblestone_stairs": return Block.get(Block.COBBLESTONE_STAIRS);
            case "snow": case "snow_layer": return Block.get(Block.SNOW_LAYER);
            case "ice": return Block.get(Block.ICE);
            case "snow_block": return Block.get(Block.SNOW_BLOCK);
            case "cactus": return Block.get(Block.CACTUS);
            case "clay": return Block.get(Block.CLAY_BLOCK);
            case "sugar_cane": return Block.get(Block.SUGARCANE_BLOCK);
            case "fence": case "oak_fence": return Block.get(Block.FENCE);
            case "pumpkin": return Block.get(Block.PUMPKIN);
            case "netherrack": return Block.get(Block.NETHERRACK);
            case "soul_sand": return Block.get(Block.SOUL_SAND);
            case "glowstone": return Block.get(Block.GLOWSTONE_BLOCK);
            case "white_wool": case "wool": return Block.get(Block.WOOL);
            case "tnt": return Block.get(Block.TNT);
            case "bookshelf": return Block.get(Block.BOOKSHELF);
            case "mossy_cobblestone": return Block.get(Block.MOSS_STONE);
            case "obsidian": return Block.get(Block.OBSIDIAN);
            case "oak_slab": case "wooden_slab": return Block.get(Block.WOODEN_SLAB);
            case "cobblestone_slab": return Block.get(Block.STONE_SLAB);
            case "sandstone": return Block.get(Block.SANDSTONE);
            case "terracotta": return Block.get(Block.TERRACOTTA);
            case "air": case "cave_air": case "void_air": return Block.get(Block.AIR);
            
            // Modern blocks (1.13+)
            case "deepslate": return Block.get(Block.STONE, 3); // Using diorite as closest visual match
            case "calcite": return Block.get(Block.QUARTZ_BLOCK);
            case "tuff": return Block.get(Block.STONE, 5); // Using andesite as closest visual match
            case "copper_ore": return Block.get(Block.GOLD_ORE);
            case "copper_block": return Block.get(Block.GOLD_BLOCK);
            case "raw_copper_block": return Block.get(Block.GOLD_BLOCK);
            case "raw_iron_block": return Block.get(Block.IRON_BLOCK);
            case "raw_gold_block": return Block.get(Block.GOLD_BLOCK);
            case "amethyst_block": return Block.get(Block.PURPUR_BLOCK);
            case "amethyst_cluster": return Block.get(Block.END_ROD);
            case "sculk": case "sculk_sensor": return Block.get(Block.WOOL, 11); // Blue wool
            case "moss_block": return Block.get(Block.MOSS_STONE);
            case "smooth_basalt": return Block.get(Block.STONE, 6);
            case "deepslate_coal_ore": return Block.get(Block.COAL_ORE);
            case "deepslate_iron_ore": return Block.get(Block.IRON_ORE);
            case "deepslate_gold_ore": return Block.get(Block.GOLD_ORE);
            case "deepslate_redstone_ore": return Block.get(Block.REDSTONE_ORE);
            case "deepslate_diamond_ore": return Block.get(Block.DIAMOND_ORE);
            case "deepslate_lapis_ore": return Block.get(Block.LAPIS_ORE);
            case "deepslate_emerald_ore": return Block.get(Block.EMERALD_ORE);
            case "powder_snow": return Block.get(Block.SNOW_BLOCK);

            // Log a message and return null for debugging
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
        String name = block.getName().toLowerCase().replace(" ", "_");
        
        // Use classic Minecraft namespace
        String identifier = "minecraft:" + name;
        
        // Create base block state
        TreeMap<String, String> properties = new TreeMap<>();
        
        // Add properties based on block damage value where applicable
        addPropertiesFromDamage(block, properties);
        
        return NukkitJeBlockState.create(identifier, properties);
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
    
    /**
     * Add Java Edition properties based on a Nukkit block's damage value.
     */
    private static void addPropertiesFromDamage(Block block, TreeMap<String, String> properties) {
        // This would be implemented based on Nukkit's block ID and damage value system
        // For example, for logs, the damage value indicates the wood type and orientation
        
        // For simplicity, we're not implementing full property mapping now
        // This would be expanded with specific block cases
    }
    
    /**
     * Map a facing direction to a damage value for applicable blocks.
     */
    private static int mapFacingToDamage(int blockId, String facing) {
        // This is a simplified example that would need proper implementation
        // Different block types use damage values differently
        
        switch (blockId) {
            // Example for directional blocks like furnaces
            case Block.FURNACE:
            case Block.BURNING_FURNACE:
                if ("north".equals(facing)) return 2;
                if ("south".equals(facing)) return 3;
                if ("west".equals(facing)) return 4;
                if ("east".equals(facing)) return 5;
                break;
            
            // Example for stairs
            case Block.OAK_WOODEN_STAIRS:
            case Block.COBBLESTONE_STAIRS:
            case Block.BRICK_STAIRS:
            case Block.STONE_BRICK_STAIRS:
            case Block.NETHER_BRICKS_STAIRS:
            case Block.SANDSTONE_STAIRS:
            case Block.SPRUCE_WOOD_STAIRS:
            case Block.BIRCH_WOOD_STAIRS:
            case Block.JUNGLE_WOOD_STAIRS:
            case Block.QUARTZ_STAIRS:
            case Block.ACACIA_WOOD_STAIRS:
            case Block.DARK_OAK_WOOD_STAIRS:
            case Block.RED_SANDSTONE_STAIRS:
                if ("east".equals(facing)) return 0;
                if ("west".equals(facing)) return 1;
                if ("south".equals(facing)) return 2;
                if ("north".equals(facing)) return 3;
                break;
        }
        
        return -1; // No mapping found
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
    
    /**
     * Initialize the mappings and enable fallbacks by default.
     */
    public static void init() {
        LOGGER.info("Initializing Nukkit block mappings");
        setFallbackEnabled(true);
    }
} 