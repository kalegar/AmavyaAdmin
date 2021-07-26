package me.sanjy33.amavyaadmin;

import java.util.*;
import java.util.logging.Level;

import me.sanjy33.amavyaadmin.inventory.Inventory;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import me.sanjy33.amavyaadmin.home.PlayerHome;
import me.sanjy33.amavyaadmin.jail.JailCell;
import me.sanjy33.amavyaadmin.mute.MutedPlayer;
import me.sanjy33.amavyaadmin.util.TimeParser;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class AAListener implements Listener{
	
	AmavyaAdmin plugin;
	
	AAListener(AmavyaAdmin inst){
		plugin = inst;
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    	String[] split = event.getMessage().split(" ");
    	if (split.length < 1) return;
    	String cmd = split[0].trim().substring(1).toLowerCase();
    	Player p = event.getPlayer();
    	UUID u = p.getUniqueId();
    	Set<CommandSender> spies = plugin.spyManager.getSpies(u);
    	for (CommandSender sender : spies) {
    	    sender.sendMessage(ChatColor.AQUA + "[SPY] " + ChatColor.GRAY + p.getName() + " used command:");
    	    sender.sendMessage(ChatColor.GRAY + event.getMessage());
        }
    	MutedPlayer mp = plugin.muteManager.getMutedPlayer(u);
    	if (mp != null) {
    		if (plugin.muteManager.isCommandBanned(cmd)) {
    			long time = mp.getUnMuteTime() - System.currentTimeMillis();
    			if (time <= 0) {
    				plugin.muteManager.unMute(mp);
    			}else {
    				String msg = plugin.mutedMessage.replaceAll("%time%",TimeParser.parseLong(time, false)).replaceAll("%reason%",mp.getReason());
    				p.sendMessage(msg);
    				event.setCancelled(true);
    				return;
    			}
    		}
    	}
    	if (plugin.jailManager.isPlayerInJail(p.getUniqueId())) {
    		if (plugin.jailManager.isCommandBlocked(cmd)) {
    			p.sendMessage(ChatColor.RED + "You can't use that command in jail!");
				event.setCancelled(true);
    		}
    	}
    	
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent event){
		MutedPlayer mp = plugin.muteManager.getMutedPlayer(event.getPlayer().getUniqueId());
		if (mp != null){
			long time = mp.getUnMuteTime() - System.currentTimeMillis();
			if (time <= 0) {
				plugin.muteManager.unMute(mp);
				return;
			}
			String msg = plugin.mutedMessage.replaceAll("%time%",TimeParser.parseLong(time, false)).replaceAll("%reason%",mp.getReason());
			event.getPlayer().sendMessage(msg);
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onSignChange(SignChangeEvent event){
		Player p = event.getPlayer();
		String[] lines = event.getLines();
		if (lines[0].equalsIgnoreCase("[BuyRegion]")){
			if (!p.hasPermission("aadmin.sign.create")){
				p.sendMessage(ChatColor.RED + "You don't have permission!");
				event.setCancelled(true);
				return;
			}
		}
		if (lines[0].equalsIgnoreCase("[Sold!]")){
			if (!p.hasPermission("aadmin.sign.create")){
				p.sendMessage(ChatColor.RED + "You don't have permission!");
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onBedLeave(PlayerBedLeaveEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("aadmin.home.bed")) {
			if (!plugin.homeManager.homeExists(player.getUniqueId())) {
				plugin.homeManager.setHome(new PlayerHome(player.getUniqueId(),player.getLocation(),player.getName()));
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(ChatColor.GREEN + "" + ChatColor.ITALIC + "Your home has been set to your bed location!"));
			}
		}
		//If anyone gets out of a bed clear all sleep ignored players
        plugin.sleepManager.clearSleepingIgnored();
        plugin.sleepManager.checkSleepDelayed(event.getPlayer().getWorld());
	}
	
	@EventHandler
	public void onBedEnter(PlayerBedEnterEvent event) {
		plugin.sleepManager.checkSleepDelayed(event.getPlayer().getWorld());
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		plugin.spyManager.removeAgent(p);
		if (plugin.vanishManager.getSilentQuit(p.getUniqueId())) {
			event.setQuitMessage(null);
			for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
				if (pl.getUniqueId().equals(p.getUniqueId())) continue;
				if (pl.hasPermission("aadmin.vanish.silentnotify")) {
					pl.sendMessage(ChatColor.GRAY + "~" + p.getName() + " left the game silently~");
				}
			}
		}
		if (plugin.vanishManager.getInvisiblePlayers().contains(p)) {
			plugin.vanishManager.toggleInvisibility(p);
		}
		//Someone quit, so there is 1 less player in the world, start a sleep check
		plugin.sleepManager.checkSleepDelayed(event.getPlayer().getWorld());
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player p = event.getPlayer();
		String message = plugin.lockdownMessage;
		if (plugin.lockdown){
			if (!p.hasPermission("aadmin.lockdown.bypass")){
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER,message);
				return;
			}
		}
		event.allow();
	}
	
//////Update to UUID when bukkit supports it!!!
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		final Player p = event.getPlayer();
		if (plugin.lockdown){
			p.sendMessage(ChatColor.RED + "<<< The server is currently locked down! >>>");
		}
		if (plugin.vanishManager.getSilentJoin(p.getUniqueId())) {
			p.sendMessage(ChatColor.GRAY + "~You joined silently~");
			event.setJoinMessage(null);
			if (!plugin.vanishManager.isPlayerInvisible(p)) {
				plugin.vanishManager.toggleInvisibility(p);
			}
			for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
				if (pl.getUniqueId().equals(p.getUniqueId())) continue;
				if (pl.hasPermission("aadmin.vanish.silentnotify")) {
					pl.sendMessage(ChatColor.GRAY + "~" + p.getName() + " joined the game silently~");
				}
			}
		}
		if (!p.hasPermission("aadmin.vanish.seevanished")) {
			for (Player invisible : plugin.vanishManager.getInvisiblePlayers()) {
				p.hidePlayer(plugin, invisible);
			}
		}
		plugin.messageOfTheDay.sendMotd(p);
		
		plugin.prevWorld.put(p, p.getWorld());
		
		if (plugin.jailManager.isToBeReleased(p.getUniqueId().toString())){
			p.sendMessage(ChatColor.GREEN+"You have been released from jail.");
			p.teleport(p.getLocation().getWorld().getSpawnLocation());
			plugin.jailManager.removeToBeReleased(p.getUniqueId().toString());
		}
		
		JailCell cell = plugin.jailManager.getCell(p.getUniqueId());
		if (cell != null) {
			p.sendMessage(ChatColor.RED+"You have been jailed! Use /jailstatus");
			p.teleport(cell.getLocation());
		}
		
		if (p.hasPermission("aadmin.staffapps.check")){
			int amm = plugin.staffApplicationManager.getUnreadApplicationCount();
			if (amm>0){
				p.sendMessage(ChatColor.AQUA + "[App] There are " + amm + " new staff applications! Use /apply check");
			}
		}
		
		plugin.messageManager.sendMessages(p);
		if (plugin.starterKit != null && p.hasPermission("aadmin.kits.starter") && !plugin.uuidManager.hasEntry(p.getUniqueId())) {
			plugin.starterKit.setPlayerInventory(p, false);
		}

		if (plugin.periodicMessageManager.areMessagesDisabled(p.getUniqueId())) {
			p.sendMessage(plugin.tipsDisabledMessage);
		}else{
			p.sendMessage(plugin.tipsEnabledMessage);
		}
		plugin.uuidManager.addEntry(p.getUniqueId(), p.getName());
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event){
		Player p = event.getEntity();
        if (p.getKiller() == null){
            if (p.hasPermission("aadmin.back")){
                Location loc = p.getLocation();
                plugin.teleportManager.setDeathLocation(p, loc);
                p.sendMessage(ChatColor.AQUA + "Death location saved! Use " + ChatColor.GREEN + "/back" + ChatColor.AQUA + " to return!");
            }
        }else{
            if (p.hasPermission("aadmin.back")){
                p.sendMessage(ChatColor.RED + "Your death location was not saved because you were killed by a player!");
            }
        }
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent event){
		if (event.getEntity() instanceof Player){
			Player p = (Player) event.getEntity();
			if (event.getCause() == EntityDamageEvent.DamageCause.FALL){
				if (p.getAllowFlight()){
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		Player p = event.getPlayer();
		//The player went to a different world, start a sleep check.
		plugin.sleepManager.checkSleepDelayed(p.getWorld());
		if (plugin.prevWorld.containsKey(p)){
			if (!plugin.prevWorld.get(p).equals(p.getWorld())){
				plugin.prevWorld.put(p, p.getWorld());
				if (!p.hasPermission("aadmin.fly")){
					if (p.getAllowFlight() && p.getGameMode().equals(GameMode.SURVIVAL)){
						p.setAllowFlight(false);
						p.setFallDistance(0);
						p.sendMessage(ChatColor.RED + "You do not have permission to fly in this world!");
					}
				}
			}
		}else{
			plugin.prevWorld.put(p, p.getWorld());
		}
	}

	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		Player player = event.getPlayer();
		GameMode newGameMode = event.getNewGameMode();
		GameMode oldGameMode = player.getGameMode();

		if (plugin.getConfig().getBoolean("seperateGameModeInventories", true) && (newGameMode != oldGameMode)) {
            // Save current gamemode inventory:
			if (plugin.isDebug()) {
				plugin.getLogger().log(Level.INFO,"[AAListener] GameMode changed for " + player.getName() + ". Swapping inventories...");
			}
            plugin.inventoryManager.savePlayerInventoryAndExperience(player, oldGameMode.name());
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.inventoryManager.save());
            // Load new gamemode inventory, if stored:
            Inventory storedInventory = plugin.inventoryManager.getStoredInventory(player.getUniqueId(), newGameMode.name());
            if (storedInventory != null) {
                storedInventory.setPlayerInventory(player, true);
            }
        }
	}
}
