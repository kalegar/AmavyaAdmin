package me.sanjy33.amavyaadmin.mute;

import java.util.UUID;

import me.sanjy33.amavyaadmin.util.UUIDManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.sanjy33.amavyaadmin.util.TimeParser;
import org.jetbrains.annotations.NotNull;

public class MuteCommandExecutor implements CommandExecutor {

	private final MuteManager manager;
	private final UUIDManager uuidManager;
	
	public MuteCommandExecutor(MuteManager manager, UUIDManager uuidManager) {
		this.manager = manager;
		this.uuidManager = uuidManager;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		final Player player;
		if (sender instanceof Player){
			player = (Player) sender;
		}else{
			player = null;
		}
		if (command.getName().equalsIgnoreCase("mute")){
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
				if (args.length < 3 && !player.hasPermission("aadmin.mute.noreason")) {
					return false;
				}
			}
			if (args.length < 2) {
				return false;
			}
			uuidManager.getUUID(args[0], ((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage(ChatColor.RED + "Player " + name + " not found!");
				} else {
					MutedPlayer mp = manager.getMutedPlayer(uuid);
					if (mp!=null){
						String timeLeft = TimeParser.parseLong(mp.getUnMuteTime()-System.currentTimeMillis(), false);
						sender.sendMessage(ChatColor.RED + "That player is already muted for " + timeLeft + " !");
						return;
					}else{
						long time = TimeParser.parseString(args[1]);
						if (time==-1){
							sender.sendMessage(ChatColor.RED + "Use this time format: 5h3m6s!");
							return;
						}
						String reason = "<Muted By Staff>";
						if (args.length > 2) {
							reason = "";
							int a = 2;
							while (a <= args.length - 1) {
								reason += args[a] + " ";
								a += 1;
							}
						}
						if (player == null) {
							manager.mute(uuid, null, reason, time);
						}else{
							manager.mute(uuid, player.getUniqueId(), reason, time);
						}
						sender.sendMessage(ChatColor.GREEN + "Player " + name + " muted for " + TimeParser.parseLong(time,false) + " for " + reason + "!");
						return;
					}
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("unmute")){
			if (!(player==null)){
				if (!command.testPermission(player)){
					player.sendMessage(ChatColor.RED + "You don't have permission!");
					return true;
				}
			}
			if (args.length < 1){
				return false;
			}
			uuidManager.getUUID(args[0], ((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage(ChatColor.RED + "That player does not exist (or is not online)!");
				} else {
					MutedPlayer mp = manager.getMutedPlayer(uuid);
					if (mp!=null){
						manager.unMute(mp);
						sender.sendMessage(ChatColor.GREEN + args[0] + " has been unmuted!");
					}else{
						sender.sendMessage(ChatColor.RED + "That player is not muted!");
					}
				}
			}));
			return true;
		}
		return false;
	}

}
