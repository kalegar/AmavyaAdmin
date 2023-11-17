package me.sanjy33.amavyaadmin.spy;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class SpyCommandExecutor implements CommandExecutor {

    private final AmavyaAdmin plugin;
    private final SpyManager manager;

    public SpyCommandExecutor(AmavyaAdmin plugin, SpyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (command.getName().equalsIgnoreCase("spy")) {
            if (player != null && !command.testPermission(player)) {
                player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(Component.text("Usage: /spy <player>",NamedTextColor.RED));
                return true;
            }
            String name = args[0];
            plugin.uuidManager.getUUID(name, ((name1, target) -> {
                if (target == null) {
                    sender.sendMessage(Component.text("Player '" + name + "' not found!", NamedTextColor.RED));
                } else {
                    Set<CommandSender> spies = manager.getSpies(target);
                    if (spies.contains(sender)) {
                        spies.remove(sender);
                        sender.sendMessage(Component.text("No longer spying on '" + name + "'.", NamedTextColor.GREEN));
                        return;
                    }
                    spies.add(sender);
                    sender.sendMessage(Component.text("Spying on '" + name + "'.", NamedTextColor.GREEN));
                }
            }));
            return true;
        }
        return true;
    }
}
