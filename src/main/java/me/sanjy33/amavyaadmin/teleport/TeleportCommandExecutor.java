package me.sanjy33.amavyaadmin.teleport;

import me.sanjy33.amavyaadmin.util.TimeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TeleportCommandExecutor implements CommandExecutor {
	
	private final TeleportManager manager;
	
	public TeleportCommandExecutor(TeleportManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
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
				sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			if (args.length < 1) {
				return acceptTeleport(player, "");
			}
			Player target = Bukkit.getPlayer(args[0]);
			if (target != null) {
				List<TeleportRequest> sentRequests = manager.getSentTeleportRequests(player);
				for (TeleportRequest request : sentRequests) {
					if (request.getTo() != null && request.getTo().equals(target)) {
						player.sendMessage(Component.text("You have already sent a teleport request to "+target.getName()+"!", NamedTextColor.RED));
						return true;
					}
				}
				manager.createTeleportRequest(target, player);
				target.sendMessage(Component.text(player.getName(), NamedTextColor.AQUA)
						.append(Component.text(" wants to teleport to you!", NamedTextColor.GREEN))
				);
				Component accept = Component.text(manager.messageButtonAccept, NamedTextColor.AQUA)
						.hoverEvent(HoverEvent.showText(Component.text("Accepts the teleport request!")))
						.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + player.getName()));
				Component deny = Component.text(manager.messageButtonDeny, NamedTextColor.AQUA)
						.hoverEvent(HoverEvent.showText(Component.text("Denies the teleport request!")))
						.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + player.getName()));

				target.sendMessage(accept.append(Component.text(" or ", NamedTextColor.GREEN)).append(deny));
				player.sendMessage(Component.text("Teleport request sent to ", NamedTextColor.GREEN)
						.append(Component.text(target.getName(), NamedTextColor.AQUA))
				);
			} else {
				player.sendMessage(Component.text(args[0] + " is not online!", NamedTextColor.RED));
			}
			return true;

		}
		if (command.getName().equalsIgnoreCase("tpaccept")){
			if (player == null) return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			String target = "";
			if (args.length > 0) {
				target = args[0];
			}
			if (!acceptTeleport(player, target)) {
				if (args.length > 0) {
					player.sendMessage(Component.text(args[0] + " has not requested to teleport to you!", NamedTextColor.RED));
				}else {
					player.sendMessage(Component.text("No one has requested to teleport to you!", NamedTextColor.RED));
				}
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("tpdeny")){
			if (player == null) return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			List<TeleportRequest> requests = manager.getTeleportRequestsTargetingPlayer(player);
			int requestsRemaining = requests.size();
			if (requestsRemaining > 0) {
				if (args.length >= 1) {
					for (TeleportRequest request : requests) {
						for (String name : args) {
							if (request.getFrom().getName().equalsIgnoreCase(name)) {
								request.getFrom().sendMessage(Component.text(player.getName() + " denied your teleport request!", NamedTextColor.RED));
								manager.deleteTeleportRequest(request.getId());
								requestsRemaining--;
							}
						}
					}
				}else{
					TeleportRequest request = requests.get(0);
					request.getFrom().sendMessage(Component.text(player.getName() + " denied your teleport request!", NamedTextColor.RED));
					manager.deleteTeleportRequest(request.getId());
					requestsRemaining--;
				}
				player.sendMessage(Component.text("Teleport request denied.", NamedTextColor.RED));
				if (requestsRemaining > 0) {
					player.sendMessage(Component.text("You still have " + requestsRemaining + " pending teleport requests.", NamedTextColor.RED));
				}
			}else{
				player.sendMessage(Component.text("No one has requested to teleport to you!", NamedTextColor.RED));
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("back")){
			if (player == null) return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			if (manager.isDeathLocationStored(player)){
				player.sendActionBar(Component.text("You will teleport in " + (manager.getTeleportWarmup()/20) + " seconds. Don't move!", NamedTextColor.GREEN));
				manager.teleport(player, manager.getDeathLocation(player), (success, pl, previousLocation, newLocation, message) -> {
					if (success) {
						pl.sendMessage(Component.text("Teleported to death location!", NamedTextColor.GREEN));
						manager.removeDeathLocation(pl);
					}else {
						pl.sendMessage(Component.text(message, NamedTextColor.RED));
					}

				});
			}else{
				player.sendMessage(Component.text("You don't have a death location!", NamedTextColor.RED));
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("setspawn")){
			if (player == null) return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			Location loc = player.getLocation();
			loc.getWorld().setSpawnLocation(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ());
			player.sendMessage(Component.text("World spawn set!", NamedTextColor.GREEN));
			return true;
		}
		if (command.getName().equalsIgnoreCase("setspawnwarp")){
			if (player == null) return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			Location spawn = player.getLocation().clone();
			spawn.setX(spawn.getBlockX() + 0.5);
			spawn.setY(spawn.getBlockY());
			spawn.setZ(spawn.getBlockZ() + 0.5);
			if (manager.setWorldSpawnLocation(spawn)) {
				manager.save();
				player.sendMessage(Component.text("Spawn set for world " + spawn.getWorld().getName(), NamedTextColor.GREEN));
			}else{
				player.sendMessage(Component.text("Error: Location had no world attached.", NamedTextColor.RED));
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("spawn")){
			if (player == null) return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			long warmup = manager.getTeleportWarmup();
			if (player.hasPermission("aadmin.spawn.instant")) {
				warmup=1L;
			}else {
				player.sendActionBar(Component.text("You will teleport in " + (warmup/20) + " seconds. Don't move!", NamedTextColor.GREEN));
			}
			World world = player.getWorld();
			Location spawn = manager.getWorldSpawnLocation(world.getUID());
			if (spawn == null){
				spawn = world.getSpawnLocation();
			}
			manager.teleport(player, spawn, (success, player12, previousLocation, newLocation, message) -> {
				if (!success) {
					player12.sendMessage(Component.text(message, NamedTextColor.RED));
				}
			},warmup);
			return true;
		}
		if (command.getName().equalsIgnoreCase("createwarp")){
			if (player == null) return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			if (args.length < 1){
				return false;
			}
			if (manager.warpExists(args[0])){
				player.sendMessage(Component.text("That warp already exists!", NamedTextColor.RED));
				if (player.hasPermission("aadmin.warp.delete")){
					player.sendMessage(Component.text("You can use /warp delete " + args[0] + " to delete it.", NamedTextColor.RED));
				}
				return true;
			}
			manager.createWarp(args[0], player.getLocation());
			player.sendMessage(Component.text("Warp ", NamedTextColor.GREEN)
					.append(Component.text(args[0], NamedTextColor.AQUA))
					.append(Component.text(" created!", NamedTextColor.GREEN))
			);
			return true;
		}
		if (command.getName().equalsIgnoreCase("deletewarp")){
			if (player == null)	return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			if (args.length < 1){
				return false;
			}
			if (manager.warpExists(args[0])){
				manager.deleteWarp(args[0]);
				player.sendMessage(Component.text("Warp ", NamedTextColor.GREEN)
						.append(Component.text(args[0], NamedTextColor.AQUA))
						.append(Component.text(" deleted!", NamedTextColor.GREEN))
				);
			}else{
				player.sendMessage(Component.text("Warp ", NamedTextColor.RED)
						.append(Component.text(args[0], NamedTextColor.AQUA))
						.append(Component.text(" not found!", NamedTextColor.RED))
				);
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("warp")){
			if (player == null) return true;
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			if (args.length < 1){
				return false;
			}
			if (manager.warpExists(args[0])){
				player.teleport(manager.getWarpLocation(args[0]));
				player.sendMessage(Component.text("Welcome to ", NamedTextColor.GREEN)
						.append(Component.text(args[0], NamedTextColor.AQUA))
						.append(Component.text("!", NamedTextColor.GREEN))
				);
			}else{
				player.sendMessage(Component.text("Warp ", NamedTextColor.RED)
						.append(Component.text(args[0], NamedTextColor.AQUA))
						.append(Component.text(" not found!", NamedTextColor.RED))
				);
			}
			return true;
		}
		for (TeleportManager.WorldWarp warp : TeleportManager.WorldWarp.values()) {
			if (command.getName().equalsIgnoreCase(warp.getCommand())) {
				Location loc = manager.getWorldWarp(warp);
				if (loc == null || player == null)
					return true;
				if (!player.hasPermission(warp.getPermission())) {
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
				Long warmup = manager.getTeleportWarmup();
				if (player.hasPermission(warp.getPermission() + ".nowarmup")) {
					warmup = 0L;
				}else{
					player.sendActionBar(Component.text("You will teleport in " + TimeParser.parseLong(warmup/20*1000,false) + ". Don't move!", NamedTextColor.GREEN));
				}
				manager.teleport(player,loc, (success, player1, previousLocation, newLocation, message) -> {
					if (success) {
						player1.sendActionBar(Component.text("Teleported to "+warp.getDisplayName()+"!", NamedTextColor.GREEN));
					}else {
						player1.sendMessage(Component.text(message, NamedTextColor.RED));
					}
				}, warmup);
				return true;
			}
			if (command.getName().equalsIgnoreCase(warp.getSetCommand())) {
				if (player == null) return true;
				if (!player.hasPermission(warp.getPermission()+".set")) {
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
				manager.setWorldWarp(warp,player.getLocation());
				player.sendMessage(Component.text(warp.getDisplayName() + " Location Set!", NamedTextColor.GREEN));
				return true;
			}
		}
		return false;
	}

	private boolean acceptTeleport(Player player, String target) {
		List<TeleportRequest> requests = manager.getTeleportRequestsTargetingPlayer(player);
		if (requests.size() > 0) {
			int acceptedCount = 0;
			for (TeleportRequest request : requests) {
				Player requester = request.getFrom();
				if (requester == null || !requester.isOnline()) {
					continue;
				}
				if (target.length() > 0 && !requester.getName().equalsIgnoreCase(target)) {
					continue;
				}
				acceptedCount ++;
				player.sendMessage(Component.text("You accepted " + requester.getName() + "'s teleport request!", NamedTextColor.GREEN));
				requester.sendMessage(Component.text(player.getName() + " accepted your teleport request!", NamedTextColor.GREEN));
				requester.sendActionBar(Component.text("You will teleport in " + (manager.getTeleportWarmup() / 20) + " seconds. Don't move!", NamedTextColor.GREEN));
				manager.deleteTeleportRequest(request.getId());
				manager.teleport(requester, player.getLocation(), (success, pl, previousLocation, newLocation, message) -> {
					if (success) {
						requester.sendMessage(Component.text(" You teleported to " + pl.getName() + "!", NamedTextColor.GREEN));
						pl.sendMessage(Component.text(requester.getName() + " teleported to you!", NamedTextColor.GREEN));
					} else {
						requester.sendMessage(Component.text(message, NamedTextColor.RED));
						pl.sendMessage(Component.text(message, NamedTextColor.RED));
					}
				},manager.getTeleportRequestWarmupTicks());
			}

			return acceptedCount > 0;
		}else{
			return false;
		}
	}

}
