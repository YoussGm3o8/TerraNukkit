package com.dfsek.terra.nukkit.commands;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.Command;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.scheduler.AsyncTask;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;
import com.dfsek.terra.nukkit.test.PerformanceTest;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;

/**
 * Commands for Terra Nukkit implementation.
 * Uses native Nukkit command system rather than Cloud command framework.
 */
public class NukkitCommands {
    private final Platform platform;
    
    public NukkitCommands(Platform platform) {
        this.platform = platform;
    }
    
    /**
     * Register commands with Nukkit.
     */
    public void register() {
        TerraNukkitPlugin plugin = TerraNukkitPlugin.INSTANCE;
        
        // Create and register the base /terra command
        PluginCommand<TerraNukkitPlugin> terraCommand = new PluginCommand<>("terra", plugin);
        terraCommand.setDescription("Terra world generation plugin commands");
        terraCommand.setUsage("/terra <help|reload|version|create|test>");
        terraCommand.setAliases(new String[]{"t"});
        terraCommand.setPermission("terra.command");
        
        // Set the command executor and tab completer
        terraCommand.setExecutor(new TerraCommandExecutor());
        
        // Register with Nukkit
        plugin.getServer().getCommandMap().register("terra", terraCommand);
    }
    
    /**
     * Command executor implementation for Terra commands.
     */
    private class TerraCommandExecutor implements cn.nukkit.command.CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }
            
            switch (args[0].toLowerCase()) {
                case "help" -> sendHelp(sender);
                case "reload" -> handleReload(sender);
                case "version" -> handleVersion(sender);
                case "create" -> handleCreate(sender, args);
                case "test" -> handleTest(sender, args);
                default -> {
                    sender.sendMessage(TextFormat.RED + "Unknown Terra command: " + args[0]);
                    sendHelp(sender);
                }
            }
            
            return true;
        }
        
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                // Complete subcommands
                return filterStartingWith(args[0], Arrays.asList("help", "reload", "version", "create", "test"));
            } else if (args.length >= 2) {
                String subCommand = args[0].toLowerCase();
                
                switch (subCommand) {
                    case "create":
                        if (args.length == 2) {
                            // Return an empty list for world name (user input)
                            return Collections.emptyList();
                        } else if (args.length == 3) {
                            // Suggest available packs
                            List<String> packs = new ArrayList<>();
                            for (ConfigPack pack : TerraNukkitPlugin.PLATFORM.getConfigRegistry().entries()) {
                                packs.add(pack.getID());
                            }
                            return filterStartingWith(args[2], packs);
                        } else if (args.length == 4) {
                            // Suggest some common seed values
                            return filterStartingWith(args[3], 
                                Arrays.asList("0", "1", "random", String.valueOf(System.currentTimeMillis())));
                        }
                        break;
                    
                    case "test":
                        if (args.length == 2) {
                            // Suggest existing worlds
                            List<String> worlds = new ArrayList<>();
                            TerraNukkitPlugin.INSTANCE.getServer().getLevels().forEach((id, level) -> worlds.add(level.getName()));
                            return filterStartingWith(args[1], worlds);
                        } else if (args.length == 3) {
                            // Suggest available packs
                            List<String> packs = new ArrayList<>();
                            for (ConfigPack pack : TerraNukkitPlugin.PLATFORM.getConfigRegistry().entries()) {
                                packs.add(pack.getID());
                            }
                            return filterStartingWith(args[2], packs);
                        } else if (args.length == 4) {
                            // Suggest chunk count
                            return filterStartingWith(args[3], 
                                Arrays.asList("10", "25", "50", "100"));
                        }
                        break;
                }
            }
            
            return Collections.emptyList();
        }
        
        private void sendHelp(CommandSender sender) {
            sender.sendMessage(TextFormat.GREEN + "=== Terra Commands ===");
            sender.sendMessage(TextFormat.YELLOW + "/terra help" + TextFormat.WHITE + " - Show this help message");
            sender.sendMessage(TextFormat.YELLOW + "/terra reload" + TextFormat.WHITE + " - Reload Terra configuration");
            sender.sendMessage(TextFormat.YELLOW + "/terra version" + TextFormat.WHITE + " - Show Terra version information");
            sender.sendMessage(TextFormat.YELLOW + "/terra create <world> [pack] [seed]" + TextFormat.WHITE + " - Create a new Terra world");
            sender.sendMessage(TextFormat.YELLOW + "/terra test <world> [pack] [chunks]" + TextFormat.WHITE + " - Run a performance test");
        }
        
        private void handleReload(CommandSender sender) {
            if (!sender.hasPermission("terra.command.reload")) {
                sender.sendMessage(TextFormat.RED + "You don't have permission to use this command.");
                return;
            }
            
            sender.sendMessage(TextFormat.YELLOW + "Reloading Terra configuration...");
            
            try {
                // Use the enhanced reload method in TerraNukkitPlugin
                TerraNukkitPlugin.INSTANCE.reload();
                sender.sendMessage(TextFormat.GREEN + "Terra configuration reloaded successfully!");
                
                // Display information about available config packs
                int packCount = TerraNukkitPlugin.PLATFORM.getConfigRegistry().entries().size();
                sender.sendMessage(TextFormat.GREEN + "Available config packs: " + TextFormat.YELLOW + packCount);
                
                // Display active config pack
                ConfigPack activePack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                if (activePack != null) {
                    sender.sendMessage(TextFormat.GREEN + "Active config pack: " + TextFormat.YELLOW + activePack.getID());
                }
            } catch (Exception e) {
                sender.sendMessage(TextFormat.RED + "An error occurred while reloading Terra: " + e.getMessage());
                if (sender instanceof ConsoleCommandSender) {
                    e.printStackTrace();
                }
            }
        }
        
        private void handleVersion(CommandSender sender) {
            sender.sendMessage(TextFormat.GREEN + "Terra version: " + TextFormat.YELLOW + platform.getVersion());
            sender.sendMessage(TextFormat.GREEN + "Platform: " + TextFormat.YELLOW + platform.platformName());
            
            // Display configured packs, if any
            try {
                sender.sendMessage(TextFormat.GREEN + "Config packs: " + 
                    TextFormat.YELLOW + (platform.getConfigRegistry() != null ? "Loaded" : "None"));
            } catch (Exception e) {
                sender.sendMessage(TextFormat.RED + "Error accessing config packs");
            }
        }
        
        private void handleTest(CommandSender sender, String[] args) {
            if (!sender.hasPermission("terra.command.test")) {
                sender.sendMessage(TextFormat.RED + "You don't have permission to use this command.");
                return;
            }
            
            if (args.length < 2) {
                sender.sendMessage(TextFormat.RED + "Usage: /terra test <world> [pack] [chunks]");
                return;
            }
            
            String worldName = args[1];
            
            // Check if world exists
            cn.nukkit.level.Level level = TerraNukkitPlugin.INSTANCE.getServer().getLevelByName(worldName);
            if (level == null) {
                sender.sendMessage(TextFormat.RED + "World '" + worldName + "' doesn't exist or isn't loaded!");
                return;
            }
            
            // Determine which pack to use
            String packId = null;
            if (args.length >= 3) {
                packId = args[2];
            } else {
                // Try to find the pack from the world's generator
                Generator generator = level.getGenerator();
                if (generator != null && generator.getName().startsWith("terra:")) {
                    // Extract the pack ID from the generator name
                    packId = generator.getName().substring("terra:".length());
                }
                
                // If still no pack, use default pack
                if (packId == null) {
                    ConfigPack defaultPack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                    if (defaultPack != null) {
                        packId = defaultPack.getID();
                    }
                }
            }
            
            if (packId == null) {
                sender.sendMessage(TextFormat.RED + "Could not determine which pack to use! Please specify a pack ID.");
                return;
            }
            
            // Determine how many chunks to generate
            int chunkCount = 25; // Default
            if (args.length >= 4) {
                try {
                    chunkCount = Integer.parseInt(args[3]);
                    if (chunkCount <= 0) {
                        sender.sendMessage(TextFormat.RED + "Chunk count must be positive!");
                        return;
                    }
                    if (chunkCount > 500) {
                        sender.sendMessage(TextFormat.RED + "Chunk count limited to 500 for safety.");
                        chunkCount = 500;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(TextFormat.RED + "Invalid chunk count: " + args[3]);
                    return;
                }
            }
            
            sender.sendMessage(TextFormat.YELLOW + "Running performance test on world '" + worldName + 
                "' with pack '" + packId + "', generating " + chunkCount + " chunks...");
            
            // Run the test in a separate thread to avoid blocking the main thread
            final String finalPackId = packId;
            final int finalChunkCount = chunkCount;
            
            // Use Nukkit's AsyncTask properly
            TerraNukkitPlugin.INSTANCE.getServer().getScheduler().scheduleAsyncTask(TerraNukkitPlugin.INSTANCE, new AsyncTask() {
                @Override
                public void onRun() {
                    try {
                        PerformanceTest.runTest(level, finalPackId, finalChunkCount);
                        sender.sendMessage(TextFormat.GREEN + "Performance test complete! Check the console for results.");
                    } catch (Exception e) {
                        sender.sendMessage(TextFormat.RED + "Error during performance test: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        }
        
        private void handleCreate(CommandSender sender, String[] args) {
            if (!sender.hasPermission("terra.command.create")) {
                sender.sendMessage(TextFormat.RED + "You don't have permission to use this command.");
                return;
            }
            
            if (args.length < 2) {
                sender.sendMessage(TextFormat.RED + "Usage: /terra create <world> [pack] [seed]");
                return;
            }
            
            String worldName = args[1];
            
            // Check if world already exists
            if (TerraNukkitPlugin.INSTANCE.getServer().getLevelByName(worldName) != null) {
                sender.sendMessage(TextFormat.RED + "World '" + worldName + "' already exists!");
                return;
            }
            
            // Determine which pack to use
            String packId = null;
            if (args.length >= 3) {
                packId = args[2];
            } else {
                // Use default pack if none specified
                ConfigPack defaultPack = TerraNukkitPlugin.PLATFORM.getActiveConfig();
                if (defaultPack != null) {
                    packId = defaultPack.getID();
                }
            }
            
            // Validate the pack exists
            ConfigPack selectedPack = null;
            if (packId != null) {
                selectedPack = TerraNukkitPlugin.PLATFORM.getConfigRegistry()
                                  .getByID(packId)
                                  .orElse(null);
                if (selectedPack == null) {
                    sender.sendMessage(TextFormat.RED + "Config pack '" + packId + "' not found!");
                    
                    // List available packs
                    List<String> availablePacks = new ArrayList<>();
                    for (ConfigPack pack : TerraNukkitPlugin.PLATFORM.getConfigRegistry().entries()) {
                        availablePacks.add(pack.getID());
                    }
                    sender.sendMessage(TextFormat.YELLOW + "Available packs: " + String.join(", ", availablePacks));
                    return;
                }
            } else {
                sender.sendMessage(TextFormat.RED + "No config packs available!");
                return;
            }
            
            // Determine the seed
            long seed = System.currentTimeMillis();
            if (args.length >= 4) {
                try {
                    seed = Long.parseLong(args[3]);
                } catch (NumberFormatException e) {
                    // Use the string as a seed
                    seed = args[3].hashCode();
                }
            }
            
            // Create generator options
            Map<String, Object> options = new HashMap<>();
            options.put("pack", packId);
            options.put("preset", packId);
            options.put("worldName", worldName);
            options.put("seed", String.valueOf(seed));
            
            sender.sendMessage(TextFormat.YELLOW + "Creating world '" + worldName + "' with pack '" + packId + 
                             "' and seed " + seed + "...");
            
            try {
                // Create the world with the Terra generator
                String generatorName = "terra:" + packId.toLowerCase();
                
                // Check if this generator is already registered
                Class<? extends Generator> generatorClass = Generator.getGenerator(generatorName);
                if (generatorClass == null) {
                    sender.sendMessage(TextFormat.YELLOW + "Generator '" + generatorName + "' not found, trying with exact case: terra:" + packId);
                    generatorName = "terra:" + packId;
                    generatorClass = Generator.getGenerator(generatorName);
                }
                
                // If still no generator, try just "terra"
                if (generatorClass == null) {
                    sender.sendMessage(TextFormat.YELLOW + "Generator '" + generatorName + "' not found, using generic 'terra' generator");
                    generatorName = "terra";
                    generatorClass = Generator.getGenerator(generatorName);
                    
                    // Make sure the options have pack name even with generic generator
                    options.put("pack", packId);
                    options.put("preset", packId);
                }
                
                // If we got a generator, proceed with world creation
                if (generatorClass != null) {
                    sender.sendMessage(TextFormat.YELLOW + "Using generator: " + generatorName);
                    
                    boolean success = TerraNukkitPlugin.INSTANCE.getServer().generateLevel(worldName, seed, 
                                                                                      generatorClass, options);
                    
                    if (success) {
                        sender.sendMessage(TextFormat.GREEN + "World '" + worldName + "' created successfully!");
                        
                        // Load the world
                        TerraNukkitPlugin.INSTANCE.getServer().loadLevel(worldName);
                        sender.sendMessage(TextFormat.GREEN + "World '" + worldName + "' loaded.");
                    } else {
                        sender.sendMessage(TextFormat.RED + "Failed to create world '" + worldName + "'!");
                    }
                } else {
                    sender.sendMessage(TextFormat.RED + "No valid Terra generator found! Please check if the pack is loaded correctly.");
                }
            } catch (Exception e) {
                sender.sendMessage(TextFormat.RED + "Error creating world: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Filter a list of strings to only those starting with the given prefix.
     * 
     * @param prefix The prefix to filter by
     * @param options The options to filter
     * @return A list of strings that start with the given prefix
     */
    private List<String> filterStartingWith(String prefix, List<String> options) {
        if (prefix.isEmpty()) {
            return options;
        }
        
        return options.stream()
            .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
            .collect(Collectors.toList());
    }
} 