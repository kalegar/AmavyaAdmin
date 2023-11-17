package me.sanjy33.amavyaadmin.mute;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import me.sanjy33.amavyaadmin.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import me.sanjy33.amavyaadmin.util.TimeParser;

public class MuteManager extends SystemManager {
	
	private final AmavyaAdmin plugin;
	private final String fileName = "muted_players.yml";

	private final Set<MutedPlayer> mutedPlayers = ConcurrentHashMap.newKeySet();
	private List<String> mutedCommands;
	private BukkitTask unMuteTask = null;
	
	public MuteManager(AmavyaAdmin plugin) {
		super();
		this.plugin = plugin;
		registerCommands();
		load();
		startUnMuteTask();
	}

	private static final String[] commands = {"mute","unmute"};
	private void registerCommands() {
		MuteCommandExecutor commandExecutor = new MuteCommandExecutor(this, plugin.uuidManager);
		Utils.registerAndSetupCommands(plugin,commands, commandExecutor,plugin.permissionTabCompleter);
	}

	/**
	 * Mute a player.
	 * @param player The UUID of the player to be muted.
	 * @param muter The UUID of the player who muted the player. null for console
	 * @param reason The mute reason.
	 * @param time The length of the mute in ms.
	 */
	public void mute(UUID player, UUID muter, String reason, long time) {
		long unMuteTime = System.currentTimeMillis()+time;
		MutedPlayer.Muter _muter;
		if (muter == null) {
			_muter = MutedPlayer.Muter.console();
		}else{
			Player pl = Bukkit.getPlayer(player);
			String name = "";
			if (pl != null)
				name = pl.getName();
			_muter = new MutedPlayer.Muter(name,player);
		}
		MutedPlayer mp = new MutedPlayer(player,_muter,reason,unMuteTime);
		mutedPlayers.add(mp);
		Player p = Bukkit.getPlayer(player);
		if (p != null) {
			p.sendMessage(Component.text("You have been muted for " + TimeParser.parseLong(time,false) + ". Reason: " + reason, NamedTextColor.RED));
		}
	}
	
	public void unMute(UUID uuid) {
		MutedPlayer p = getMutedPlayer(uuid);
		if (p != null) {
			unMute(p);
		}
	}
	
	public void unMute(MutedPlayer mutedPlayer){
		UUID u = mutedPlayer.getPlayer();
		Player p = Bukkit.getPlayer(u);
		if (mutedPlayers.contains(mutedPlayer)){
			mutedPlayers.remove(mutedPlayer);
			if (p != null){
				p.sendMessage(Component.text("You have been unmuted!", NamedTextColor.GREEN));
			}
		}
	}
	
	public MutedPlayer getMutedPlayer(UUID u) {
		for (MutedPlayer m : mutedPlayers) {
			if (m.getPlayer().equals(u)) {
				return m;
			}
		}
		return null;
	}
	
	public boolean isCommandBanned(String command) {
		for (String s : mutedCommands) {
			if (s.equalsIgnoreCase(command)) {
				return true;
			}
		}
		return false;
	}
	
	private void startUnMuteTask(){
		unMuteTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				for (MutedPlayer mp : mutedPlayers) {
					if (System.currentTimeMillis()>=mp.getUnMuteTime()) {
						Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
							@Override
							public void run() {
								unMute(mp);
							}
						}, 20L);
						
					}
				}
			}
			
		}, 20L, 100L);
	}
	
	@Override
	public void reload() {
		mutedCommands = plugin.getConfig().getStringList("mutedcommands");
		unMuteTask.cancel();
		startUnMuteTask();
	}
	
	@Override
	public void save(){
		YamlConfiguration c = new YamlConfiguration();
		c.set("total", mutedPlayers.size());
		int i = 0;
		for (MutedPlayer mp : mutedPlayers) {
			c.set(i+".player", mp.getPlayer().toString());
			c.set(i+".muter.name", mp.getMuter().getName());
			c.set(i+".muter.uuid", mp.getMuter().getUuid().toString());
			c.set(i+".unMuteTime", mp.getUnMuteTime());
			c.set(i+".reason", mp.getReason());
			i++;
		}
		try {
			c.save(new File(plugin.getDataFolder(),fileName));
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName +": " + e.getMessage());
		}
	}
	
	private void load() {
		YamlConfiguration c = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
		int total = c.getInt("total");
		mutedPlayers.clear();
		for (int i=0;i<total;i++) {
			String player = c.getString(i+".player");
			if (player == null) continue;

			MutedPlayer.Muter muter;
			String uuid = c.getString(i+".muter.uuid");
			if (uuid == null || uuid.length() == 0) {
				muter = MutedPlayer.Muter.console();
			}else{
				muter = new MutedPlayer.Muter(c.getString(i+".muter.name"),UUID.fromString(uuid));
			}

			MutedPlayer mp = new MutedPlayer(
					UUID.fromString(player),
					muter,
					c.getString(i+".reason"),
					c.getLong(i+"unMuteTime"));
			mutedPlayers.add(mp);
		}
	}

}
