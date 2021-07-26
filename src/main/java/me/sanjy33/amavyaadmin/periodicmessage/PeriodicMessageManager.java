package me.sanjy33.amavyaadmin.periodicmessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import me.sanjy33.amavyaadmin.util.TimeParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PeriodicMessageManager extends SystemManager{

	private final String fileName = "periodic_messages.yml";
	private FileConfiguration config;
	private final List<PeriodicMessage> messages = new ArrayList<>();
	private final Set<UUID> playerTimedMessagesDisabled = new HashSet<>();
	private final AmavyaAdmin plugin;
	
	public PeriodicMessageManager(AmavyaAdmin plugin) {
		super();
		this.plugin = plugin;
		load();
	}
	
	public boolean areMessagesDisabled(UUID uuid) {
		return playerTimedMessagesDisabled.contains(uuid);
	}
	
	public boolean toggleMessagesDisabled(UUID uuid) {
		if (playerTimedMessagesDisabled.contains(uuid)) {
			playerTimedMessagesDisabled.remove(uuid);
			return true;
		}else {
			playerTimedMessagesDisabled.add(uuid);
			return false;
		}
	}

	private void load() {
		File file = new File(plugin.getDataFolder(), fileName);
		config = YamlConfiguration.loadConfiguration(file);

		List<String> list = config.getStringList("disabled");
		list.forEach(u -> playerTimedMessagesDisabled.add(UUID.fromString(u)));

	}
	
	@Override
	public void reload() {
		for (PeriodicMessage tm : messages){
			if (!tm.isCancelled())
				tm.cancel();
			tm.unRegisterPermission();
		}
		messages.clear();
		File file = new File(plugin.getDataFolder(), fileName);
		config = YamlConfiguration.loadConfiguration(file);

		InputStream input = plugin.getResource(fileName);
		//Look for defaults in jar
		if (input != null) {
			Reader defConfigStream = new InputStreamReader(input, StandardCharsets.UTF_8);
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			if (!file.exists()) {
				try {
					defConfig.save(file);
				} catch (IOException e) {
					Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
				}
				config = defConfig;
			}
		}

		ConfigurationSection messageSection = config.getConfigurationSection("periodicmessages");
		if (messageSection != null) {
			for (String key : messageSection.getKeys(false)) {
    			PeriodicMessage tm = new PeriodicMessage(
    					plugin,
    					this,
    					key,
    					messageSection.getStringList(key+".messages"),
						TimeParser.parseString(messageSection.getString(key+".frequency","5m"))/1000*20,
						messageSection.getString(key+".permission",null),
						messageSection.getInt(key+".mode",0)
    					);
    			tm.registerPermission();
    			messages.add(tm);
    			plugin.getLogger().info("Started Periodic Message Task '" + key + "'");
    		}
        }
	}

	@Override
	public void save() {
		if (config == null) return;
		config.set("disabled", playerTimedMessagesDisabled.stream().map(UUID::toString).collect(Collectors.toList()));
		try {
			File file = new File(plugin.getDataFolder(), fileName);
			config.save(file);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
		}
	}
}
