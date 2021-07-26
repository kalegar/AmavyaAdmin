package me.sanjy33.amavyaadmin.jail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;

public class JailManager extends SystemManager{
	
	private final AmavyaAdmin plugin;
	private JailCommandExecutor commandExecutor;
	
	private List<String> blockedCommands = new ArrayList<String>();
	private BukkitTask unJailTask = null;
	private Map<Integer,JailCell> jailCells = new ConcurrentHashMap<>();
	private List<String> toBeReleased = new ArrayList<String>();
	public Long unJailCheckRate = 20*5L; //Check every five seconds
	private Map<Integer,BukkitTask> unJailTasks = new ConcurrentHashMap<>();
	
	public JailManager(AmavyaAdmin plugin) {
		super();
		this.plugin = plugin;
		registerCommands();
		load();
		startUnJailCheckTask();
	}
	
	private void registerCommands() {
		//Jail commands:
		commandExecutor = new JailCommandExecutor(plugin,this);
		plugin.getCommand("jail").setExecutor(commandExecutor);
		plugin.getCommand("jailcreate").setExecutor(commandExecutor);
		plugin.getCommand("jaildelete").setExecutor(commandExecutor);
		plugin.getCommand("jaildeleteall").setExecutor(commandExecutor);
		plugin.getCommand("unjail").setExecutor(commandExecutor);
		plugin.getCommand("unjailall").setExecutor(commandExecutor);
		plugin.getCommand("jailstatus").setExecutor(commandExecutor);
		plugin.getCommand("jailaddtime").setExecutor(commandExecutor);
		plugin.getCommand("jailsubtracttime").setExecutor(commandExecutor);
		plugin.getCommand("jaillist").setExecutor(commandExecutor);
	}
	
	public void reload() {
		FileConfiguration config = plugin.getConfig();
		blockedCommands = config.getStringList("jailBlockedCommands");
		unJailTask.cancel();
		startUnJailCheckTask();
	}
	
	public boolean isCommandBlocked(String cmd) {
		for (String blocked : blockedCommands) {
			if (blocked.equalsIgnoreCase(cmd)) {
				return true;
			}
		}
		return false;
	}
	
	public int jailCellCount() {
		return jailCells.size();
	}
	
	public JailCell createCell(Location location) {
		JailCell cell = new JailCell(jailCells.size());
		cell.setLocation(location);
		jailCells.put(cell.getID(),cell);
		save();
		return cell;
	}
	
	public boolean deleteCell(int id) {
		if (jailCells.containsKey(id)) {
			jailCells.remove(id);
			return true;
		}
		return false;
	}
	
	public void clearCells() {
		jailCells.clear();
		save();
	}
	
	public boolean addTime(UUID uuid, long time) {
		JailCell cell = getCell(uuid);
		if (cell != null) {
			cell.setTimeWhenReleased(cell.getTimeWhenReleased()+time);
			return true;
		}
		return false;
	}
	
	public JailCell getCell(UUID uuid) {
		for (JailCell cell : jailCells.values()) {
			if (!cell.isEmpty() && cell.getOccupant().equals(uuid)) {
				return cell;
			}
		}
		return null;
	}
	
	public boolean jailPlayer(UUID uuid, UUID jailer, String jailerName, String reason, long duration) {
		for (JailCell cell : jailCells.values()) {
			if (cell.isEmpty()) {
				return cell.jailUUID(uuid, jailer, jailerName, duration, reason);
			}
		}
		return false;
	}
	
	public boolean unJailPlayer(UUID uuid) {
		for (JailCell cell : jailCells.values()) {
			if (!cell.isEmpty() && cell.getOccupant().equals(uuid)) {
				cell.reset();
				return true;
			}
		}
		return false;
	}
	
	public Collection<JailCell> getJailCells() {
		return jailCells.values();
	}
	
