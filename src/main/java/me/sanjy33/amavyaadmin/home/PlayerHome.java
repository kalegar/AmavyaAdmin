package me.sanjy33.amavyaadmin.home;

import java.util.UUID;

import org.bukkit.Location;

public class PlayerHome {
	
	private final Location location;
	private String name;
	private final UUID owner;
	
	public PlayerHome(UUID owner, Location location) {
		this(owner, location, "default");
	}
	
	public PlayerHome(UUID owner, Location location, String name) {
		this.owner = owner;
		this.location = location;
		this.name = name;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public UUID getOwner() {
		return owner;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

}
