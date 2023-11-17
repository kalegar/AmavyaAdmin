package me.sanjy33.amavyaadmin.message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;

public class MessageManager extends SystemManager{
	
	private final Map<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
	private final String fileName = "queued_messages.yml";
	private final AmavyaAdmin plugin;
	
	public MessageManager(AmavyaAdmin plugin) {
		this.plugin = plugin;
		load();
	}
	
	public void sendMessage(UUID recipient, String message) {
		Player player = Bukkit.getPlayer(recipient);
		if (player != null && player.isOnline()) {
			player.sendMessage(message);
		}else {
			queueMessage(recipient, message);
		}
	}
	
	public void queueMessage(UUID recipient, String message) {
		List<String> messageList;
		if (messages.containsKey(recipient)) {
			messageList = messages.get(recipient);
		}else {
			messageList = new ArrayList<String>();
		}
		messageList.add(message);
	}
	
	public void sendMessages(Player player) {
		if (!player.isOnline()) return;
		UUID recipient = player.getUniqueId();
		if (messages.containsKey(recipient)) {
			List<String> messageList = messages.get(recipient);
			for (String message : messageList) {
				player.sendMessage(message);
			}
			messageList.clear();
		}
	}
	
	@Override
	public void save() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = new YamlConfiguration();
		for (UUID uuid : messages.keySet()) {
			String u = uuid.toString();
			c.set(u, messages.get(uuid));
		}
		try {
			c.save(file);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving queued messages: " + e.getMessage());
		}
	}
	
	private void load() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		Set<String> uuids = c.getKeys(false);
		for (String u : uuids) {
			UUID uuid = UUID.fromString(u);
			List<String> messageList = c.getStringList(u);
			messages.put(uuid, messageList);
		}
	}

}
