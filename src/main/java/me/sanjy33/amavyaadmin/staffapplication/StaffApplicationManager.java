package me.sanjy33.amavyaadmin.staffapplication;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

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
	
	private Map<UUID,StaffApplication> applications = new HashMap<UUID,StaffApplication>();
	private Set<UUID> applyingForStaff = new HashSet<UUID>();
	private List<String> applicationPages = new ArrayList<String>();
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
	}
	
	private void registerCommands() {
		commandExecutor = new StaffApplicationCommandExecutor(plugin, this);
		plugin.getCommand("apply").setExecutor(commandExecutor);
	}
	
	public void setApplyingForStaff(UUID uuid, boolean applying) {
		if (applying) {
			if (!applyingForStaff.contains(uuid)) {
				applyingForStaff.add(uuid);
			}
		}else {
			if (applyingForStaff.contains(uuid)) {
				applyingForStaff.remove(uuid);
			}
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
			List<String> pages = c.getStringList(u + ".pages");
			boolean read = c.getBoolean(u + ".read");
			String name = c.getString(u + ".username");
			applications.put(UUID.fromString(u), new StaffApplication(UUID.fromString(u),name,pages,read));
		}
	}
	
	@Override
	public void save() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = new YamlConfiguration();
		for (UUID uuid : applications.keySet()) {
			String u = uuid.toString();
			StaffApplication app = applications.get(uuid);
			c.set(u + ".pages", app.getApplicationPages());
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
