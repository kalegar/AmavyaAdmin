package me.sanjy33.amavyaadmin.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
                sender.sendMessage(Component.text("This command cannot be used from the console.", NamedTextColor.RED));
                return true;
            }
            Player player = (Player) sender;
            if (args[0].equalsIgnoreCase("store") || args[0].equalsIgnoreCase("save")) {
                if (!player.hasPermission("aadmin.inventory.store")) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /inventory " + args[0] + " <inventoryname>", NamedTextColor.RED));
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
                        player.sendMessage(
                                Component.text("You are at your inventory storage limit. You must clear an inventory with ", NamedTextColor.RED)
                                .append(Component.text("/inventory clear", NamedTextColor.AQUA))
                                .append(Component.text(" first.",NamedTextColor.RED))
                        );
                        return true;
                    }
                }
                manager.savePlayerInventoryAndExperience(player,args[1].toUpperCase());
                player.sendMessage(Component.text("Your inventory has been saved with key '" + args[1] + "'", NamedTextColor.DARK_PURPLE));
                return true;
            }
            if (args[0].equalsIgnoreCase("restore") || args[0].equalsIgnoreCase("load")) {
                if (!sender.hasPermission("aadmin.inventory.restore")) {
                    sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /inventory " + args[0] + " <inventoryname>", NamedTextColor.RED));
                    return true;
                }
                Player target;
                if (args.length >= 3) {
                    target = Bukkit.getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage(Component.text("No player called " + args[2] + " was found!"));
                        return true;
                    }
                } else {
                    target = player;
                }
                UUID uuid = target.getUniqueId();
                String key = args[1].toUpperCase();
                if (!manager.isInventoryStored(uuid,key)) {
                    sender.sendMessage(Component.text("No inventory with key " + args[1] + " is stored for player " + target.getName() + "!", NamedTextColor.RED));
                    return true;
                }
                manager.getStoredInventory(uuid,key).setPlayerInventory(target,true, false);
                sender.sendMessage(Component.text(target.getName() +"'s inventory has been restored from saved inventory '" + args[1] + "'", NamedTextColor.DARK_PURPLE));
                return true;
            }
            if (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("delete")) {
                if (!player.hasPermission("aadmin.inventory.clear")) {
                    player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /inventory " + args[0] + " <inventoryname>", NamedTextColor.RED));
                    return true;
                }
                UUID uuid = player.getUniqueId();
                String key = args[1].toUpperCase();
                if (!manager.isInventoryStored(uuid,key)) {
                    player.sendMessage(Component.text("No inventory with key " + args[1] + " is stored!", NamedTextColor.RED));
                    return true;
                }
                manager.clearStoredInventory(uuid,key);
                player.sendMessage(Component.text("Your stored inventory with key '" + args[1] + "' has been cleared.", NamedTextColor.DARK_PURPLE));
                return true;
            }
        }
        return false;
    }
}
