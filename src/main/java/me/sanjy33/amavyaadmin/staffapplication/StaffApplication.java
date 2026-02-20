package me.sanjy33.amavyaadmin.staffapplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StaffApplication {
	private final Map<String, String> fields = new HashMap<>();
	private boolean read;
	private final UUID uuid;
	private final String lastKnownName;
	
	public StaffApplication(UUID uuid, String lastKnownName, Map<String, String> fields, boolean read) {
		this.uuid = uuid;
		this.lastKnownName = lastKnownName;
		this.fields.putAll(fields);
		this.read = read;
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public String getLastKnownName() {
		return lastKnownName;
	}

	public Map<String, String> getFields() {
		return fields;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
}
