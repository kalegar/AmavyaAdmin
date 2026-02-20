package me.sanjy33.amavyaadmin.staffapplication;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.InlinedRegistryBuilderProvider;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;

public class StaffApplicationManager extends SystemManager{
	
	private final String fileName = "staff_applications.yml";
	private final AmavyaAdmin plugin;
	private StaffApplicationCommandExecutor commandExecutor;
	
	private final Map<UUID,StaffApplication> applications = new HashMap<UUID,StaffApplication>();
	private final Set<UUID> applyingForStaff = new HashSet<UUID>();
	private final List<String> applicationPages = new ArrayList<String>();
	private final Dialog applicationDialog;
	public static String applicationAcceptedMessage = "";
	public static String applicationDeniedMessage = "";
	public static String applicationInProgressMessage = "";
	public static String applicationSimpleMessage = "";
	public static Boolean simpleApplications = false;
	
	public StaffApplicationManager(AmavyaAdmin plugin) {
		super();
		this.plugin = plugin;
		registerCommands();
		load();

//		RegistryKeySet<Dialog> registryKeySet = RegistrySet.keySet(RegistryKey.DIALOG);
//		RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG);
//		TypedKey<Dialog> key = DialogKeys.create(Key.key("amavyaadmin:staff_application"));
		applicationDialog = Dialog.create(cons -> {

			List<DialogBody> bodyList = new ArrayList<>();
			List<DialogInput> inputList = new ArrayList<>();

			bodyList.add(DialogBody.plainMessage(
					Component.text("Please fill out the following fields to apply to be a staff member on ",NamedTextColor.WHITE)
							.append(Component.text("TimelessCraft", NamedTextColor.AQUA))
							.append(Component.text("!",NamedTextColor.WHITE)))
			);

			inputList.add(DialogInput.text(
					"email",
					160,
					Component.text("Email Address"),
					true,
					"",
					255,
					null));
			inputList.add(DialogInput.text(
					"pluginexperience",
					160,
					Component.text("Spigot/Paper plugin experience"),
					true,
					"Please describe any experience you have using spigot / paper server plugins (plugin names and familiarity",
					1024,
					TextDialogInput.MultilineOptions.create(8,120)
			));
			inputList.add(DialogInput.text(
					"staffexperience",
					160,
					Component.text("Prior staff experience"),
					true,
					"Please describe any experience you have as staff on other minecraft servers",
					1024,
					TextDialogInput.MultilineOptions.create(8,120)
			));
			inputList.add(DialogInput.text(
					"history",
					160,
					Component.text("History on ")
							.append(Component.text("TimelessCraft",NamedTextColor.AQUA)),
					true,
					"Please describe your history on TimelessCraft!",
					1024,
					TextDialogInput.MultilineOptions.create(8,120)
			));
			inputList.add(DialogInput.text(
					"bans",
					160,
					Component.text("Previous bans and reasons"),
					true,
					"Please describe any times you have been banned previously and why",
					1024,
					TextDialogInput.MultilineOptions.create(8,120)
			));
			inputList.add(DialogInput.text(
					"otherinfo",
					160,
					Component.text("Other info"),
					true,
					"",
					1024,
					TextDialogInput.MultilineOptions.create(8,120)
			));

			List<ActionButton> actions = new ArrayList<>();
			actions.add(
					ActionButton.create(
							Component.text("Submit"),
							null,
							80,
							DialogAction.customClick((resp, audience) -> {
								Optional<UUID> uuidOpt = audience.get(Identity.UUID);
								if (uuidOpt.isPresent()) {
									if (hasApplied(uuidOpt.get())) {
										audience.sendMessage(Component.text("You have already applied.",NamedTextColor.RED));
										return;
									}

									plugin.getLogger().info("uuid: " + uuidOpt.get());
									String email = resp.getText("email");
									if (email == null || email.isEmpty()) {
										audience.sendMessage(Component.text("Application failed. Email address is required.",NamedTextColor.RED));
										return;
									}
									plugin.getLogger().info("email: " + email);

									Map<String, String> fields = new HashMap<>();
									fields.put("email", resp.getText("email"));
									fields.put("pluginexperience", resp.getText("pluginexperience"));
									fields.put("staffexperience", resp.getText("staffexperience"));
									fields.put("history", resp.getText("history"));
									fields.put("bans", resp.getText("bans"));
									fields.put("otherinfo", resp.getText("otherinfo"));
									StaffApplication application = new StaffApplication(uuidOpt.get(), "", fields, false);
									addApplication(application);

									audience.sendMessage(Component.text("Application Submitted!",NamedTextColor.AQUA));
								}
							}, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
					)
			);
			actions.add(
					ActionButton.create(
							Component.text("Cancel"),
							null,
							80,
							null
					)
			);

			cons
					.empty()
					.type(DialogType.multiAction(actions).build())
					.base(DialogBase.create(
						Component.text("TimelessCraft Staff Application"),
						Component.text("staff_application"),
						true,
						false,
						DialogBase.DialogAfterAction.CLOSE,
						bodyList,
						inputList
					));
		});
	}

	public void showDialog(Player player) {
		player.showDialog(applicationDialog);
	}
	
	private void registerCommands() {
		commandExecutor = new StaffApplicationCommandExecutor(plugin, this);
		plugin.getCommand("apply").setExecutor(commandExecutor);
	}
	
	public void setApplyingForStaff(UUID uuid, boolean applying) {
		if (applying) {
			applyingForStaff.add(uuid);
		}else {
			applyingForStaff.remove(uuid);
		}
	}
	
	public boolean isApplyingForStaff(Player player) {
		return applyingForStaff.contains(player.getUniqueId());
	}
	
	public boolean hasApplied(Player player) {
		return hasApplied(player.getUniqueId());
	}
	
	public boolean hasApplied(UUID uuid) {
		return applications.containsKey(uuid);
	}
	
	public StaffApplication getApplication(String name) {
		for (StaffApplication app : applications.values()) {
			if (app.getLastKnownName().equalsIgnoreCase(name)) {
				return app;
			}
		}
		return null;
	}
	
	public Collection<StaffApplication> getApplications() {
		return applications.values();
	}
	
	public int getUnreadApplicationCount() {
		int i = 0;
		for (StaffApplication app : applications.values()) {
			if (!app.isRead()) i++;
		}
		return i;
	}
	
	public boolean deleteApplication(String username) {
		Iterator<Map.Entry<UUID,StaffApplication>> it = applications.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID,StaffApplication> pair = it.next();
			if (pair.getValue().getLastKnownName().equalsIgnoreCase(username)) {
				it.remove();
				return true;
			}
		}
		return false;
	}
	
