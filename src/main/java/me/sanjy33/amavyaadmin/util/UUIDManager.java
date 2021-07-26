package me.sanjy33.amavyaadmin.util;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UUIDManager extends SystemManager{
	
	private final AmavyaAdmin plugin;
	private final String fileName = "uuids.yml";
	private final String uuidSectionKey = "uuids";
	private final String aliasesSectionKey = "aliases";
	
	@NotNull
    private final Map<UUID, String> uuidToName = new ConcurrentHashMap<>();
	@NotNull
    private final Map<String, UUID> nameToUUID = new ConcurrentHashMap<>();
	@NotNull
    private final ConcurrentMap<UUID, Set<String>> knownAliases = new ConcurrentHashMap<>();
	
	public UUIDManager(AmavyaAdmin plugin) {
		super();
		this.plugin = plugin;
		load();
	}
	
	public boolean hasEntry(UUID uuid) {
		return uuidToName.containsKey(uuid);
	}
	
	public boolean hasEntry(String name) {
		for (String n : nameToUUID.keySet()) {
			if (n.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	@Deprecated
	public UUID getUUID(String name) {
		plugin.getLogger().log(Level.WARNING," Deprecated uuid lookup for " + name);
		for (String n : nameToUUID.keySet()) {
			if (n.equalsIgnoreCase(name)) {
				return nameToUUID.get(n);
			}
		}
		return loadEntryFromFile(name);
	}

	public void getUUID(final String name, final UUIDCallback callback) {
		for (String n : nameToUUID.keySet()) {
			if (n.equalsIgnoreCase(name)) {
				callback.onFinished(n,nameToUUID.get(n));
				return;
			}
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			UUID uuid = loadEntryFromFile(name);
			Bukkit.getScheduler().runTask(plugin, () -> callback.onFinished(name,uuid));
		});
	}

	@Deprecated
	public String getName(UUID uuid) {
		plugin.getLogger().log(Level.WARNING," Deprecated name lookup for " + uuid.toString());
		loadEntryFromFile(uuid);
		return uuidToName.get(uuid);
	}

	public void getName(final UUID uuid, final UUIDCallback callback) {
		if (uuidToName.containsKey(uuid)) {
			callback.onFinished(uuidToName.get(uuid),uuid);
		} else {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				String name = loadEntryFromFile(uuid);
				Bukkit.getScheduler().runTask(plugin, () -> callback.onFinished(name,uuid));
			});
		}
	}

	public void getNames(final List<UUID> uuids, final BulkUUIDCallback callback) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			final List<String> names = new ArrayList<>();
			for (UUID uuid : uuids) {
				if (uuidToName.containsKey(uuid)) {
					names.add(uuidToName.get(uuid));
				}else {
					names.add(loadEntryFromFile(uuid));
				}
			}
			Bukkit.getScheduler().runTask(plugin, () -> callback.onFinished(names,uuids));
		});
	}
	
	@Nullable
    public List<String> getKnownAliases(UUID uuid) {
		if (knownAliases.containsKey(uuid)) {
			return new ArrayList<>(knownAliases.get(uuid));
		}
		return null;
	}
	
	/**
	 * Asynchronously add/update a UUID-Name pair.
	 * @param uuid UUID to add
	 * @param name Name to add
	 */
	public void addEntry(final UUID uuid, final String name) {
		getAddEntryRunnable(uuid,name).runTaskAsynchronously(plugin);
	}

	private BukkitRunnable getAddEntryRunnable(final UUID uuid, final String name) {
		return new BukkitRunnable() {
			@Override
			public void run() {
				internalAddEntry(uuid,name,true);
			}

		};
	}

	private void internalAddEntry(final UUID uuid, final String name, boolean save) {
		String oldName = uuidToName.get(uuid);
		if (oldName != null && oldName.length() > 0) {
			nameToUUID.remove(oldName);
		}
		uuidToName.put(uuid, name);
		nameToUUID.put(name, uuid);
		if (knownAliases.containsKey(uuid)) {
			Set<String> names = knownAliases.get(uuid);
			names.add(name);
		}else {
			Set<String> names = new HashSet<>();
			names.add(name);
			knownAliases.put(uuid, names);
		}
		if (save)
			save();
	}
	
	@Override
	public void save() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		for (UUID u : uuidToName.keySet()) {
			c.set("uuids."+u.toString(), uuidToName.get(u));
		}
		for (UUID u : knownAliases.keySet()) {
			List<String> names = new ArrayList<>(knownAliases.get(u));
			c.set("aliases."+u.toString(), names);
		}
		try {
			c.save(file);
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
		}
	}

	private UUID loadEntryFromFile(String name) {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		ConfigurationSection uuidSection = c.getConfigurationSection(uuidSectionKey);
		if (uuidSection != null) {
			for (String key : uuidSection.getKeys(false)) {
				String val = uuidSection.getString(key);
				if (val != null && val.equalsIgnoreCase(name)) {
					UUID uuid = UUID.fromString(key);
					internalAddEntry(uuid,val,false);
					return uuid;
				}
			}
		}
		return null;
	}

	private String loadEntryFromFile(UUID uuid) {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		ConfigurationSection uuidSection = c.getConfigurationSection(uuidSectionKey);
		if (uuidSection != null) {
			if (uuidSection.contains(uuid.toString())) {
				String name = uuidSection.getString(uuid.toString());
				internalAddEntry(uuid, name, false);
				return name;
			}
		}
		return null;
	}
	
	private void load() {
		File file = new File(plugin.getDataFolder(), fileName);
		YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
		ConfigurationSection aliasSection = c.getConfigurationSection(aliasesSectionKey);
		if (aliasSection != null) {
			Set<String> keys = aliasSection.getKeys(false);
			for (String u : keys) {
				UUID uuid = UUID.fromString(u);
				List<String> list = aliasSection.getStringList(u);
				Set<String> names = new HashSet<>(list);
				knownAliases.put(uuid, names);
			}
		}
	}

}
