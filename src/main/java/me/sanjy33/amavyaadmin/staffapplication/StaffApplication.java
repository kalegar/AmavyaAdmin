package me.sanjy33.amavyaadmin.staffapplication;

import java.util.List;
import java.util.UUID;

public class StaffApplication {
	private final List<String> applicationPages;
	private boolean read;
	private final UUID uuid;
	private final String lastKnownName;
	
	public StaffApplication(UUID uuid, String lastKnownName, List<String> applicationPages, boolean read) {
		this.uuid = uuid;
		this.lastKnownName = lastKnownName;
		this.applicationPages = applicationPages;
		this.read = read;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public String getLastKnownName() {
		return lastKnownName;
	}
	
	public List<String> getApplicationPages() {
		return applicationPages;
	}
	
	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
}
