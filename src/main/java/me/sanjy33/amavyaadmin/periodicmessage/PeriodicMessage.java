package me.sanjy33.amavyaadmin.periodicmessage;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;

import me.sanjy33.amavyaadmin.AmavyaAdmin;

public class PeriodicMessage extends BukkitRunnable{
	private List<String> messages;
	private final long frequency;
	private final String permission;
	private final String name;
	private final PeriodicMessageManager manager;
	private final int mode;
	private int index = 0;
	
	public PeriodicMessage(AmavyaAdmin plugin, PeriodicMessageManager manager, String name, List<String> messages, long frequency, String permission, int mode) {
		this.manager = manager;
		this.name = name;
		this.messages = messages;
		this.frequency = frequency;
		if (permission != null) {
			this.permission = "aadmin.pm." + permission;
		}else{
			this.permission = null;
		}
		this.mode = mode;
		runTaskTimer(plugin,1L,frequency);
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
	
	public List<String> getMessages() {
		return messages;
	}
	
	public void addMessage(String message) {
		if (messages==null) {
			messages = new ArrayList<>();
		}
		this.messages.add(message);
	}
	
	public long getFrequency() {
		return frequency;
	}

	@Override
	public void run() {
		if (mode == 0) {
			int newIndex = (int) Math.floor(Math.random()*messages.size());
			if (newIndex == index) {
				newIndex ++;
			}
			index = newIndex;
			if (index >= messages.size()) {
				index = 0;
			}
		}
		String message = messages.get(index);
		if (mode == 1) {
			index += 1;
			if (index >= messages.size()) {
				index = 0;
			}
		}
		if (message==null){
			System.out.println("[AmavyaAdmin] Error getting random message for timed message group " + name);
			return;
		}
		if (message.length() > 0){
			String s = ChatColor.translateAlternateColorCodes('&', message);
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (permission == null || p.hasPermission(permission)) {
					if (!manager.areMessagesDisabled(p.getUniqueId())) {
						p.sendMessage(s);
					}
				}
			}
		}else{
			System.out.println("[AmavyaAdmin] Error getting random message for timed message group " + name);
		}
	}
}
