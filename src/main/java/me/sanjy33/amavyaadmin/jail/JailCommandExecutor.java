package me.sanjy33.amavyaadmin.jail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.util.TimeParser;
import org.jetbrains.annotations.NotNull;

public class JailCommandExecutor implements CommandExecutor {
	
	private final AmavyaAdmin plugin;
	private final JailManager manager;
	
	public JailCommandExecutor(AmavyaAdmin plugin, JailManager manager) {
		this.plugin = plugin;
		this.manager = manager;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		final Player player;
		if (sender instanceof Player){
			player = (Player) sender;
		}else{
			player = null;
		}
		if (command.getName().equalsIgnoreCase("jail")){
			final String jailerName;
			if (player!=null){
				jailerName=player.getName();
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}else{
				jailerName = "Console";
			}
			if (manager.jailCellCount()<1){
				sender.sendMessage(Component.text("There are no prison cells! Create some with /jailcreate", NamedTextColor.RED));
				return true;
			}
			if (args.length<3){
				return false;
			}
			plugin.uuidManager.getUUID(args[0], ((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage(name + " has never been on this server!");
					return;
				}
				if (manager.isPlayerInJail(uuid)){
					sender.sendMessage(Component.text(args[0] + " is already in jail!", NamedTextColor.RED));
					return;
				}
				long time = TimeParser.parseString(args[1]);
				if (time==-1){
					sender.sendMessage(Component.text("Use this time format: 5h3m6s!", NamedTextColor.RED));
					return;
				}
				StringBuilder reason = new StringBuilder(args[2]);
				for (int i=3;i<args.length;i++){
					reason.append(" ").append(args[i]);
				}
				boolean result = manager.jailPlayer(uuid, player != null ? player.getUniqueId() : null, jailerName, reason.toString(), time);
				if (result) {
					sender.sendMessage(
							Component.text("[Jail] ", NamedTextColor.AQUA)
							.append(Component.text(args[0] + " jailed for " + TimeParser.parseLong(time,true) + ". Reason: "+reason+".", NamedTextColor.WHITE))
					);
					Player target = Bukkit.getPlayer(uuid);
					if (target != null){
						sender.sendMessage(Component.text(args[0] + " was jailed!", NamedTextColor.GREEN));
						target.sendMessage(Component.text("You have been jailed by "+jailerName+" for "+TimeParser.parseLong(time, true)+". Reason: "+reason, NamedTextColor.RED));
						target.sendMessage(Component.text("Use /jailstatus to check how long you have left.", NamedTextColor.RED));
						target.teleport(manager.getCell(uuid).getLocation());
					}else{
						sender.sendMessage(Component.text(args[0] + " will be jailed the next time they login!", NamedTextColor.GREEN));
					}
				}else {
					sender.sendMessage(Component.text(" Failed to jail " + args[0] + ". No empty cells!", NamedTextColor.RED));
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("jailcreate")){
			if (player==null){
				sender.sendMessage("You can't do this in the console.");
				return true;
			}else{
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			JailCell cell = manager.createCell(player.getLocation());
			player.sendMessage(Component.text("Prison cell " + cell.getID() + " created!", NamedTextColor.GREEN));
			return true;
		}
		if (command.getName().equalsIgnoreCase("jaildelete")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			if (args.length<1){
				return false;
			}
			int i = Integer.parseInt(args[0]);
			if (manager.deleteCell(i)) {
				sender.sendMessage(Component.text("Prison cell deleted.", NamedTextColor.GREEN));
			}else {
				sender.sendMessage(Component.text("No prison cell with the id " + i + " exists!", NamedTextColor.RED));
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("jaildeleteall")){
			if (player!=null){
				if (!player.hasPermission("aadmin.jail.deleteall")){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			manager.clearCells();
			sender.sendMessage(Component.text("All prison cells deleted.", NamedTextColor.GREEN));
			return true;
		}
		if (command.getName().equalsIgnoreCase("unjail")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			if (args.length<1){
				return false;
			}
			plugin.uuidManager.getUUID(args[0], ((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage(args[0] + " has never been on this server!");
					return;
				}
				if (manager.unJailPlayer(uuid)) {
					Player target = Bukkit.getPlayer(uuid);
					if (target != null && target.isOnline()){
						target.teleport(target.getLocation().getWorld().getSpawnLocation());
						target.sendMessage(Component.text("You have been released from jail.", NamedTextColor.GREEN));
						sender.sendMessage(Component.text(args[0] + " was released from jail.", NamedTextColor.GREEN));
					}else{
						manager.addToBeReleased(uuid.toString());
						sender.sendMessage(Component.text(args[0] + " will be released when they are next online.", NamedTextColor.GREEN));
					}
				}else {
					sender.sendMessage(Component.text(args[0] + " is not jailed!", NamedTextColor.RED));
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("unjailall")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			for (JailCell p : manager.getJailCells()){
					UUID u = p.getOccupant();
					if (u!=null){
						Player target = Bukkit.getPlayer(u);
						if (target == null) {
							continue;
						}
						if (target.isOnline()){
							target.teleport(target.getLocation().getWorld().getSpawnLocation());
							target.sendMessage(Component.text("You have been released from jail.", NamedTextColor.GREEN));
							sender.sendMessage(Component.text(target.getName() + " was released from jail.", NamedTextColor.GREEN));
						}else{
							manager.addToBeReleased(u.toString());
							sender.sendMessage(Component.text(target.getName() + " will be released when they are next online.", NamedTextColor.GREEN));
						}
					}
					p.reset();
					return true;
			}
		}
		if (command.getName().equalsIgnoreCase("jailstatus")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			final String lookupName;
			final String messageName;
			if (args.length == 0) {
				if (player==null){
					sender.sendMessage("/jailstatus <player>");
					return true;
				}
				lookupName = player.getName();
				messageName = "You";
			}else{
				if (player!=null){
					if (!player.hasPermission("aadmin.jail.status.other")){
						player.sendMessage(Component.text("Usage: /jailstatus", NamedTextColor.RED));
						return true;
					}
				}
				lookupName = args[0];
				messageName = args[0];
			}
			plugin.uuidManager.getUUID(lookupName, ((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage("Could not find player named " + lookupName);
				} else {
					getJailStatus(sender,player,uuid,messageName);
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("jailaddtime")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			if (args.length<2){
				return false;
			}
			plugin.uuidManager.getUUID(args[0], ((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage("Could not find player named " + args[0]);
					return;
				}
				long time = TimeParser.parseString(args[1]);
				if (time==-1){
					sender.sendMessage(Component.text("Use this time format: 5h3m6s!", NamedTextColor.RED));
					return;
				}
				if (manager.addTime(uuid, time)) {
					sender.sendMessage(Component.text("Successfully added time.", NamedTextColor.GREEN));
				} else {
					sender.sendMessage(Component.text(args[0] + " is not jailed!", NamedTextColor.RED));
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("jailsubtracttime")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			if (args.length<2){
				return false;
			}
			plugin.uuidManager.getUUID(args[0],((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage("Could not find player named " + args[0]);
					return;
				}
				long time = TimeParser.parseString(args[1]);
				if (time==-1){
					sender.sendMessage(Component.text("Use this time format: 5h3m6s!", NamedTextColor.RED));
					return;
				}
				if (manager.addTime(uuid, -time)) {
					sender.sendMessage(Component.text("Successfully subtracted time.", NamedTextColor.GREEN));
				} else {
					sender.sendMessage(Component.text(args[0] + " is not jailed!", NamedTextColor.RED));
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("jaillist")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			final List<UUID> uuids = new ArrayList<>();
			for (JailCell cell : manager.getJailCells()) {
				if (cell.getOccupant() != null)
					uuids.add(cell.getOccupant());
			}
			plugin.uuidManager.getNames(uuids, ((names, uuids1) -> {
				StringBuilder sb = new StringBuilder("Jail cells: ");
				int i = 0;
				for (JailCell p : manager.getJailCells()){
					String name = "Empty";
					if (p.getOccupant() != null) {
						name = names.get(i);
						i++;
					}
					sb.append(p.getID()).append(": ").append(name).append(", ");
				}
				sb.delete(sb.length()-2, sb.length());
				sender.sendMessage(sb.toString());
			}));
			return true;
		}
		return false;
	}

	private void getJailStatus(CommandSender sender, Player player, UUID uuid, String name) {
		JailCell cell = manager.getCell(uuid);
		if (cell != null){
			long timedif = cell.getTimeWhenReleased()-System.currentTimeMillis();
			String timeString = timedif > 5 ? TimeParser.parseLong(timedif, false) : "approximately 5 seconds.";
			if (player != null && uuid.equals(player.getUniqueId())) {
				sender.sendMessage("You will be released in "+timeString);
				sender.sendMessage("You were jailed by " + cell.getJailerName() + " for " + cell.getReason());
			}else {
				sender.sendMessage(name + " will be released in "+timeString);
				sender.sendMessage(name + " was jailed by " + cell.getJailerName() + " for " + cell.getReason());
			}
		} else {
			if (player != null && uuid.equals(player.getUniqueId())) {
				sender.sendMessage(Component.text("You are not in jail!", NamedTextColor.RED));
			}else {
				sender.sendMessage(Component.text(name + " is not in jail!", NamedTextColor.RED));
			}
		}
	}

}
