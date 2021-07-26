package me.sanjy33.amavyaadmin.vanish;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommandExecutor implements CommandExecutor {
	
	private final VanishManager manager;
	
	public VanishCommandExecutor(VanishManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (command.getName().equalsIgnoreCase("fakejoin")) {
			if (player!=null){
				if (!player.hasPermission("aadmin.vanish.fakejoin")){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " joined the game");
			if (manager.isPlayerInvisible(player)) {
				player.sendMessage(ChatColor.GRAY + "(You are now visible)");
				manager.toggleInvisibility(player);
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("fakequit")) {
			if (player!=null){
				if (!player.hasPermission("aadmin.vanish.fakequit")){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " left the game");
			if (!manager.isPlayerInvisible(player)) {
				player.sendMessage(ChatColor.GRAY + "(You are now invisible)");
				manager.toggleInvisibility(player);
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("silentjoin")) {
			if (player!=null){
				if (!player.hasPermission("aadmin.vanish.silentjoin")){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			if (manager.getSilentJoin(player.getUniqueId())) {
				manager.disableSilentJoin(player.getUniqueId());
				player.sendMessage(ChatColor.GOLD + "Silent join " + ChatColor.RED + "disabled"+ChatColor.GOLD+"!");
			}else {
				manager.enableSilentJoin(player.getUniqueId());
				player.sendMessage(ChatColor.GOLD + "Silent join " + ChatColor.GREEN + "enabled"+ChatColor.GOLD+"!");
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("silentquit")) {
			if (player!=null){
				if (!player.hasPermission("aadmin.vanish.silentquit")){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			if (manager.getSilentQuit(player.getUniqueId())) {
				manager.disableSilentQuit(player.getUniqueId());
				player.sendMessage(ChatColor.GOLD + "Silent quit " + ChatColor.RED + "disabled"+ChatColor.GOLD+"!");
			}else {
				manager.enableSilentQuit(player.getUniqueId());
				player.sendMessage(ChatColor.GOLD + "Silent quit " + ChatColor.GREEN + "enabled"+ChatColor.GOLD+"!");
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("vanish")) {
			if (player!=null){
				if (!player.hasPermission("aadmin.vanish")){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			manager.toggleInvisibility(player);
			if (manager.isPlayerInvisible(player)) {
				player.sendMessage(ChatColor.GOLD + "You are now " + ChatColor.GRAY + "invisible");
			}else {
				player.sendMessage(ChatColor.GOLD + "You are now " + ChatColor.WHITE + "visible");
			}
			return true;
		}
		return false;
	}

}
