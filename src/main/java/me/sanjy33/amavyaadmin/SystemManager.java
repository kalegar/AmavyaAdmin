package me.sanjy33.amavyaadmin;

import java.util.ArrayList;
import java.util.List;

public abstract class SystemManager {
	
	private static List<SystemManager> managers = new ArrayList<SystemManager>();
	
	public SystemManager(){
		managers.add(this);
	}
	
	public void reload() {
		
	}
	public void save() {
		
	}
	
	public static List<SystemManager> getManagers() {
		return managers;
	}

}
