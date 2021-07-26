package me.sanjy33.amavyaadmin.jail;

import java.util.UUID;

import org.bukkit.Location;

public class JailCell {
	
	private UUID occupant=null;
	private long timeRelease=0;
	private Location location=null;
	private int id;
	private String reason="Breaking server rules.";
	private UUID jailer=null;
	private String jailerName="Console";
	
	public JailCell(int i){
		id=i;
	}
	
	public boolean isEmpty(){
		return occupant == null;
	}
	
	public void setLocation(Location l){
		location = l;
	}
	
	public boolean jailUUID(UUID u, UUID jailer, String jailerName, long time, String reason){
		boolean result = jailUUID(u,time);
		if (result){
			this.jailer=jailer;
			this.jailerName=jailerName;
			this.reason=reason;
		}
		return result;
	}
	
	public boolean jailUUID(UUID u, long time){
		if (occupant==null){
			occupant = u;
			timeRelease = System.currentTimeMillis()+time;
			return true;
		}
		return false;
	}
	
	public void reset() {
		setOccupant(null);
		setTimeWhenReleased(0);
		setReason("Breaking server rules");
		setJailer(null);
		setJailerName("Console");
	}
	
	public UUID getOccupant(){
		return occupant;
	}
	
	public long getTimeWhenReleased(){
		return timeRelease;
	}
	
	public String getReason(){
		return reason;
	}
	
	public UUID getJailer(){
		return jailer;
	}
	
	public String getJailerName(){
		return jailerName;
	}
	
	public Location getLocation(){
		return location;
	}
	
	public int getID(){
		return id;
	}
	
	public void setOccupant(UUID u){
		occupant=u;
	}
	
	public void setTimeWhenReleased(long t){
		timeRelease=t;
	}
	
	public void setReason(String r){
		reason=r;
	}
	
	public void setJailer(UUID u){
		jailer=u;
	}
	
	public void setJailerName(String n){
		jailerName=n;
	}
	
}
