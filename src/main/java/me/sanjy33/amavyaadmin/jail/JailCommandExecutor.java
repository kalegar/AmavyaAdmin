package me.sanjy33.amavyaadmin.jail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.util.TimeParser;

public class JailCommandExecutor implements CommandExecutor {
	
	private final AmavyaAdmin plugin;
	private final JailManager manager;
	
	public JailCommandExecutor(AmavyaAdmin plugin, JailManager manager) {
		this.plugin = plugin;
		this.manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
				if (!player.hasPermission("aadmin.jail.jail")){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}else{
				jailerName = "Console";
			}
			if (manager.jailCellCount()<1){
				sender.sendMessage(ChatColor.RED + "There are no prison cells! Create some with /jailcreate");
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
					sender.sendMessage(ChatColor.RED + args[0] + " is already in jail!");
					return;
				}
				long time = TimeParser.parseString(args[1]);
				if (time==-1){
					sender.sendMessage(ChatColor.RED + "Use this time format: 5h3m6s!");
					return;
				}
				String reason = args[2];
				for (int i=3;i<args.length;i++){
					reason += " " + args[i];
				}
				boolean result = manager.jailPlayer(uuid, player != null ? player.getUniqueId() : null, jailerName, reason, time);
				if (result) {
					sender.sendMessage(ChatColor.AQUA+"[Jail] "+ChatColor.WHITE+ args[0] + " jailed for " + TimeParser.parseLong(time,true) + ". Reason: "+reason+".");
					Player target = Bukkit.getPlayer(uuid);
					if (target != null){
						sender.sendMessage(ChatColor.GREEN + args[0] + " was jailed!");
						target.sendMessage(ChatColor.RED + "You have been jailed by "+jailerName+" for "+TimeParser.parseLong(time, true)+". Reason: "+reason);
						target.sendMessage(ChatColor.RED + "Use /jailstatus to check how long you have left.");
						target.teleport(manager.getCell(uuid).getLocation());
					}else{
						sender.sendMessage(ChatColor.GREEN + args[0] + " will be jailed the next time they login!");
					}
				}else {
					sender.sendMessage(ChatColor.RED + " Failed to jail " + args[0] + ". No empty cells!");
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("jailcreate")){
			if (player==null){
				sender.sendMessage("You can't do this in the console.");
				return true;
			}else{
				if (!player.hasPermission("aadmin.jail.create")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
					return true;
				}
			}
			JailCell cell = manager.createCell(player.getLocation());
			player.sendMessage(ChatColor.GREEN + "Prison cell " + cell.getID() + " created!");
			return true;
		}
		if (command.getName().equalsIgnoreCase("jaildelete")){
			if (player!=null){
				if (!player.hasPermission("aadmin.jail.delete")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
					return true;
				}
			}
			if (args.length<1){
				return false;
			}
			int i = Integer.parseInt(args[0]);
			if (manager.deleteCell(i)) {
				sender.sendMessage(ChatColor.GREEN + "Prison cell deleted.");
			}else {
				sender.sendMessage(ChatColor.RED + "No prison cell with the id " + i + " exists!");
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("jaildeleteall")){
			if (player!=null){
				if (!player.hasPermission("aadmin.jail.deleteall")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
					return true;
				}
			}
			manager.clearCells();
			sender.sendMessage(ChatColor.GREEN + "All prison cells deleted.");
			return true;
		}
		if (command.getName().equalsIgnoreCase("unjail")){
			if (player!=null){
				if (!player.hasPermission("aadmin.jail.unjail")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
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
						target.sendMessage(ChatColor.GREEN + "You have been released from jail.");
						sender.sendMessage(ChatColor.GREEN+args[0] + " was released from jail.");
					}else{
						manager.addToBeReleased(uuid.toString());
						sender.sendMessage(ChatColor.GREEN+args[0] + " will be released when they are next online.");
					}
				}else {
					sender.sendMessage(ChatColor.RED+args[0] + " is not jailed!");
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("unjailall")){
			if (player!=null){
				if (!player.hasPermission("aadmin.jail.unjailall")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
					return true;
				}
			}
			for (JailCell p : manager.getJailCells()){
					UUID u = p.getOccupant();
					if (u!=null){
						Player target = Bukkit.getPlayer(u);
						if (target.isOnline()){
							target.teleport(target.getLocation().getWorld().getSpawnLocation());
							target.sendMessage(ChatColor.GREEN + "You have been released from jail.");
							sender.sendMessage(ChatColor.GREEN+target.getName() + " was released from jail.");
						}else{
							manager.addToBeReleased(u.toString());
							sender.sendMessage(ChatColor.GREEN+target.getName() + " will be released when they are next online.");
						}
					}
					p.reset();
					return true;
			}
		}
		if (command.getName().equalsIgnoreCase("jailstatus")){
			if (player!=null){
				if (!player.hasPermission("aadmin.jail.jailstatus")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
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
					if (!player.hasPermission("aadmin.jail.jailstatus.other")){
						player.sendMessage(ChatColor.RED+"Usage: /jailstatus");
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
				if (!player.hasPermission("aadmin.jail.addtime")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
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
					sender.sendMessage(ChatColor.RED + "Use this time format: 5h3m6s!");
					return;
				}
				if (manager.addTime(uuid, time)) {
					sender.sendMessage(ChatColor.GREEN + "Successfully added time.");
				} else {
					sender.sendMessage(ChatColor.RED + args[0] + " is not jailed!");
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("jailsubtracttime")){
			if (player!=null){
				if (!player.hasPermission("aadmin.jail.subtracttime")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
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
					sender.sendMessage(ChatColor.RED + "Use this time format: 5h3m6s!");
					return;
				}
				if (manager.addTime(uuid, -time)) {
					sender.sendMessage(ChatColor.GREEN + "Successfully subtracted time.");
				} else {
					sender.sendMessage(ChatColor.RED + args[0] + " is not jailed!");
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("jaillist")){
			if (player!=null){
				if (!player.hasPermission("aadmin.jail.jaillist")){
					player.sendMessage(ChatColor.RED+"You don't have permission!");
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
				sender.sendMessage(ChatColor.RED + "You are not in jail!");
			}else {
				sender.sendMessage(ChatColor.RED + name + " is not in jail!");
			}
		}
	}

}
