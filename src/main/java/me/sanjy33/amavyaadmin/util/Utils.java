package me.sanjy33.amavyaadmin.util;

import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

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

}