	public boolean deleteApplication(UUID uuid) {
		if (applications.containsKey(uuid)) {
			applications.remove(uuid);
			return true;
		}
		return false;
	}
	
	public void deleteReadApplications() {
		Iterator<Map.Entry<UUID,StaffApplication>> it = applications.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID,StaffApplication> pair = it.next();
			if (pair.getValue().isRead()) {
				it.remove();
			}
		}
	}
	
	public void clearApplications() {
		applications.clear();
	}
	
	public void addApplication(StaffApplication application) {
		applications.put(application.getUUID(), application);
	}
	
	public ItemStack getApplicationBook() {
		ItemStack i = new ItemStack(Material.WRITABLE_BOOK, 1);
		BookMeta b = (BookMeta) i.getItemMeta();
		List<String> pages = new ArrayList<String>();
		if (applicationPages.size() == 0) {
		pages.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Staff Application" + ChatColor.RESET + "\n" +
					"Email Address: \n\n" +
					"Age:");
		}else {
			pages.addAll(applicationPages);
		}
		b.setPages(pages);
		i.setItemMeta(b);
		return i;
	}
	
	@Override
	public void reload() {
		//Load from main config:
		FileConfiguration config = plugin.getConfig();
		if (config.contains("staffapplications.simpleapplications")) {
			simpleApplications = config.getBoolean("staffapplications.simpleapplications");
		}
		if (config.contains("staffapplications.messages.accepted")) {
			applicationAcceptedMessage = ChatColor.translateAlternateColorCodes('&',config.getString("staffapplications.messages.accepted"));
		}
		if (config.contains("staffapplications.messages.denied")) {
			applicationDeniedMessage = ChatColor.translateAlternateColorCodes('&',config.getString("staffapplications.messages.denied"));
		}
		if (config.contains("staffapplications.messages.inprogress")) {
			applicationInProgressMessage = ChatColor.translateAlternateColorCodes('&',config.getString("staffapplications.messages.inprogress"));
		}
		if (config.contains("staffapplications.messages.simple")) {
			applicationSimpleMessage = ChatColor.translateAlternateColorCodes('&',config.getString("staffapplications.messages.simple"));
		}
		if (config.contains("staffapplications.pages")) {
			List<String> temp = config.getStringList("staffapplications.pages");
			applicationPages.clear();
			for (String s : temp) {
				applicationPages.add(ChatColor.translateAlternateColorCodes('&',s));
			}
		}
	}
	
	private void load() {
		reload();
		//Load from data file:
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		Set<String> uuids = c.getKeys(false);
		for (String u : uuids) {
			List<Map<?, ?>> mapList = c.getMapList(u + ".fields");
			Map<String, String> fields = (Map<String, String>) mapList.get(0);
			boolean read = c.getBoolean(u + ".read");
			String name = c.getString(u + ".username");
			applications.put(UUID.fromString(u), new StaffApplication(UUID.fromString(u),name,fields,read));
		}
	}
	
	@Override
	public void save() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = new YamlConfiguration();
		for (UUID uuid : applications.keySet()) {
			String u = uuid.toString();
			StaffApplication app = applications.get(uuid);
			c.set(u + ".fields", List.of(app.getFields()));
			c.set(u + ".read", app.isRead());
			c.set(u + ".username", app.getLastKnownName());
		}
		try {
			c.save(file);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
		}
	}

}
