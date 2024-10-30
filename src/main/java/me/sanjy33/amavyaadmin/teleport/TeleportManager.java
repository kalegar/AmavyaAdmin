package me.sanjy33.amavyaadmin.teleport;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import me.sanjy33.amavyaadmin.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;

public class TeleportManager extends SystemManager {
	
	private final AmavyaAdmin plugin;
	private final String fileName = "locations.yml";

	private final List<TeleportRequest> teleportRequests = new ArrayList<>();
	private final Map<Player, Location> deathLocations = new HashMap<>();
	private final Map<Player, BukkitTask> teleportTasks = new HashMap<>();
	private final Map<Player, Location> lastLocations = new HashMap<>();
	private final Map<String, Location> warps = new HashMap<>();
	private Long teleportWarmup = 60L;
	private int teleportRequestTimeoutTicks = 1200; //60 seconds
	private Long teleportRequestWarmupTicks = 60L;
	private final Map<UUID, Location> worldSpawnLocations = new HashMap<>();

	private Location survivalLocation = null;
	private Location creativeLocation = null;
	private Location pvpLocation = null;
	private Location hubLocation = null;

	public String messageButtonAccept = "";
	public String messageButtonDeny = "";
	
	public TeleportManager(AmavyaAdmin plugin) {
		this.plugin = plugin;
		registerCommands();
		load();
	}
	
	public void teleport(Player player, Location location, TeleportCallback callback) {
		teleport(player,location,callback,teleportWarmup);
	}
	
