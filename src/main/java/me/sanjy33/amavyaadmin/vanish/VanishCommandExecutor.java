package me.sanjy33.amavyaadmin.vanish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishCommandExecutor implements CommandExecutor {
	
	private final VanishManager manager;
	
	public VanishCommandExecutor(VanishManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (command.getName().equalsIgnoreCase("fakejoin")) {
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			Bukkit.broadcast(Component.text(player.getName() + " joined the game", NamedTextColor.YELLOW));
			if (manager.isPlayerInvisible(player)) {
				player.sendMessage(Component.text("(You are now visible)", NamedTextColor.GRAY));
				manager.toggleInvisibility(player);
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("fakequit")) {
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			Bukkit.broadcast(Component.text(player.getName() + " left the game", NamedTextColor.YELLOW));
			if (!manager.isPlayerInvisible(player)) {
				player.sendMessage(Component.text("(You are now invisible)", NamedTextColor.GRAY));
				manager.toggleInvisibility(player);
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("silentjoin")) {
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			if (manager.getSilentJoin(player.getUniqueId())) {
				manager.disableSilentJoin(player.getUniqueId());
				player.sendMessage(Component.text("Silent join ", NamedTextColor.GOLD)
						.append(Component.text("disabled", NamedTextColor.RED))
						.append(Component.text("!", NamedTextColor.GOLD))
				);
			}else {
				manager.enableSilentJoin(player.getUniqueId());
				player.sendMessage(Component.text("Silent join ", NamedTextColor.GOLD)
						.append(Component.text("enabled", NamedTextColor.GREEN))
						.append(Component.text("!", NamedTextColor.GOLD))
				);
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("silentquit")) {
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			if (manager.getSilentQuit(player.getUniqueId())) {
				manager.disableSilentQuit(player.getUniqueId());
				player.sendMessage(Component.text("Silent quit ", NamedTextColor.GOLD)
						.append(Component.text("disabled", NamedTextColor.RED))
						.append(Component.text("!", NamedTextColor.GOLD))
				);
			}else {
				manager.enableSilentQuit(player.getUniqueId());
				player.sendMessage(Component.text("Silent quit ", NamedTextColor.GOLD)
						.append(Component.text("enabled", NamedTextColor.GREEN))
						.append(Component.text("!", NamedTextColor.GOLD))
				);
			}
			return true;
		}
		if (command.getName().equalsIgnoreCase("vanish")) {
			if (player!=null){
				if (!command.testPermission(player)){
					player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					return true;
				}
			}else{
				sender.sendMessage("That can't be used in the console!");
				return true;
			}
			manager.toggleInvisibility(player);
			if (manager.isPlayerInvisible(player)) {
				player.sendMessage(Component.text("You are now ", NamedTextColor.GOLD)
						.append(Component.text("invisible", NamedTextColor.GRAY))
				);
			}else {
				player.sendMessage(Component.text("You are now ", NamedTextColor.GOLD)
						.append(Component.text("visible", NamedTextColor.WHITE))
				);
			}
			return true;
		}
		return false;
	}

}
