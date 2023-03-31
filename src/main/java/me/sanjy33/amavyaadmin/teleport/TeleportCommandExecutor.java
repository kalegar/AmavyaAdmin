package me.sanjy33.amavyaadmin.teleport;

import me.sanjy33.amavyaadmin.util.TimeParser;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;

public class TeleportCommandExecutor implements CommandExecutor {
	
	private final TeleportManager manager;
	
	public TeleportCommandExecutor(TeleportManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (command.getName().equalsIgnoreCase("tp")) {
			String arg = String.join(" ",args);
			if (player == null) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "minecraft:"+command.getName() + arg);
				return true;
			}
			if (args.length <= 1) {
				if (!player.isOp()) {
					player.performCommand("tpa " + arg);
					return true;
				}
			}
			player.performCommand("minecraft:tp " + arg);
			return true;
		}
		if (command.getName().equalsIgnoreCase("tpa")){
			if (player == null) {
				return true;
			}
			if (!command.testPermission(sender)){
				sender.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			if (args.length < 1) {
				return acceptTeleport(player);
			}
			Player target = Bukkit.getPlayer(args[0]);
			if (target != null) {
				List<TeleportRequest> sentRequests = manager.getSentTeleportRequests(player);
				for (TeleportRequest request : sentRequests) {
					if (request.getTo() != null && request.getTo().equals(target)) {
						player.sendMessage(ChatColor.RED + "You have already sent a teleport request to "+target.getName()+"!");
						return true;
					}
				}
				manager.createTeleportRequest(target, player);
				target.sendMessage(ChatColor.AQUA + player.getName() + ChatColor.GREEN + " wants to teleport to you!");
				TextComponent accept = new TextComponent(ChatColor.AQUA + manager.messageButtonAccept);
				accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tpaccept"));
				accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new Text("Accepts the teleport request!")));
				TextComponent deny = new TextComponent(ChatColor.AQUA + manager.messageButtonDeny);
				deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tpdeny"));
				deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new Text("Denies the teleport request!")));
				target.spigot().sendMessage(accept,new TextComponent(ChatColor.GREEN + " or "),deny);
				player.sendMessage(ChatColor.GREEN + "Teleport request sent to " + ChatColor.AQUA + target.getName());
				return true;
			} else {
				player.sendMessage(ChatColor.RED + args[0] + " is not online!");
				return true;
			}
			
		}
		if (command.getName().equalsIgnoreCase("tpaccept")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			if (!acceptTeleport(player)) {
				player.sendMessage(ChatColor.RED + "No one has requested to teleport to you!");
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("tpdeny")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			List<TeleportRequest> requests = manager.getTeleportRequestsTargetingPlayer(player);
			int requestsRemaining = requests.size();
			if (requestsRemaining > 0) {
				if (args.length >= 1) {
					for (TeleportRequest request : requests) {
						for (String name : args) {
							if (request.getFrom().getName().equalsIgnoreCase(name)) {
								request.getFrom().sendMessage(ChatColor.RED + player.getName() + " denied your teleport request!");
								manager.deleteTeleportRequest(request.getId());
								requestsRemaining--;
							}
						}
					}
				}else{
					TeleportRequest request = requests.get(0);
					request.getFrom().sendMessage(ChatColor.RED + player.getName() + " denied your teleport request!");
					manager.deleteTeleportRequest(request.getId());
					requestsRemaining--;
				}
				player.sendMessage(ChatColor.RED + "Teleport request denied.");
				if (requestsRemaining > 0) {
					player.sendMessage(ChatColor.RED + "You still have " + requestsRemaining + " pending teleport requests.");
				}
				return true;
			}else{
				player.sendMessage(ChatColor.RED + "No one has requested to teleport to you!");
				return true;
			}
		}
		if (command.getName().equalsIgnoreCase("back")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			if (manager.isDeathLocationStored(player)){
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.GREEN + "You will teleport in " + (manager.getTeleportWarmup()/20) + " seconds. Don't move!"));
				manager.teleport(player, manager.getDeathLocation(player), new TeleportCallback() {

					@Override
					public void onTeleport(boolean success, Player player, Location previousLocation,
							Location newLocation, String message) {
						if (success) {
							player.sendMessage(ChatColor.GREEN + "Teleported to death location!");
							manager.removeDeathLocation(player);
						}else {
							player.sendMessage(ChatColor.RED + message);
						}
						
					}
					
				});
			}else{
				player.sendMessage(ChatColor.RED + "You don't have a death location!");
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("setspawn")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			Location loc = player.getLocation();
			loc.getWorld().setSpawnLocation(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ());
			player.sendMessage(ChatColor.GREEN + "World spawn set!");
			return true;
		}
		if (command.getName().equalsIgnoreCase("setspawnwarp")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			Location spawn = player.getLocation().clone();
			spawn.setX(spawn.getBlockX() + 0.5);
			spawn.setY(spawn.getBlockY());
			spawn.setZ(spawn.getBlockZ() + 0.5);
			if (manager.setWorldSpawnLocation(spawn)) {
				manager.save();
				player.sendMessage(ChatColor.GREEN + "Spawn set for world " + spawn.getWorld().getName());
			}else{
				player.sendMessage(ChatColor.RED + "Error: Location had no world attached.");
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("spawn")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			long warmup = manager.getTeleportWarmup();
			if (player.hasPermission("aadmin.spawn.instant")) {
				warmup=1L;
			}else {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.GREEN + "You will teleport in " + (warmup/20) + " seconds. Don't move!"));
			}
			World world = player.getWorld();
			Location spawn = manager.getWorldSpawnLocation(world.getUID());
			if (spawn == null){
				spawn = world.getSpawnLocation();
			}
			manager.teleport(player, spawn, (success, player12, previousLocation, newLocation, message) -> {
				if (!success) {
					player12.sendMessage(ChatColor.RED + message);
				}
			},warmup);
			return true;
		}
		if (command.getName().equalsIgnoreCase("createwarp")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			if (args.length < 1){
				return false;
			}
			if (manager.warpExists(args[0])){
				player.sendMessage(ChatColor.RED + "That warp already exists!");
				if (player.hasPermission("aadmin.warp.delete")){
					player.sendMessage(ChatColor.RED + "You can use /warp delete " + args[0] + " to delete it.");
				}
				return true;
			}
			manager.createWarp(args[0], player.getLocation());
			player.sendMessage(ChatColor.GREEN + "Warp " + ChatColor.AQUA + args[0] + ChatColor.GREEN + " created!");
			return true;
		}
		if (command.getName().equalsIgnoreCase("deletewarp")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			if (args.length < 1){
				return false;
			}
			if (manager.warpExists(args[0])){
				manager.deleteWarp(args[0]);
				player.sendMessage(ChatColor.GREEN + "Warp " + ChatColor.AQUA +  args[0] + ChatColor.GREEN + " deleted!");
				return true;
			}else{
				player.sendMessage(ChatColor.RED + "Warp " + ChatColor.AQUA + args[0] + ChatColor.RED + " not found!");
				return true;
			}
		}
		if (command.getName().equalsIgnoreCase("warp")){
			if (!command.testPermission(player)){
				player.sendMessage(ChatColor.RED + "You don't have permission!");
				return true;
			}
			if (args.length < 1){
				return false;
			}
			if (manager.warpExists(args[0])){
				player.teleport(manager.getWarpLocation(args[0]));
				player.sendMessage(ChatColor.GREEN + "Welcome to " + ChatColor.AQUA + args[0] + ChatColor.GREEN + "!");
				return true;
			}else{
				player.sendMessage(ChatColor.RED + "Warp " + ChatColor.AQUA + args[0] + ChatColor.RED + " not found!");
				return true;
			}
		}
		for (TeleportManager.WorldWarp warp : TeleportManager.WorldWarp.values()) {
			if (command.getName().equalsIgnoreCase(warp.getCommand())) {
				Location loc = manager.getWorldWarp(warp);
				if (loc == null || player == null)
					return true;
				if (!player.hasPermission(warp.getPermission())) {
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
				Long warmup = manager.getTeleportWarmup();
				if (player.hasPermission(warp.getPermission() + ".nowarmup")) {
					warmup = 0L;
				}else{
					player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.GREEN + "You will teleport in " + TimeParser.parseLong(warmup/20*1000,false) + ". Don't move!"));
				}
				manager.teleport(player,loc, (success, player1, previousLocation, newLocation, message) -> {
					if (success) {
						player1.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.GREEN + "Teleported to "+warp.getDisplayName()+"!"));
					}else {
						player1.sendMessage(ChatColor.RED + message);
					}
				}, warmup);
				return true;
			}
			if (command.getName().equalsIgnoreCase(warp.getSetCommand())) {
				if (player == null) return true;
				if (!player.hasPermission(warp.getPermission()+".set")) {
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
				manager.setWorldWarp(warp,player.getLocation());
				player.sendMessage(ChatColor.GREEN + warp.getDisplayName() + " Location Set!");
				return true;
			}
		}
		return false;
	}

	private boolean acceptTeleport(Player player) {
		List<TeleportRequest> requests = manager.getTeleportRequestsTargetingPlayer(player);
		if (requests.size() > 0) {
			for (TeleportRequest request : requests) {
				Player requester = request.getFrom();
				if (requester == null || !requester.isOnline()) {
					continue;
				}
				player.sendMessage(ChatColor.GREEN + "You accepted " + requester.getName() + "'s teleport request!");
				requester.sendMessage(ChatColor.GREEN + player.getName() + " accepted your teleport request!");
				requester.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "You will teleport in " + (manager.getTeleportWarmup() / 20) + " seconds. Don't move!"));
				manager.deleteTeleportRequest(request.getId());
				manager.teleport(requester, player.getLocation(), new TeleportCallback() {

					@Override
					public void onTeleport(boolean success, Player player, Location previousLocation,
										   Location newLocation, String message) {
						if (success) {
							requester.sendMessage(ChatColor.GREEN + " You teleported to " + player.getName() + "!");
							player.sendMessage(ChatColor.GREEN + requester.getName() + " teleported to you!");
						} else {
							requester.sendMessage(ChatColor.RED + message);
							player.sendMessage(ChatColor.RED + message);
						}
					}

				},manager.getTeleportRequestWarmupTicks());
			}

			return true;
		}else{
			return false;
		}
	}

}
