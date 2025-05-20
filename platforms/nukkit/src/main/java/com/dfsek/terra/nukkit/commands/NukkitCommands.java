package com.dfsek.terra.nukkit.commands;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.Command;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.TextFormat;

import com.dfsek.terra.api.Platform;
import com.dfsek.terra.nukkit.TerraNukkitPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
        terraCommand.setUsage("/terra <help|reload|version>");
        terraCommand.setAliases(new String[]{"t"});
        terraCommand.setPermission("terra.command");
        
        // Set the command executor
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
                default -> {
                    sender.sendMessage(TextFormat.RED + "Unknown Terra command: " + args[0]);
                    sendHelp(sender);
                }
            }
            
            return true;
        }
        
        private void sendHelp(CommandSender sender) {
            sender.sendMessage(TextFormat.GREEN + "=== Terra Commands ===");
            sender.sendMessage(TextFormat.YELLOW + "/terra help" + TextFormat.WHITE + " - Show this help message");
            sender.sendMessage(TextFormat.YELLOW + "/terra reload" + TextFormat.WHITE + " - Reload Terra configuration");
            sender.sendMessage(TextFormat.YELLOW + "/terra version" + TextFormat.WHITE + " - Show Terra version information");
        }
        
        private void handleReload(CommandSender sender) {
            if (!sender.hasPermission("terra.command.reload")) {
                sender.sendMessage(TextFormat.RED + "You don't have permission to use this command.");
                return;
            }
            
            sender.sendMessage(TextFormat.YELLOW + "Reloading Terra configuration...");
            
            try {
                boolean success = platform.reload();
                if (success) {
                    sender.sendMessage(TextFormat.GREEN + "Terra configuration reloaded successfully!");
                } else {
                    sender.sendMessage(TextFormat.RED + "Failed to reload Terra configuration. Check console for errors.");
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