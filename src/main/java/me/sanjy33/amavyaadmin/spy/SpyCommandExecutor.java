package me.sanjy33.amavyaadmin.spy;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class SpyCommandExecutor implements CommandExecutor {

    private final AmavyaAdmin plugin;
    private final SpyManager manager;

    public SpyCommandExecutor(AmavyaAdmin plugin, SpyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (command.getName().equalsIgnoreCase("spy")) {
            if (player != null && !player.hasPermission("aadmin.spy")) {
                player.sendMessage(ChatColor.RED + "You don't have permission!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Usage: /spy <player>");
                return true;
            }
            String name = args[0];
            plugin.uuidManager.getUUID(name, ((name1, target) -> {
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player '" + name + "' not found!");
                } else {
                    Set<CommandSender> spies = manager.getSpies(target);
                    if (spies.contains(sender)) {
                        spies.remove(sender);
                        sender.sendMessage(ChatColor.GREEN + "No longer spying on '" + name + "'.");
                        return;
                    }
                    spies.add(sender);
                    sender.sendMessage(ChatColor.GREEN + "Spying on '" + name + "'.");
                }
            }));
            return true;
        }
        return true;
    }
}
