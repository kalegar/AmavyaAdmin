package me.sanjy33.amavyaadmin.tabcompleter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class PermissionTabCompleter implements TabCompleter {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        Player player = null;
        if (!(commandSender instanceof Player)) {
            return null;
        }
        player = (Player) commandSender;
        String permissionStr = command.getPermission();
        if (permissionStr == null) {
            return null;
        }
        if (player.hasPermission(permissionStr)) {
            return null;
        }
        return Collections.emptyList();


    }
}
