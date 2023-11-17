package me.sanjy33.amavyaadmin.mute;


import me.sanjy33.amavyaadmin.util.UUIDManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
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
					sender.sendMessage(Component.text("Player " + name + " not found!", NamedTextColor.RED));
				} else {
					MutedPlayer mp = manager.getMutedPlayer(uuid);
					if (mp!=null){
						String timeLeft = TimeParser.parseLong(mp.getUnMuteTime()-System.currentTimeMillis(), false);
						sender.sendMessage(Component.text("That player is already muted for " + timeLeft + " !",NamedTextColor.RED));
						return;
					}else{
						long time = TimeParser.parseString(args[1]);
						if (time==-1){
							sender.sendMessage(Component.text("Use this time format: 5h3m6s!", NamedTextColor.RED));
							return;
						}
						StringBuilder reason = new StringBuilder();
						if (args.length > 2) {
							int a = 2;
							while (a <= args.length - 1) {
								if (reason.length() > 0) {
									reason.append(" ");
								}
								reason.append(args[a]);
								a += 1;
							}
						}else{
							reason.append("<Muted By Staff>");
						}
						if (player == null) {
							manager.mute(uuid, null, reason.toString(), time);
						}else{
							manager.mute(uuid, player.getUniqueId(), reason.toString(), time);
						}
						sender.sendMessage(Component.text("Player " + name + " muted for " + TimeParser.parseLong(time,false) + " for " + reason + "!", NamedTextColor.GREEN));
					}
				}
			}));
			return true;
		}
		if (command.getName().equalsIgnoreCase("unmute")){
			if (!(player==null)){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}
			if (args.length < 1){
				return false;
			}
			uuidManager.getUUID(args[0], ((name, uuid) -> {
				if (uuid == null) {
					sender.sendMessage(Component.text("That player does not exist (or is not online)!", NamedTextColor.RED));
				} else {
					MutedPlayer mp = manager.getMutedPlayer(uuid);
					if (mp!=null){
						manager.unMute(mp);
						sender.sendMessage(Component.text(args[0] + " has been unmuted!", NamedTextColor.GREEN));
					}else{
						sender.sendMessage(Component.text("That player is not muted!", NamedTextColor.RED));
					}
				}
			}));
			return true;
		}
		return false;
	}

}
