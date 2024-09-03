package com.dsopinka.mimicmobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class MimicMobsPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Mimic Mobs Plugin has been enabled!");

        // Register events
        Bukkit.getPluginManager().registerEvents(new MimicListener(this), this);

        // Save default config if not present
        this.saveDefaultConfig();

        // Register command and tab completer
        this.getCommand("mimic").setExecutor(this);
        this.getCommand("mimic").setTabCompleter(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Mimic Mobs Plugin has been disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mimic")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    MimicListener mimicListener = new MimicListener(this);
                    mimicListener.listMimics(player);
                } else {
                    sender.sendMessage("This command can only be run by a player.");
                }
            } else {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "MIMIC: " + ChatColor.YELLOW + "Invalid command. Use /mimic list");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("mimic")) {
            if (args.length == 1) {
                return Arrays.asList("list");
            }
        }
        return null;
    }
}
