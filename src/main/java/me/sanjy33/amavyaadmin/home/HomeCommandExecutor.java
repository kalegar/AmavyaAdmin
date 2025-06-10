package me.sanjy33.amavyaadmin.home;

import java.util.Arrays;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.teleport.TeleportCallback;
import org.jetbrains.annotations.NotNull;

public class HomeCommandExecutor implements CommandExecutor {
	
	private final AmavyaAdmin plugin;
	private final HomeManager manager;
	
	public HomeCommandExecutor(AmavyaAdmin plugin, HomeManager manager) {
		this.plugin = plugin;
		this.manager = manager;
	}

	public boolean onHomeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (player==null){
			sender.sendMessage("This command can't be used in the console.");
			return true;
		}else{
			if (!command.testPermission(player)){
				// TODO: Remove permission messages? Calling testPermission seems to send a message already.
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
		}
		if (args.length>0){
			String subCommand = args[0];
			String[] subArgs = {};
			if (args.length > 1) {
				subArgs = Arrays.copyOfRange(args,1,args.length-1);
			}
			if (subCommand.equalsIgnoreCase("set")) {
				return onSetHomeCommand(sender, plugin.getCommand("sethome"), label, subArgs);
			}else if (subCommand.equalsIgnoreCase("delete")) {
				return onDeleteHomeCommand(sender, plugin.getCommand("deletehome"), label, subArgs);
			}else if (subCommand.equalsIgnoreCase("list")) {
				return onListHomeCommand(sender, plugin.getCommand("listhome"), label, subArgs);
			}else if (subCommand.equalsIgnoreCase("other")) {
				return onOtherHomeCommand(sender, plugin.getCommand("otherhome"), label, subArgs);
			}else{
				return false;
			}
		}
		UUID targetUuid = player.getUniqueId();
		if (!teleportToHome(player, targetUuid)) {
			player.sendMessage(Component.text("You don't have a home! Set one with /home set", NamedTextColor.RED));
		}
		return true;
	}

	public boolean onSetHomeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (player==null){
			sender.sendMessage("This command can't be used in the console.");
			return true;
		}else{
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
		}
		UUID u = player.getUniqueId();
		if (manager.homeExists(u)){
			player.sendMessage(Component.text("You already have a home set! Delete it first with /home delete", NamedTextColor.RED));
			return true;
		}
		PlayerHome home = new PlayerHome(u, player.getLocation().clone(), player.getName());
		manager.setHome(home);
		manager.save();
		player.sendMessage(Component.text("Home set!", NamedTextColor.GREEN));
		return true;
	}

	public boolean onDeleteHomeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (player==null){
			sender.sendMessage("This command can't be used in the console.");
			return true;
		}else{
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
		}
		if (args.length>0){
			if (!player.hasPermission("aadmin.home.delete.other")){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			plugin.uuidManager.getUUID(args[0],((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage(Component.text(args[0] + " has never been on this server.", NamedTextColor.RED));
				}
				if (manager.homeExists(uuid)){
					manager.clearHome(uuid);
					manager.save();
					sender.sendMessage(Component.text(args[0]+"'s home deleted!", NamedTextColor.GREEN));
				}else{
					sender.sendMessage(Component.text(args[0] + " does not have a home!", NamedTextColor.RED));
				}
			}));
		}else{
			UUID u = player.getUniqueId();
			if (manager.homeExists(u)){
				manager.clearHome(u);
				manager.save();
				player.sendMessage(Component.text("Home deleted!", NamedTextColor.GREEN));
			}else{
				player.sendMessage(Component.text("You don't have a home!", NamedTextColor.RED));
			}
		}
		return true;
	}

	public boolean onListHomeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (player!=null){
			if (!command.testPermission(player)){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
		}
		sender.sendMessage(Component.text(manager.getHomeList(), NamedTextColor.AQUA));
		return true;
	}

	public boolean onOtherHomeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (player==null){
			sender.sendMessage("This command can't be used in the console.");
			return true;
		}else if (args.length > 0){
			if (!player.hasPermission("aadmin.home.other")){
				player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
				return true;
			}
			final Player pl = player;
			plugin.uuidManager.getUUID(args[0], ((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage(Component.text(args[0] + " hasn't been on this server!", NamedTextColor.RED));
				} else {
					if (!teleportToHome(pl, uuid)) {
						pl.sendMessage(Component.text(args[0] + "doesn't have a home!", NamedTextColor.RED));
					}
				}
			}));
			return true;
		}
		return false;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

		if (command.getName().equalsIgnoreCase("home")){
			return onHomeCommand(sender, command, label, args);
		}
		if (command.getName().equalsIgnoreCase("sethome")){
			return onSetHomeCommand(sender, command, label, args);
		}
		if (command.getName().equalsIgnoreCase("deletehome")){
			return onDeleteHomeCommand(sender, command, label, args);
		}
		if (command.getName().equalsIgnoreCase("listhome")){
			return onListHomeCommand(sender, command, label, args);
		}
		if (command.getName().equalsIgnoreCase("otherhome")){
			return onOtherHomeCommand(sender, command, label, args);
		}
		return false;
	}

	private boolean teleportToHome(Player player, UUID target) {
		if (manager.homeExists(target)){
			long warmup = manager.getHomeWarpWarmup();
			if (player.hasPermission("aadmin.home.instant")) {
				warmup=1L;
			}else {
				player.sendActionBar(Component.text("Teleporting in " + (warmup/20) + " seconds. Don't move!", NamedTextColor.GREEN));
			}
			PlayerHome home = manager.getHome(target);
			plugin.teleportManager.teleport(player, home.getLocation(), new TeleportCallback() {

				@Override
				public void onTeleport(boolean success, Player player, Location previousLocation,
									   Location newLocation, String message) {
					if (success) {
						player.sendActionBar(Component.text( "Welcome home!", Style.style(NamedTextColor.GREEN, TextDecoration.ITALIC)));
					}else {
						player.sendMessage(Component.text(message, NamedTextColor.RED));
					}

				}

			}, warmup);
			return true;
		}else{
			return false;
		}
	}

}
