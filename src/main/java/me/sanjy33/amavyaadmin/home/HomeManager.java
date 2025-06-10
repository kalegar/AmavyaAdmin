package me.sanjy33.amavyaadmin.home;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import me.sanjy33.amavyaadmin.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;

public class HomeManager extends SystemManager {
	
	private final AmavyaAdmin plugin;
	private final String fileName = "homes.yml";

	private final Map<UUID, PlayerHome> playerHomes = new HashMap<UUID, PlayerHome>();
	private Long homeWarpWarmup = 60L;
	private int maxPlayerHomes = 1;
	
	public HomeManager(AmavyaAdmin plugin) {
		super();
		this.plugin = plugin;
		registerCommands();
		load();
	}

	private static final String[] commands = {
			"home",
			"sethome",
			"deletehome",
			"listhome",
			"otherhome"
	};
	private void registerCommands() {
		//Home commands:
		HomeCommandExecutor commandExecutor = new HomeCommandExecutor(plugin, this);
		Utils.registerAndSetupCommands(plugin,commands, commandExecutor,plugin.permissionTabCompleter);
	}
	
	@Override
	public void reload() {
		homeWarpWarmup = plugin.getConfig().getLong("homes.warpwarmup")*20;
		maxPlayerHomes = plugin.getConfig().getInt("homes.max");
	}
	
	public Long getHomeWarpWarmup() {
		return homeWarpWarmup;
	}
	
	public int getMaxPlayerHomes() {
		return maxPlayerHomes;
	}
	
	public boolean homeExists(UUID uuid) {
		return playerHomes.containsKey(uuid);
	}
	
	public PlayerHome getHome(UUID uuid) {
		if (playerHomes.containsKey(uuid)) {
			return playerHomes.get(uuid);
		}
		return null;
	}
	
	public void setHome(PlayerHome home) {
		playerHomes.put(home.getOwner(), home);
	}
	
	public void clearHome(UUID uuid) {
		playerHomes.remove(uuid);
	}
	
	public void updateHomeName(UUID uuid, String name) {
		if (playerHomes.containsKey(uuid)) {
			PlayerHome home = playerHomes.get(uuid);
			home.setName(name);
		}
	}
	
	public String getHomeList() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Homes: ");
		for (PlayerHome home : playerHomes.values()){
			stringBuilder.append(home.getName()).append(", ");
		}
		stringBuilder.delete(stringBuilder.length()-2, stringBuilder.length());
		return stringBuilder.toString();
	}
	
	@Override
	public void save() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = new YamlConfiguration();
		for (UUID u : playerHomes.keySet()) {
			String uuid = u.toString();
			PlayerHome home = playerHomes.get(u);
			Location l = home.getLocation();
			c.set("homes."+uuid+".name", home.getName());
			c.set("homes."+uuid+".x", l.getX());
			c.set("homes."+uuid+".y", l.getY());
			c.set("homes."+uuid+".z", l.getZ());
			c.set("homes."+uuid+".world", l.getWorld().getName());
			c.set("homes."+uuid+".pitch", l.getPitch());
			c.set("homes."+uuid+".yaw", l.getYaw());
		}
		try {
			c.save(file);
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
		}
	}
	
	private void load() {
		reload();
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		ConfigurationSection homeSection = (ConfigurationSection) c.getConfigurationSection("homes");
		if (homeSection != null) {
			Set<String> uuids = homeSection.getKeys(false);
			for (String u : uuids) {
				String name = homeSection.getString(u+".name");
				Location loc = new Location(
						Bukkit.getWorld(homeSection.getString(u+".world")),
						homeSection.getDouble(u+".x"),
						homeSection.getDouble(u+".y"),
						homeSection.getDouble(u+".z"),
						(float) homeSection.getDouble(u+".yaw"),
						(float) homeSection.getDouble(u+".pitch")
						);
				PlayerHome home = new PlayerHome(UUID.fromString(u),loc,name);
				playerHomes.put(home.getOwner(), home);
			}
		}
	}
	
	

}
