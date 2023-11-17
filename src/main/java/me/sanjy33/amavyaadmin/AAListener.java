package me.sanjy33.amavyaadmin;

import java.util.*;
import java.util.logging.Level;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.sanjy33.amavyaadmin.inventory.Inventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
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
    	    sender.sendMessage(Component.text().content("[SPY]").color(NamedTextColor.AQUA).append(Component.text(p.getName() + " used command:", NamedTextColor.GRAY)));
    	    sender.sendMessage(Component.text(event.getMessage(), NamedTextColor.GRAY));
        }
    	MutedPlayer mp = plugin.muteManager.getMutedPlayer(u);
    	if (mp != null) {
    		if (plugin.muteManager.isCommandBanned(cmd)) {
    			long time = mp.getUnMuteTime() - System.currentTimeMillis();
    			if (time <= 0) {
    				plugin.muteManager.unMute(mp);
    			}else {
					String timeString = TimeParser.parseLong(time, false);
					if (timeString == null) {
						timeString = "0 seconds";
					}
    				String msg = plugin.mutedMessage.replaceAll("%time%",timeString).replaceAll("%reason%",mp.getReason());
    				p.sendMessage(msg);
    				event.setCancelled(true);
    				return;
    			}
    		}
    	}
    	if (plugin.jailManager.isPlayerInJail(p.getUniqueId())) {
    		if (plugin.jailManager.isCommandBlocked(cmd)) {
    			p.sendMessage(Component.text("You can't use that command in jail.", NamedTextColor.RED));
				event.setCancelled(true);
    		}
    	}
    	
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncChatEvent event){
		MutedPlayer mp = plugin.muteManager.getMutedPlayer(event.getPlayer().getUniqueId());
		if (mp != null){
			long time = mp.getUnMuteTime() - System.currentTimeMillis();
			if (time <= 0) {
				plugin.muteManager.unMute(mp);
				return;
			}
			String timeStr = TimeParser.parseLong(time, false);
			if (timeStr == null)
				timeStr = "0 seconds";
			String msg = plugin.mutedMessage.replaceAll("%time%",timeStr).replaceAll("%reason%",mp.getReason());
			event.getPlayer().sendMessage(msg);
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onSignChange(SignChangeEvent event){
		Player p = event.getPlayer();
		List<Component> lines = event.lines();
		if (lines.get(0) instanceof TextComponent) {
			if (((TextComponent) lines.get(0)).content().equalsIgnoreCase("[BuyRegion]")) {
				if (!p.hasPermission("aadmin.sign.create")) {
					p.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					event.setCancelled(true);
					return;
				}
			}
			if (((TextComponent) lines.get(0)).content().equalsIgnoreCase("[Sold!]")) {
				if (!p.hasPermission("aadmin.sign.create")) {
					p.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onBedLeave(PlayerBedLeaveEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("aadmin.home.bed")) {
			if (!plugin.homeManager.homeExists(player.getUniqueId())) {
				plugin.homeManager.setHome(new PlayerHome(player.getUniqueId(),player.getLocation(),player.getName()));
				Component component = Component.text("Your home has been set to your bed location!", Style.style(NamedTextColor.GREEN, TextDecoration.ITALIC));
				player.sendActionBar(component);
			}
		}
		//If anyone gets out of a bed clear all sleep ignored players
        plugin.sleepManager.clearSleepingIgnored();
        plugin.sleepManager.checkSleepDelayed(event.getPlayer().getWorld());
	}
	
	@EventHandler
	public void onBedEnter(PlayerBedEnterEvent event) {
		if (event.getBedEnterResult().equals(PlayerBedEnterEvent.BedEnterResult.OK)) {
			plugin.sleepManager.checkSleepDelayed(event.getPlayer().getWorld());
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		plugin.spyManager.removeAgent(p);
		if (plugin.vanishManager.getSilentQuit(p.getUniqueId())) {
			event.quitMessage(null);
			for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
				if (pl.getUniqueId().equals(p.getUniqueId())) continue;
				if (pl.hasPermission("aadmin.vanish.silentnotify")) {
					pl.sendMessage(Component.text("~" + p.getName() + " left the game silently~", NamedTextColor.GRAY));
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
		if (plugin.lockdown){
			if (!p.hasPermission("aadmin.lockdown.bypass")){
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER,Component.text(plugin.lockdownMessage));
				return;
			}
		}
		if (!p.isBanned())
			event.allow();
	}
	
//////Update to UUID when bukkit supports it!!!
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		final Player p = event.getPlayer();
		if (plugin.lockdown){
			p.sendMessage(Component.text("<<< The server is currently locked down! >>>", NamedTextColor.RED));
		}
		if (plugin.vanishManager.getSilentJoin(p.getUniqueId())) {
			p.sendMessage(Component.text("~You joined silently~", NamedTextColor.GRAY));
			event.joinMessage(null);
			if (!plugin.vanishManager.isPlayerInvisible(p)) {
				plugin.vanishManager.toggleInvisibility(p);
			}
			for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
				if (pl.getUniqueId().equals(p.getUniqueId())) continue;
				if (pl.hasPermission("aadmin.vanish.silentnotify")) {
					pl.sendMessage(Component.text("~" + p.getName() + " joined the game silently~", NamedTextColor.GRAY));
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
			p.sendMessage(Component.text("You have been released from jail.", NamedTextColor.GREEN));
			p.teleport(p.getLocation().getWorld().getSpawnLocation());
			plugin.jailManager.removeToBeReleased(p.getUniqueId().toString());
		}
		
		JailCell cell = plugin.jailManager.getCell(p.getUniqueId());
		if (cell != null) {
			p.sendMessage(Component.text("You have been jailed! Use /jailstatus", NamedTextColor.RED));
			p.teleport(cell.getLocation());
		}
		
		if (p.hasPermission("aadmin.staffapps.check")){
			int amm = plugin.staffApplicationManager.getUnreadApplicationCount();
			if (amm>0){
				p.sendMessage(Component.text("[App] There are " + amm + " new staff applications! Use /apply check", NamedTextColor.AQUA));
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
                p.sendMessage(Component.text("Death location saved! Use",NamedTextColor.AQUA)
								.appendSpace()
								.append(Component.text("/back", NamedTextColor.GREEN))
								.appendSpace()
								.append(Component.text("to return!",NamedTextColor.AQUA))
				);
            }
        }else{
            if (p.hasPermission("aadmin.back")){
                p.sendMessage(Component.text("Your death location was not saved because you were killed by a player!", NamedTextColor.RED));
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
						p.sendMessage(Component.text("You do not have permission to fly in this world!", NamedTextColor.RED));
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
