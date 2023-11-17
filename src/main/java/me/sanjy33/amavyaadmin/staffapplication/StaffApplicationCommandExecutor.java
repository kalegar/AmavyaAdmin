package me.sanjy33.amavyaadmin.staffapplication;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
import org.jetbrains.annotations.NotNull;

public class StaffApplicationCommandExecutor implements CommandExecutor {
	
	private final AmavyaAdmin plugin;
	private final StaffApplicationManager manager;
	
	public StaffApplicationCommandExecutor(AmavyaAdmin plugin, StaffApplicationManager manager) {
		this.plugin = plugin;
		this.manager = manager;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}
		if (command.getName().equalsIgnoreCase("apply")){
			if (player==null){
				sender.sendMessage("This command can't be used in the console!");
				return true;
			}
			if (StaffApplicationManager.simpleApplications) {
				sender.sendMessage(StaffApplicationManager.applicationSimpleMessage);
				return true;
			}
			if (args.length>=1) {
				if (args[0].equalsIgnoreCase("submit")) {
					if (!player.hasPermission("aadmin.staffapps.apply")) {
						player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
						return true;
					}
					if (!manager.isApplyingForStaff(player)) {
						player.sendMessage(Component.text("Please use ",NamedTextColor.RED)
								.append(Component.text( "/apply", NamedTextColor.AQUA))
								.append(Component.text(" first!", NamedTextColor.RED)));
						return true;
					}
					ItemStack i = player.getInventory().getItemInMainHand();
					if ((!i.getType().equals(Material.WRITABLE_BOOK)) && (!i.getType().equals(Material.WRITTEN_BOOK))) {
						player.sendMessage(Component.text("Hold the application book in your hand and use /apply submit", NamedTextColor.RED));
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
					player.sendMessage(Component.text("Application submitted successfully!", NamedTextColor.GREEN));
					player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					for (OfflinePlayer p : Bukkit.getOperators()) {
						if (p.isOnline()) {
							p.getPlayer().sendMessage(Component.text("[App] " + player.getName() + " just submitted a staff application!", NamedTextColor.AQUA));
						}
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("cancel")) {
					if (!player.hasPermission("aadmin.staffapps.apply")) {
						player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
						return true;
					}
					if (manager.isApplyingForStaff(player)) {
						player.sendMessage(Component.text("You have cancelled your application.", NamedTextColor.GREEN));
						manager.setApplyingForStaff(player.getUniqueId(), false);
						if (player.getInventory().contains(Material.WRITABLE_BOOK)) {
							player.getInventory().remove(new ItemStack(Material.WRITABLE_BOOK, 1));
						}
						return true;
					}
					player.sendMessage(Component.text("You aren't currently applying for staff! To apply, use /apply", NamedTextColor.RED));
					return true;
				}
				if (args[0].equalsIgnoreCase("accept")) {
					if (!player.hasPermission("aadmin.staffapps.accept")) {
						player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(Component.text("Usage: /apply accept <player>", NamedTextColor.RED));
						return true;
					}
					StaffApplication app = manager.getApplication(args[1]);
					if (app == null) {
						player.sendMessage(Component.text(args[1] + " has not submitted an application!", NamedTextColor.RED));
						return true;
					}
					setPermsGroup(app.getUUID(), plugin.staffPermissionGroup);
					player.sendMessage(Component.text(args[1] + "'s application was accepted!", NamedTextColor.GREEN));
					plugin.messageManager.sendMessage(app.getUUID(), StaffApplicationManager.applicationAcceptedMessage);
					manager.deleteApplication(app.getUUID());
					return true;
				}
				if (args[0].equalsIgnoreCase("reject") || args[0].equalsIgnoreCase("deny")) {
					if (!player.hasPermission("aadmin.staffapps.deny")) {
						player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(Component.text("Usage: /apply deny <player>", NamedTextColor.RED));
						return true;
					}
					StaffApplication app = manager.getApplication(args[1]);
					if (app == null) {
						player.sendMessage(Component.text(args[1] + " has not submitted an application!", NamedTextColor.RED));
						return true;
					}
					plugin.messageManager.sendMessage(app.getUUID(), StaffApplicationManager.applicationDeniedMessage);
					player.sendMessage(Component.text(args[1] + "'s application was rejected.", NamedTextColor.RED));
					manager.deleteApplication(app.getUUID());
					return true;
				}
				if (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("read")) {
					if (!player.hasPermission("aadmin.staffapps.check")) {
						player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(Component.text("Staff Applications: (Use /apply check <name>)", NamedTextColor.DARK_GREEN));
						if (player.hasPermission("aadmin.staffapps.accept")) {
							player.sendMessage(Component.text("To accept/deny, use /apply <accept/deny> <name>", NamedTextColor.DARK_GREEN));
						}
						player.sendMessage(Component.text("Unread", NamedTextColor.AQUA)
								.append(Component.text(" - ", NamedTextColor.WHITE))
								.append(Component.text("Read", NamedTextColor.DARK_GRAY))
						);
						for (StaffApplication app : manager.getApplications()) {
							if (app.isRead()) {
								player.sendMessage(Component.text(" - " + app.getLastKnownName(), NamedTextColor.DARK_GRAY));
							} else {
								player.sendMessage(Component.text(" - " + app.getLastKnownName(), NamedTextColor.AQUA));
							}
						}
						return true;
					}
					StaffApplication app = manager.getApplication(args[1]);
					if (app == null) {
						player.sendMessage(Component.text("There is no application from player " + args[1], NamedTextColor.RED));
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
					player.sendMessage(Component.text("You have been given a book with the application!", NamedTextColor.GREEN));
					return true;
				}
				if (args[0].equalsIgnoreCase("delete")) {
					if (!player.hasPermission("aadmin.staffapps.delete")) {
						player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
						return true;
					}
					if (args.length < 2) {
						player.sendMessage(Component.text("Usage: /apply delete <name>", NamedTextColor.RED));
						return true;
					}
					if (manager.deleteApplication(args[1])) {
						player.sendMessage(Component.text("Application '" + args[1] + "' deleted!", NamedTextColor.GREEN));
					} else {
						player.sendMessage(Component.text("There is no application from player " + args[1], NamedTextColor.RED));
					}
					return true;
				}
				if (args[0].equalsIgnoreCase("deleteall")) {
					if (!player.hasPermission("aadmin.staffapps.delete")) {
						player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
						return true;
					}
					manager.clearApplications();
					player.sendMessage(Component.text("All applications deleted!", NamedTextColor.GREEN));
					return true;
				}
				if (args[0].equalsIgnoreCase("deleteread")) {
					if (!player.hasPermission("aadmin.staffapps.delete")) {
						player.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
						return true;
					}
					manager.deleteReadApplications();
					player.sendMessage(Component.text("All read applications deleted!", NamedTextColor.GREEN));
					return true;
				}
			}
			if (!player.hasPermission("aadmin.staffapps.apply")){
				player.sendMessage(Component.text("You are already a staff-member!", NamedTextColor.RED));
				player.sendMessage(Component.text("Did you mean to use '/apply check' ?", NamedTextColor.RED));
				return true;
			}
			if (manager.isApplyingForStaff(player)){
				player.sendMessage(Component.text("You are already applying for staff!", NamedTextColor.RED));
				player.sendMessage(Component.text("If you want to submit your application, use /apply submit", NamedTextColor.RED));
				player.sendMessage(Component.text("If you want to cancel, use /apply cancel", NamedTextColor.RED));
				return true;
			}
			UUID uuid = player.getUniqueId();
			if (manager.hasApplied(uuid)) {
				player.sendMessage(StaffApplicationManager.applicationInProgressMessage);
				return true;
			}
			ItemStack i = manager.getApplicationBook();
			player.getInventory().addItem(i);
			player.sendMessage(Component.text("Write your application in the book given.", NamedTextColor.GREEN));
			player.sendMessage(Component.text(" - Use ", NamedTextColor.GREEN)
					.append(Component.text("/apply submit", NamedTextColor.AQUA))
					.append(Component.text(" to send in your app", NamedTextColor.GREEN))
			);
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
