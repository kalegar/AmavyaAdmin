package me.sanjy33.amavyaadmin.staffapplication;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

public class StaffApplicationCommandExecutor implements CommandExecutor {
	
	private final AmavyaAdmin plugin;
	private final StaffApplicationManager manager;
	
	public StaffApplicationCommandExecutor(AmavyaAdmin plugin, StaffApplicationManager manager) {
		this.plugin = plugin;
		this.manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (command.getName().equalsIgnoreCase("apply")){
			if (player==null){
				sender.sendMessage("This command can't be used in the console!");
				return true;
			}
			if (args.length>=1){
				if (args[0].equalsIgnoreCase("submit")){
					if (!player.hasPermission("aadmin.staffapps.apply")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					if (!manager.isApplyingForStaff(player)){
						player.sendMessage(ChatColor.RED + "Please use " + ChatColor.AQUA + "/apply" + ChatColor.RED + " first!");
						return true;
					}
					ItemStack i = player.getInventory().getItemInMainHand();
					if (i==null || ((!i.getType().equals(Material.WRITABLE_BOOK)) && (!i.getType().equals(Material.WRITTEN_BOOK)))){
						player.sendMessage(ChatColor.RED + "Hold the application book in your hand and use /apply submit");
						return true;
					}
					BookMeta b = (BookMeta) i.getItemMeta();
					List<String> pages = b.getPages();
					UUID uuid = player.getUniqueId();
					if (manager.hasApplied(uuid)) {
						player.sendMessage(StaffApplicationManager.applicationInProgressMessage);
						return true;
					}
					StaffApplication app = new StaffApplication(
							uuid,
							player.getName(),
							pages,
							false);
					manager.addApplication(app);
					manager.setApplyingForStaff(uuid, false);
					player.sendMessage(ChatColor.GREEN + "Application submitted successfully!");
					player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					for (OfflinePlayer p : Bukkit.getOperators()){
						if (p.isOnline()){
							p.getPlayer().sendMessage(ChatColor.AQUA + "[App] " + player.getName() + " just submitted a staff application!");
						}
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("cancel")){
					if (!player.hasPermission("aadmin.staffapps.apply")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					if (manager.isApplyingForStaff(player)){
						player.sendMessage(ChatColor.GREEN + "You have cancelled your application.");
						manager.setApplyingForStaff(player.getUniqueId(), false);
						if (player.getInventory().contains(Material.WRITABLE_BOOK)){
							player.getInventory().remove(new ItemStack(Material.WRITABLE_BOOK,1));
						}
						return true;
					}
					player.sendMessage(ChatColor.RED + "You aren't currently applying for staff! To apply, use /apply");
					return true;
				}
				if (args[0].equalsIgnoreCase("accept")){
					if (!player.hasPermission("aadmin.staffapps.accept")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					if (args.length < 2){
						player.sendMessage(ChatColor.RED + "Usage: /apply accept <player>");
						return true;
					}
					StaffApplication app = manager.getApplication(args[1]);
					if (app == null){
						player.sendMessage(ChatColor.RED + args[1] + " has not submitted an application!");
						return true;
					}
					setPermsGroup(app.getUUID(),plugin.staffPermissionGroup);
					player.sendMessage(ChatColor.GREEN + args[1] + "'s application was accepted!");
					plugin.messageManager.sendMessage(app.getUUID(), StaffApplicationManager.applicationAcceptedMessage);
					manager.deleteApplication(app.getUUID());
					return true;
				}
				if (args[0].equalsIgnoreCase("reject") || args[0].equalsIgnoreCase("deny")){
					if (!player.hasPermission("aadmin.staffapps.deny")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					if (args.length < 2){
						player.sendMessage(ChatColor.RED + "Usage: /apply deny <player>");
						return true;
					}
					StaffApplication app = manager.getApplication(args[1]);
					if (app == null){
						player.sendMessage(ChatColor.RED + args[1] + " has not submitted an application!");
						return true;
					}
					plugin.messageManager.sendMessage(app.getUUID(), StaffApplicationManager.applicationDeniedMessage);
					player.sendMessage(ChatColor.RED + args[1] + "'s application was rejected.");
					manager.deleteApplication(app.getUUID());
					return true;
				}
				if (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("read")){
					if (!player.hasPermission("aadmin.staffapps.check")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					if (args.length < 2){
						player.sendMessage(ChatColor.DARK_GREEN + "Staff Applications: (Use /apply check <name>)");
						if (player.hasPermission("aadmin.staffapps.accept")){
							player.sendMessage(ChatColor.DARK_GREEN + "To accept/deny, use /apply <accept/deny> <name>");
						}
						player.sendMessage(ChatColor.AQUA + "Unread" + ChatColor.WHITE + " - " + ChatColor.DARK_GRAY + "Read");
						for (StaffApplication app : manager.getApplications()){
							if (app.isRead()){
								player.sendMessage(ChatColor.DARK_GRAY + " - " + app.getLastKnownName());
							}else{
								player.sendMessage(ChatColor.AQUA + " - " + app.getLastKnownName());
							}
						}
						return true;
					}
					StaffApplication app = manager.getApplication(args[1]);
					if (app == null){
						player.sendMessage(ChatColor.RED + "There is no application from player " + args[1]);
						return true;
					}
					app.setRead(true);
					List<String> pages = app.getApplicationPages();
					ItemStack i = new ItemStack(Material.WRITTEN_BOOK, 1);
					BookMeta b = (BookMeta) i.getItemMeta();
					b.setTitle("Staff Application");
					b.setAuthor(args[1]);
					b.setPages(pages);
					i.setItemMeta(b);
					player.getInventory().addItem(i);
					player.sendMessage(ChatColor.GREEN + "You have been given a book with the application!");
					return true;
				}
				if (args[0].equalsIgnoreCase("delete")){
					if (!player.hasPermission("aadmin.staffapps.delete")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					if (args.length < 2){
						player.sendMessage(ChatColor.RED + "Usage: /apply delete <name>");
						return true;
					}
					if (manager.deleteApplication(args[1])) {
						player.sendMessage(ChatColor.GREEN + "Application '" + args[1] + "' deleted!");
						return true;
					}else {
						player.sendMessage(ChatColor.RED + "There is no application from player " + args[1]);
						return true;
					}
				}
				if (args[0].equalsIgnoreCase("deleteall")){
					if (!player.hasPermission("aadmin.staffapps.delete")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					manager.clearApplications();
					player.sendMessage(ChatColor.GREEN + "All applications deleted!");
					return true;
				}
				if (args[0].equalsIgnoreCase("deleteread")){
					if (!player.hasPermission("aadmin.staffapps.delete")){
						player.sendMessage(ChatColor.RED + "You don't have permission!");
						return true;
					}
					manager.deleteReadApplications();
					player.sendMessage(ChatColor.GREEN + "All read applications deleted!");
					return true;
				}
			}
			if (!player.hasPermission("aadmin.staffapps.apply")){
				player.sendMessage(ChatColor.RED + "You are already a staff-member!");
				player.sendMessage(ChatColor.RED + "Perhaps you meant /apply check");
				return true;
			}
			if (manager.isApplyingForStaff(player)){
				player.sendMessage(ChatColor.RED + "You are already applying for staff!");
				player.sendMessage(ChatColor.RED + "If you want to submit your application, use /apply submit");
				player.sendMessage(ChatColor.RED + "If you want to cancel, use /apply cancel");
				return true;
			}
			UUID uuid = player.getUniqueId();
			if (manager.hasApplied(uuid)) {
				player.sendMessage(StaffApplicationManager.applicationInProgressMessage);
				return true;
			}
			ItemStack i = manager.getApplicationBook();
			player.getInventory().addItem(i);
			player.sendMessage(ChatColor.GREEN + "Write your application in the book given.");
			player.sendMessage(ChatColor.GREEN + " - Use " + ChatColor.AQUA + "/apply submit" + ChatColor.GREEN + " to send in your app");
			manager.setApplyingForStaff(uuid, true);
			return true;
		}
		return false;
	}
	
	private void setPermsGroup(UUID uuid, String groupName) {
		CompletableFuture<User> userFuture = plugin.luckPermsApi.getUserManager().loadUser(uuid);
		
		userFuture.thenAcceptAsync(user -> {
			Set<InheritanceNode> currentGroups = user.getNodes().stream()
					.filter(NodeType.INHERITANCE::matches)
					.map(NodeType.INHERITANCE::cast)
					.collect(Collectors.toSet());
			for (InheritanceNode node : currentGroups) {
				user.data().remove(node);
			}
			user.data().add(InheritanceNode.builder(groupName).build());
			plugin.luckPermsApi.getUserManager().saveUser(user);
		});
	}

}