	public boolean isPlayerInJail(UUID uuid) {
		for (JailCell cell : jailCells.values()) {
			if (!cell.isEmpty() && cell.getOccupant().equals(uuid)) {
				return true;
			}
		}
		return false;
	}
	
	public void addToBeReleased(String uuid) {
		toBeReleased.add(uuid);
	}
	
	public boolean isToBeReleased(String uuid) {
		return toBeReleased.contains(uuid);
	}
	
	public void removeToBeReleased(String uuid) {
		toBeReleased.remove(uuid);
	}
	
	public void addBlockedCommand(String cmd) {
		blockedCommands.add(cmd);
	}
	
	public void removeBlockedCommand(String cmd) {
		Iterator<String> it = blockedCommands.iterator();
		while (it.hasNext()) {
			if (it.next().equalsIgnoreCase(cmd)) {
				it.remove();
				return;
			}
		}
	}
	
	@Override
	public void save() {
		YamlConfiguration c = new YamlConfiguration();
		c.set("toBeReleased", toBeReleased);
		c.set("cells", jailCells.size());
		int i = 0;
		for (JailCell p : jailCells.values()){
			UUID u = p.getOccupant();
			String us = "null";
			if (u!=null) us = u.toString();
			c.set(i+".occupant",us);
			c.set(i+".timeLeft",p.getTimeWhenReleased());
			c.set(i+".location.x",p.getLocation().getX());
			c.set(i+".location.y",p.getLocation().getY());
			c.set(i+".location.z",p.getLocation().getZ());
			c.set(i+".location.pitch",p.getLocation().getPitch());
			c.set(i+".location.yaw",p.getLocation().getYaw());
			c.set(i+".location.world", p.getLocation().getWorld().getName());
			c.set(i+".reason",p.getReason());
			u = p.getJailer();
			us = "null";
			if (u!=null) us = u.toString();
			c.set(i+".jailer.uuid",us);
			c.set(i+".jailer.name",p.getJailerName());
		}
		try {
			c.save(new File(plugin.getDataFolder(), "jail.yml"));
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving jail.yml: " + e.getMessage());
		}
	}
	
	private void load() {
		YamlConfiguration c = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "jail.yml"));
		toBeReleased = c.getStringList("toBeReleased");
		int totalCells = c.getInt("cells");
		jailCells.clear();
		for (int i=0;i<totalCells;i++){
			JailCell p = new JailCell(i);
			String o = c.getString(i+".occupant");
			if (!o.equalsIgnoreCase("null")){
				p.setOccupant(UUID.fromString(o));
			}
			p.setTimeWhenReleased(c.getLong(i+".timeLeft"));
			Location l = new Location(Bukkit.getWorld(c.getString(i+".location.world")),c.getDouble(i+".location.x"),c.getDouble(i+".location.y"),c.getDouble(i+".location.z"),(float)c.getLong(i+".location.yaw"),(float)c.getLong(i+".location.pitch"));
			p.setLocation(l);
			p.setReason(c.getString(i+".reason"));
			o = c.getString(i+".jailer.uuid");
			if (!o.equalsIgnoreCase("null")){
				p.setJailer(UUID.fromString(o));
			}
			p.setJailerName(c.getString(i+".jailer.name"));
			jailCells.put(i,p);
		}
	}

	private void startUnJailCheckTask() {
		unJailTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				for (JailCell p : jailCells.values()){
					if (!p.isEmpty()){
						long dif = p.getTimeWhenReleased() - System.currentTimeMillis();
						//Start task to unjail player
						if (dif < (unJailCheckRate/20)) {
							startUnJailTask(p);
						}
					}
				}
			}
			
		}, 60L, unJailCheckRate);
	}
	
	private void startUnJailTask(JailCell cell) {
		if (unJailTasks.containsKey(cell.getID())) {
			unJailTasks.get(cell.getID()).cancel();
			unJailTasks.remove(cell.getID());
		}
		BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, new JailReleaseTask(cell,toBeReleased), 20L);
		unJailTasks.put(cell.getID(), task);
	}
}
