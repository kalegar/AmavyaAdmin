package me.sanjy33.amavyaadmin.jail;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class JailReleaseTask implements Runnable {
	
	private final JailCell cell;
	private final List<String> toBeReleased;
	
	public JailReleaseTask(JailCell cell, List<String> toBeReleased) {
		this.cell = cell;
		this.toBeReleased = toBeReleased;
	}

	@Override
	public void run() {
		UUID u = cell.getOccupant();
		Player target = Bukkit.getPlayer(u);
		if (target instanceof Player){
			target.teleport(target.getLocation().getWorld().getSpawnLocation());
			target.sendMessage(ChatColor.GREEN + "You have been released from jail.");
		}else{
			toBeReleased.add(u.toString());
		}
		cell.reset();
	}

}
