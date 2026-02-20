package me.sanjy33.amavyaadmin.periodicmessage;

import java.util.*;

import me.sanjy33.amavyaadmin.util.TimeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import org.bukkit.scheduler.BukkitTask;

public class PeriodicMessage extends BukkitRunnable{
	private final List<Component> messages = new ArrayList<>();
	private final AmavyaAdmin plugin;
	private long frequency;
	private String permission;
	private String name;
	private final PeriodicMessageManager manager;
	private Mode mode;
	private int index = 0;
	private BukkitTask task = null;

	public PeriodicMessage(AmavyaAdmin plugin, PeriodicMessageManager manager, String name) {
		this(plugin, manager, name, List.of(), TimeParser.parseString("5m"),name,0);
	}

	public PeriodicMessage(AmavyaAdmin plugin, PeriodicMessageManager manager, String name, List<Component> messages, long frequency, String permission, int mode) {
		this.manager = manager;
		this.name = name;
		this.messages.addAll(messages);
		this.frequency = frequency;
		setPermission(permission);
		this.mode = Mode.fromInteger(mode);
		this.plugin = plugin;
	}

	public PeriodicMessage(AmavyaAdmin plugin, PeriodicMessageManager manager, ConfigurationSection section) {
		this.plugin = plugin;
		this.manager = manager;
		this.name = section.getName();
		List<String> jsonStrings = section.getStringList("messages");
		for (String json : jsonStrings) {
			try {
				messages.add(JSONComponentSerializer.json().deserialize(json));
			} catch (Exception e) {
				messages.add(Component.text(json));
			}
		}
		this.frequency = TimeParser.parseString(section.getString("frequency","5m"));
		this.permission = section.getString("permission",null);
		this.mode = Mode.fromInteger(section.getInt("mode",0));
	}

	public void start() {
		if (task != null) {
			task.cancel();
		}
		if (frequency <= 0) {
			return;
		}
		task = runTaskTimer(plugin,1L,frequency/1000*20);
	}

	public void cancelTask() {
		if (task != null)
			task.cancel();
	}

	public boolean isStarted() {
		return task != null;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public void setPermission(String permission) {
		if (this.permission != null) {
			unRegisterPermission();
		}
		this.permission = permission;
		if (this.permission != null) {
			this.permission = "aadmin.pm.view." + permission;
			registerPermission();
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMode(int mode) {
		this.mode = Mode.fromInteger(mode);
	}

	public void toConfigSection(ConfigurationSection root) {
		ConfigurationSection section = root.createSection(name);
		List<String> jsonStrings = new ArrayList<>();
		for (Component message : messages) {
			jsonStrings.add(JSONComponentSerializer.json().serialize(message));
		}
		section.set("messages",jsonStrings);
		section.set("frequency",TimeParser.parseLong(frequency,true));
		if (permission != null)
			section.set("permission",permission);
		section.set("mode",mode.index);
	}
	
	public String getPermission() {
		return permission;
	}
	
	public void registerPermission() {
		if (permission==null) return;
		Bukkit.getPluginManager().addPermission(new Permission(permission,PermissionDefault.FALSE));
	}
	
	public void unRegisterPermission() {
		if (permission==null) return;
		Bukkit.getPluginManager().removePermission(permission);
	}
	
	public String getName() {
		return name;
	}
	
	public List<Component> getMessages() {
		return messages;
	}

	public boolean deleteMessage(int index) {
		if (index < 0 || index >= messages.size()) return false;
		messages.remove(index);
		return true;
	}
	
	public void addMessage(Component message) {
		this.messages.add(message);
	}
	
	public long getFrequency() {
		return frequency;
	}

	@Override
	public void run() {
		if (messages.isEmpty()) return;
		if (mode == Mode.SEQUENCE) {
			int newIndex = (int) Math.floor(Math.random()*messages.size());
			if (newIndex == index) {
				newIndex ++;
			}
			index = newIndex;
			if (index >= messages.size()) {
				index = 0;
			}
		}
		Component message = messages.get(index);
		if (mode == Mode.RANDOM) {
			index += 1;
			if (index >= messages.size()) {
				index = 0;
			}
		}
		if (message==null){
			plugin.getLogger().warning("[AmavyaAdmin] Error getting random message for timed message group " + name);
			return;
		}
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (permission == null || p.hasPermission(permission)) {
				if (!manager.areMessagesDisabled(p.getUniqueId())) {
					p.sendMessage(message);
				}
			}
		}
	}

	public enum Mode {
		RANDOM(0),
		SEQUENCE(1);

		final int index;

		Mode(int index) {
			this.index = index;
		}

		static Mode fromInteger(int index) {
			for (Mode mode : Mode.values()) {
				if (mode.index == index)
					return mode;
			}
			return null;
		}
	}
}
