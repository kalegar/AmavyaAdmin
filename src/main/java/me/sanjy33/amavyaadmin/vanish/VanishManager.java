package me.sanjy33.amavyaadmin.vanish;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

import me.sanjy33.amavyaadmin.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;

public class VanishManager extends SystemManager{
	
	private final AmavyaAdmin plugin;
	private final String fileName = "vanish.yml";

	private final String[] commands = {"vanish","silentjoin","silentquit","fakejoin","fakequit"};

	private final Set<Player> invisiblePlayers = new HashSet<Player>();
	private final Set<UUID> silentJoinPlayers = new HashSet<UUID>();
	private final Set<UUID> silentQuitPlayers = new HashSet<UUID>();
	
	public VanishManager(AmavyaAdmin plugin) {
		super();
		this.plugin = plugin;
		registerCommands();
		load();
	}
	
	private void registerCommands() {
		VanishCommandExecutor commandExecutor = new VanishCommandExecutor(this);
		Utils.registerAndSetupCommands(plugin,commands, commandExecutor,plugin.permissionTabCompleter);
	}
	
	public void toggleInvisibility(Player player) {
		if (invisiblePlayers.contains(player)) {
			invisiblePlayers.remove(player);
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.equals(player)) continue;
				p.showPlayer(plugin, player);
			}
		}else {
			for (Player p :Bukkit.getOnlinePlayers()) {
				if (p.equals(player)) continue;
				if (!p.hasPermission("aadmin.vanish.seevanished")) {
					p.hidePlayer(plugin, player);
				}
			}
			invisiblePlayers.add(player);
		}
	}
	
	public boolean isPlayerInvisible(Player player) {
		return invisiblePlayers.contains(player);
	}
	
	public Set<Player> getInvisiblePlayers(){
		return invisiblePlayers;
	}
	
	public void enableSilentJoin(UUID uuid) {
		silentJoinPlayers.add(uuid);
	}
	
	public void disableSilentJoin(UUID uuid) {
		silentJoinPlayers.remove(uuid);
	}
	
	public boolean getSilentJoin(UUID uuid) {
		return silentJoinPlayers.contains(uuid);
	}
	
	public void enableSilentQuit(UUID uuid) {
		silentQuitPlayers.add(uuid);
	}
	
	public void disableSilentQuit(UUID uuid) {
		silentQuitPlayers.remove(uuid);
	}
	
	public boolean getSilentQuit(UUID uuid) {
		return silentQuitPlayers.contains(uuid);
	}
	
	@Override
	public void save() {
		YamlConfiguration c = new YamlConfiguration();
		List<String> stringList = new ArrayList<>();
		if (silentJoinPlayers.size() > 0) {
			silentJoinPlayers.forEach(uuid -> stringList.add(uuid.toString()));
			c.set("silentjoin", stringList);
			stringList.clear();
		}
		if (silentQuitPlayers.size() > 0) {
			silentQuitPlayers.forEach(uuid -> stringList.add(uuid.toString()));
			c.set("silentquit", stringList);
		}
		try {
			c.save(new File(plugin.getDataFolder(), fileName));
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
		}
	}
	
	private void load() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		List<String> stringList = c.getStringList("silentjoin");
		stringList.forEach(str -> silentJoinPlayers.add(UUID.fromString(str)));
		stringList = c.getStringList("silentquit");
		stringList.forEach(str -> silentQuitPlayers.add(UUID.fromString(str)));
	}
	
}
