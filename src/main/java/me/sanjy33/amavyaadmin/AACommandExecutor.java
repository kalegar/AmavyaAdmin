package me.sanjy33.amavyaadmin;

import java.util.List;
import java.util.UUID;

import me.sanjy33.amavyaadmin.teleport.TeleportCallback;
import me.sanjy33.amavyaadmin.teleport.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.sanjy33.amavyaadmin.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class AACommandExecutor implements CommandExecutor{
	
	AmavyaAdmin plugin;
	
	public AACommandExecutor(AmavyaAdmin inst){
		plugin = inst;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
				Player player = null;
				if (sender instanceof Player){
					player = (Player) sender;
				}
				if (command.getName().equalsIgnoreCase("knownaliases")) {
					if (player != null && !player.hasPermission("aadmin.aliases")) {
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					if (args.length == 0) {
						sender.sendMessage("/knownaliases <name>");
						return true;
					}
					plugin.uuidManager.getUUID(args[0], (name,uuid) -> {
						if (uuid == null) {
							sender.sendMessage(ChatColor.RED + args[0] + " has never been on this server!");
							return;
						}
						List<String> aliases = plugin.uuidManager.getKnownAliases(uuid);
						if (aliases == null) {
							sender.sendMessage(ChatColor.RED + args[0] + " has no known aliases.");
							return;
						}
						sender.sendMessage(ChatColor.AQUA + args[0] + ChatColor.WHITE + "'s Known Aliases: ");
						for (String alias : aliases) {
							sender.sendMessage(alias);
						}
					});
					return true;
				}
				if (command.getName().equalsIgnoreCase("tips")){
					if (player!=null){
						if (!player.hasPermission("aadmin.tips")){
							player.sendMessage(ChatColor.RED + "You don't have permission!");
							return true;
						}
					}else{
						sender.sendMessage("That can't be used in the console!");
						return true;
					}
					String status = plugin.periodicMessageManager.toggleMessagesDisabled(player.getUniqueId()) ? ChatColor.GREEN+"ON" : ChatColor.RED + "OFF";
					player.sendMessage(ChatColor.GREEN + "Messages toggled "+status+ChatColor.GREEN+"!");
					return true;
				}
				if (command.getName().equalsIgnoreCase("setkit")){
					if (player!=null){
						if (!player.hasPermission("aadmin.setkit")){
							player.sendMessage(ChatColor.RED + "You don't have permission!");
							return true;
						}
					}else{
						sender.sendMessage("That can't be used in the console!");
						return true;
					}
					plugin.starterKit = new Inventory(player.getInventory());
					player.sendMessage(ChatColor.GREEN + "Starter kit set!");
					plugin.starterKit.save(plugin.getConfig().getConfigurationSection("kits.starter"));
					plugin.saveConfig();
					return true;
				}
				if (command.getName().equalsIgnoreCase("rules")){
					if (player!=null){
						if (!player.hasPermission("aadmin.rules")){
							player.sendMessage(ChatColor.RED + "You don't have permission!");
							return true;
						}
					}
					if (plugin.rules.size()>0) {
						for (String msg : plugin.rules) {
							sender.sendMessage(msg);
						}
					}else {
						sender.sendMessage(ChatColor.WHITE + "Rules: ");
						sender.sendMessage(ChatColor.WHITE + " 1. Have Fun!");
					}
					return true;
				}
				if (command.getName().equalsIgnoreCase("aareload")){
					if (!(player==null)){
						if (!player.hasPermission("aadmin.reload")){
							player.sendMessage(ChatColor.RED + "You don't have permission!");
							return true;
						}
					}
					plugin.reload();
					sender.sendMessage("Reloaded config.");
					return true;
				}
				if (command.getName().equalsIgnoreCase("ts")){
					if (player == null){
						if (args.length >= 2){
							World world = Bukkit.getWorld(args[1]);
							if (world != null) {
								world.setTime(Integer.parseInt(args[0]));
								sender.sendMessage("Time in world " + args[1] + " set to " + args[0]);
							}else{
								sender.sendMessage("World " + args[1] + " not found!");
							}
						}else{
							sender.sendMessage("ts <time> <world>");
						}
						return true;
					}
					if (!player.hasPermission("aadmin.timeset")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					World world = player.getWorld();
					int time = 0;
					if (args.length == 0) {
						player.sendMessage(ChatColor.RED + "/ts <time> [world]");
						return true;
					}
					if (args.length > 0){
						try {
							time = Integer.parseInt(args[0]);
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "/ts <time> [world]");
							return true;
						}
					}
					if (args.length > 1) {
						try {
							world = Bukkit.getWorld(args[1]);
						} catch (Exception e) {
							player.sendMessage(ChatColor.RED + "/ts <time> [world]");
							return true;
						}
					}
					if (world == null) {
						player.sendMessage(ChatColor.RED + "/ts <time> [world]");
						return true;
					}
					world.setTime(time);
					player.sendMessage(ChatColor.GREEN + "Time in world " + world.getName() + " set to " + time);
					return true;
				}
				if (command.getName().equalsIgnoreCase("fly")){
					if (player == null){
						if ((args.length) >=1) {
							Player target = Bukkit.getPlayer(args[0]);
							if (target != null) {
								if (target.getAllowFlight()){
									sender.sendMessage(ChatColor.GREEN + args[0] + "'s flight toggled " + ChatColor.RED + "off");
									target.setFallDistance(0);
									target.setAllowFlight(false);
								}else{
									sender.sendMessage(ChatColor.GREEN + args[0] + "'s flight toggled on");
									target.setAllowFlight(true);
								}
								return true;
							}else{
								sender.sendMessage("Player not Found!");
								return true;
							}
						}
					}else{
						if (args.length < 1){
							if (!player.hasPermission("aadmin.fly")){
								player.sendMessage(ChatColor.RED + "You don't have permission!");
								return true;
							}
							if (player.getAllowFlight()){
								player.setAllowFlight(false);
								player.sendMessage(ChatColor.GREEN + "Flight " + ChatColor.RED + "Disabled");
								player.setFallDistance(0);
							}else{
								player.setAllowFlight(true);
								player.sendMessage(ChatColor.GREEN + "Flight Enabled");
							}
							return true;
						}else{
							if (!player.hasPermission("aadmin.fly.other")){
								player.sendMessage(ChatColor.RED + "You don't have permission!");
								return true;
							}
							Player target = Bukkit.getPlayer(args[0]);
							if (target != null) {
								if (target.getAllowFlight()){
									player.sendMessage(ChatColor.GREEN + args[0] + "'s flight toggled " + ChatColor.RED + "off");
									target.setAllowFlight(false);
								}else{
									player.sendMessage(ChatColor.GREEN + args[0] + "'s flight toggled on");
									target.setAllowFlight(true);
								}
							}else{
								player.sendMessage("Player not Found!");
							}
							return true;
						}
					}
				}
				if (command.getName().equalsIgnoreCase("lockdown")){
					if (player!=null){
						if (!player.hasPermission("aadmin.lockdown")){
							player.sendMessage(ChatColor.RED + "You don't have permission!");
							return true;
						}
					}
					plugin.lockdown = !plugin.lockdown;
					if (plugin.lockdown){
						for (Player p : Bukkit.getOnlinePlayers()){
							if (!p.hasPermission("aadmin.lockdown.bypass")){
								p.kickPlayer("The server is under lockdown!");
							}else{
								p.sendMessage(ChatColor.RED + "<<< The server is now under lockdown! >>>");
							}
						}
					}else{
						Bukkit.broadcastMessage(ChatColor.GREEN + "<<< The server is no longer under lockdown! >>>");
					}
					return true;
				}
				if (command.getName().equalsIgnoreCase("motd")) {
					if (args.length < 1 && player != null) {
						plugin.messageOfTheDay.sendMotd(player);
						return true;
					}
					if (player != null && !player.hasPermission("aadmin.motd")) {
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					StringBuilder m = new StringBuilder();
					for (String s : args) {
						m.append(s).append(" ");
					}
					String json = m.toString();
					plugin.messageOfTheDay.parseMotd(json);
					plugin.messageOfTheDay.save();
					if (player != null) {
						sender.sendMessage(ChatColor.GREEN + "MOTD set to: ");
						plugin.messageOfTheDay.sendMotd(player);
					} else {
						sender.sendMessage("MOTD changed!");
					}
					return true;
				}
		return false;
	}

}