	public void teleport(Player player, Location location, TeleportCallback callback, Long warmup) {
		UUID uuid = player.getUniqueId();
		//Store current location
		lastLocations.put(player, player.getLocation());
		if (teleportTasks.containsKey(player)) {
			teleportTasks.get(player).cancel();
		}
		BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(uuid);
				if (player==null) return;
				if (lastLocations.containsKey(player)){
					Location previousLocation = lastLocations.get(player);
					Location currentLocation = player.getLocation();
					if (currentLocation.distance(previousLocation)>0.5){
						callback.onTeleport(false, player, previousLocation, currentLocation,"Teleport cancelled due to movement.");
						plugin.particleLibHook.addSpiralEffect(player, Particle.ANGRY_VILLAGER,20,20,0.75);
						return;
					}
					lastLocations.remove(player);
				}
				Location currentLocation = player.getLocation();
				player.teleport(location);
				callback.onTeleport(true, player, currentLocation, location, "Teleport Successful.");
				plugin.particleLibHook.addBurstEffect(player, Particle.HAPPY_VILLAGER,20,4,0.75, 12);
				teleportTasks.remove(player);
			}
		}, warmup);
		teleportTasks.put(player, task);
		plugin.particleLibHook.addDoubleSpiralEffect(player, Particle.HAPPY_VILLAGER,warmup,warmup,0.75);
	}
	
	public boolean isDeathLocationStored(Player player) {
		return deathLocations.containsKey(player);
	}
	
	public Location getDeathLocation(Player player) {
		if (deathLocations.containsKey(player)) {
			return deathLocations.get(player);
		}
		return null;
	}
	
	public void setDeathLocation(Player player, Location location) {
		deathLocations.put(player, location);
	}
	
	public void removeDeathLocation(Player player) {
		deathLocations.remove(player);
	}

	public boolean deleteTeleportRequest(String requestId) {
		for (int i = 0; i < teleportRequests.size(); i++) {
			if (teleportRequests.get(i).getId().equals(requestId)) {
				teleportRequests.remove(i);
				return true;
			}
		}
		return false;
	}
	
	public TeleportRequest createTeleportRequest(Player target, Player sender) {
		TeleportRequest request = new TeleportRequest(plugin,target,sender,teleportRequestTimeoutTicks,(req) -> {
			for (int i = 0; i < teleportRequests.size(); i++) {
				if (teleportRequests.get(i).getId().equals(req.getId())) {
					if (req.getFrom() != null && req.getFrom().isOnline()) {
						req.getFrom().sendMessage(Component.text("Your teleport request to " + req.getTo().getName() + " expired.", NamedTextColor.RED));
					}
					teleportRequests.remove(i);
					return;
				}
			}
		});
		teleportRequests.add(request);
		return request;
	}

	public List<TeleportRequest> getTeleportRequestsTargetingPlayer(Player target) {
		List<TeleportRequest> result = new ArrayList<>();
		for (TeleportRequest request : teleportRequests) {
			if (request.getTo().equals(target)) {
				result.add(request);
			}
		}
		return result;
	}

	public List<TeleportRequest> getSentTeleportRequests(Player sender) {
		List<TeleportRequest> result = new ArrayList<>();
		for (TeleportRequest request : teleportRequests) {
			if (request.getFrom().equals(sender)) {
				result.add(request);
			}
		}
		return result;
	}

	public Location getWorldSpawnLocation(UUID worldUUID) {
		if (worldSpawnLocations.containsKey(worldUUID)) {
			return worldSpawnLocations.get(worldUUID);
		}
		World world = Bukkit.getWorld(worldUUID);
		if (world != null) {
			return world.getSpawnLocation();
		}
		return null;
	}
	
	public Location getWorldSpawnLocation(String worldName) {
		World world = Bukkit.getWorld(worldName);
		if (world != null) {
			return getWorldSpawnLocation(world.getUID());
		}
		return null;
	}
	
	public boolean setWorldSpawnLocation(Location location) {
		World world = location.getWorld();
		if (world != null) {
			worldSpawnLocations.put(world.getUID(),location);
			return true;
		}
		return false;
	}
	
	public Long getTeleportWarmup() {
		return teleportWarmup;
	}
	
	public boolean warpExists(String name) {
		return warps.containsKey(name.toLowerCase());
	}
	
	public void createWarp(String name, Location location) {
		warps.put(name.toLowerCase(),location.clone());
	}
	
	public void deleteWarp(String name) {
		String lower = name.toLowerCase();
		warps.remove(lower);
	}
	
	public Location getWarpLocation(String name) {
		String lower = name.toLowerCase();
		if (warps.containsKey(lower)) {
			return warps.get(lower);
		}
		return null;
	}

	public void setWorldWarp(WorldWarp warp, Location location) {
		switch (warp) {
			case PVP: {
				pvpLocation = location.clone();
				break;
			}
			case CREATIVE: {
				creativeLocation = location.clone();
				break;
			}
			case SURVIVAL: {
				survivalLocation = location.clone();
				break;
			}
		}
	}

	public Location getWorldWarp(WorldWarp warp) {
		switch (warp) {
			case PVP: return pvpLocation;
			case CREATIVE: return creativeLocation;
			case SURVIVAL: return survivalLocation;
			case HUB: return hubLocation;
			default: return null;
		}
	}

	private static final String[] commands = {"back","tp","tpa","tpaccept","tpdeny","spawn","setspawnwarp","warp","createwarp","deletewarp"};
	private void registerCommands() {
		TeleportCommandExecutor commandExecutor = new TeleportCommandExecutor(this);
		Utils.registerAndSetupCommands(plugin,commands, commandExecutor,plugin.permissionTabCompleter);
		//World TP Commands:
		for (WorldWarp warp : WorldWarp.values()) {
			PluginCommand command = plugin.getCommand(warp.getCommand());
			if (command != null) {
				command.setExecutor(commandExecutor);
				command.setTabCompleter(plugin.permissionTabCompleter);
			}
			command = plugin.getCommand(warp.getSetCommand());
			if (command != null) {
				command.setExecutor(commandExecutor);
				command.setTabCompleter(plugin.permissionTabCompleter);
			}
		}
	}
	
	@Override
	public void reload() {
		FileConfiguration config = plugin.getConfig();
		teleportWarmup = (config.getInt("warpwarmup")* 20L);
		teleportRequestWarmupTicks = (config.getInt("tpa.warmupSeconds",3) * 20L);
		teleportRequestTimeoutTicks = (config.getInt("tpa.timeoutSeconds",60) * 20);
		if (config.contains("tpa.messages.buttons.accept")) {
			messageButtonAccept = config.getString("tpa.messages.buttons.accept","[Accept]");
		}
		if (config.contains("tpa.messages.buttons.deny")) {
			messageButtonDeny = config.getString("tpa.messages.buttons.deny","[Deny]");
		}
	}
	
	@Override
	public void save() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = new YamlConfiguration();

		if (worldSpawnLocations.size() > 0) {
			ConfigurationSection worldSpawnSection = c.createSection("worldSpawns");
			worldSpawnLocations.forEach((k,v) -> {
				worldSpawnSection.set(k.toString(),v);
			});
		}

		//Save warps
		if (warps.size() > 0) {
			ConfigurationSection warpSection = c.createSection("warps");
			warps.forEach(warpSection::set);
		}

		c.set("locations.survival",survivalLocation);
		c.set("locations.creative",creativeLocation);
		c.set("locations.pvp",pvpLocation);
		c.set("locations.hub",hubLocation);

		try {
			c.save(file);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
		}
	}
	
	private void load() {
		reload();
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

		ConfigurationSection worldSpawnSection = c.getConfigurationSection("worldSpawns");
		if (worldSpawnSection != null) {
			worldSpawnLocations.clear();
			for (String key : worldSpawnSection.getKeys(false)) {
				worldSpawnLocations.put(UUID.fromString(key),worldSpawnSection.getLocation(key));
			}
		}

		ConfigurationSection warpSection = c.getConfigurationSection("warps");
		if (warpSection != null) {
			warps.clear();
			for (String key : warpSection.getKeys(false)) {
				warps.put(key,warpSection.getLocation(key));
			}
		}

		survivalLocation = c.getLocation("locations.survival");
		creativeLocation = c.getLocation("locations.creative");
		pvpLocation      = c.getLocation("locations.pvp");
		hubLocation      = c.getLocation("locations.hub");
	}

	public enum WorldWarp {
		SURVIVAL("Survival"),
		CREATIVE("Creative"),
		PVP("PvP"),
		HUB("Hub");

		private final String displayName;

		WorldWarp(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getPermission() {
			return "aadmin."+this.name().toLowerCase();
		}

		public String getCommand() {
			return this.name().toLowerCase();
		}

		public String getSetCommand() {
			return "set"+this.name().toLowerCase()+"warp";
		}

	}

	public Long getTeleportRequestWarmupTicks() {
		return teleportRequestWarmupTicks;
	}
}
