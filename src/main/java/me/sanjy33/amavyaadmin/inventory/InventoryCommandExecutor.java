package me.sanjy33.amavyaadmin.inventory;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InventoryCommandExecutor implements CommandExecutor {

    private final InventoryManager manager;

    public InventoryCommandExecutor(InventoryManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("inventory")) {
            if (args.length < 1) {
                return false;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command cannot be used from the console.");
                return true;
            }
            Player player = (Player) sender;
            if (args[0].equalsIgnoreCase("store") || args[0].equalsIgnoreCase("save")) {
                if (!player.hasPermission("aadmin.inventory.store")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /inventory " + args[0] + " <inventoryname>");
                    return true;
                }
                int storedInventories = manager.getStoredInventoryCount(player.getUniqueId());
                int maxStoredInventories = 0;
                for (int i = 1; i <= InventoryManager.MAX_STORED_INVENTORIES; i++) {
                    if (player.hasPermission("aadmin.inventory.max."+i)) {
                        maxStoredInventories = i;
                    }
                }
                if (maxStoredInventories > 0) {
                    if (storedInventories+1 > maxStoredInventories) {
                        player.sendMessage(ChatColor.RED + "You are at your inventory storage limit. You must clear an inventory with " + ChatColor.AQUA + "/inventory clear" + ChatColor.RED + " first.");
                        return true;
                    }
                }
                manager.savePlayerInventoryAndExperience(player,args[1].toUpperCase());
                player.sendMessage(ChatColor.DARK_PURPLE + "Your inventory has been saved with key '" + args[1] + "'");
                return true;
            }
            if (args[0].equalsIgnoreCase("restore") || args[0].equalsIgnoreCase("load")) {
                if (!player.hasPermission("aadmin.inventory.restore")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /inventory " + args[0] + " <inventoryname>");
                    return true;
                }
                UUID uuid = player.getUniqueId();
                String key = args[1].toUpperCase();
                if (!manager.isInventoryStored(uuid,key)) {
                    player.sendMessage(ChatColor.RED + "No inventory with key " + args[1] + " is stored!");
                    return true;
                }
                manager.getStoredInventory(uuid,key).setPlayerInventory(player,false);
                player.sendMessage(ChatColor.DARK_PURPLE + "Your inventory has been restored from saved inventory '" + args[1] + "'");
                return true;
            }
            if (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("delete")) {
                if (!player.hasPermission("aadmin.inventory.clear")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /inventory " + args[0] + " <inventoryname>");
                    return true;
                }
                UUID uuid = player.getUniqueId();
                String key = args[1].toUpperCase();
                if (!manager.isInventoryStored(uuid,key)) {
                    player.sendMessage(ChatColor.RED + "No inventory with key " + args[1] + " is stored!");
                    return true;
                }
                manager.clearStoredInventory(uuid,key);
                player.sendMessage(ChatColor.DARK_PURPLE + "Your stored inventory with key '" + args[1] + "' has been cleared.");
                return true;
            }
        }
        return false;
    }
}
