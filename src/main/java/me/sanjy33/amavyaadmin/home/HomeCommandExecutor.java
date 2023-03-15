package me.sanjy33.amavyaadmin.home;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.teleport.TeleportCallback;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class HomeCommandExecutor implements CommandExecutor {
	
	private final AmavyaAdmin plugin;
	private final HomeManager manager;
	
	public HomeCommandExecutor(AmavyaAdmin plugin, HomeManager manager) {
		this.plugin = plugin;
		this.manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (command.getName().equalsIgnoreCase("home")){
			if (player==null){
				sender.sendMessage("This command can't be used in the console.");
				return true;
			}else{
				if (!command.testPermission(player)){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}
			if (args.length>0){
				if (!player.hasPermission("aadmin.home.other")){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
				final Player pl = player;
				plugin.uuidManager.getUUID(args[0], ((name, uuid) -> {
					if (uuid == null) {
						sender.sendMessage(ChatColor.RED + args[0] + " hasn't been on this server!");
					} else {
						if (!teleportToHome(pl, uuid)) {
							pl.sendMessage(ChatColor.RED + args[0] + "doesn't have a home!");
						}
					}
				}));
				return true;
			}
			UUID targetUuid = player.getUniqueId();
			if (!teleportToHome(player, targetUuid)) {
				player.sendMessage(ChatColor.RED + "You don't have a home! Set one with /sethome");
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("sethome")){
			if (player==null){
				sender.sendMessage("This command can't be used in the console.");
				return true;
			}else{
				if (!command.testPermission(player)){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}
			UUID u = player.getUniqueId();
			if (manager.homeExists(u)){
				player.sendMessage(ChatColor.RED + "You already have a home set! Delete it first with /deletehome");
				return true;
			}
			PlayerHome home = new PlayerHome(u, player.getLocation().clone(), player.getName());
			manager.setHome(home);
			player.sendMessage(ChatColor.GREEN + "Home set!");
			return true;
		}
		if (command.getName().equalsIgnoreCase("deletehome")){
			if (player==null){
				sender.sendMessage("This command can't be used in the console.");
				return true;
			}else{
				if (!command.testPermission(player)){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}
			if (args.length>0){
				if (!player.hasPermission("aadmin.home.delete.other")){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
				plugin.uuidManager.getUUID(args[0],((name, uuid) -> {
					if (uuid == null) {
						sender.sendMessage(ChatColor.RED + args[0] + " has never been on this server.");
					}
					if (manager.homeExists(uuid)){
						manager.clearHome(uuid);
						sender.sendMessage(ChatColor.GREEN + args[0]+"'s home deleted!");
					}else{
						sender.sendMessage(ChatColor.RED + args[0] + " does not have a home!");
					}
				}));
				return true;
			}else{
				UUID u = player.getUniqueId();
				if (manager.homeExists(u)){
					manager.clearHome(u);
					player.sendMessage(ChatColor.GREEN + "Home deleted!");
					return true;
				}else{
					player.sendMessage(ChatColor.RED + "You don't have a home!");
					return true;
				}
			}
		}
		if (command.getName().equalsIgnoreCase("listhome")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}
			sender.sendMessage(manager.getHomeList());
			return true;
		}
		return false;
	}

	private boolean teleportToHome(Player player, UUID target) {
		if (manager.homeExists(target)){
			long warmup = manager.getHomeWarpWarmup();
			if (player.hasPermission("aadmin.home.instant")) {
				warmup=1L;
			}else {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.GREEN+"Teleporting in " + (warmup/20) + " seconds. Don't move!"));
			}
			PlayerHome home = manager.getHome(target);
			plugin.teleportManager.teleport(player, home.getLocation(), new TeleportCallback() {

				@Override
				public void onTeleport(boolean success, Player player, Location previousLocation,
									   Location newLocation, String message) {
					if (success) {
						player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.GREEN + "" + ChatColor.ITALIC + "Welcome home!"));
					}else {
						player.sendMessage(ChatColor.RED + message);
					}

				}

			}, warmup);
			return true;
		}else{
			return false;
		}
	}

}
