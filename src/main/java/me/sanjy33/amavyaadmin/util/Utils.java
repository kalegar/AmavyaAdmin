package me.sanjy33.amavyaadmin.util;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Utils {
	
	public static String format(String string) {
	    String s = string;
	    for (ChatColor color : ChatColor.values()) {
	        s = s.replaceAll("(?i)<" + color.name() + ">", "" + color);
	    }
	    return s;
	}
	
	public static void filterOutSpectators(List<Player> players) {
		players.removeIf(player -> player.getGameMode().equals(GameMode.SPECTATOR));
	}

	public static void registerAndSetupCommands(JavaPlugin plugin, String[] commands, CommandExecutor executor, TabCompleter tabCompleter) {
		for (String name : commands) {
			PluginCommand command = plugin.getCommand(name);
			if (command != null) {
				if (executor != null) {
					command.setExecutor(executor);
				}
				if (tabCompleter != null) {
					command.setTabCompleter(tabCompleter);
				}
			}
		}
	}

	public static void registerAndSetupCommands(JavaPlugin plugin, String[] commands, CommandExecutor executor) {
		registerAndSetupCommands(plugin,commands,executor,null);
	}

}
