package me.sanjy33.amavyaadmin.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface TeleportCallback {
	
	void onTeleport(boolean success, Player player, Location previousLocation, Location newLocation, String message);

}
