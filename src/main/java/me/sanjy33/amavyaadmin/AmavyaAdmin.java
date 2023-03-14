package me.sanjy33.amavyaadmin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.sanjy33.amavyaadmin.hook.AmavyaParticleLibDefault;
import me.sanjy33.amavyaadmin.hook.AmavyaParticleLibHook;
import me.sanjy33.amavyaadmin.hook.AmavyaParticleLibWrapper;
import me.sanjy33.amavyaadmin.inventory.InventoryManager;
import me.sanjy33.amavyaadmin.motd.MessageOfTheDay;
import me.sanjy33.amavyaadmin.sleep.SleepManager;
import me.sanjy33.amavyaadmin.spy.SpyManager;
import me.sanjy33.amavyaadmin.tabcompleter.OperatorTabCompleter;
import me.sanjy33.amavyaadmin.tabcompleter.PermissionTabCompleter;
import me.sanjy33.amavyaadmin.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.sanjy33.amavyaadmin.home.HomeManager;
import me.sanjy33.amavyaadmin.jail.JailManager;
import me.sanjy33.amavyaadmin.message.MessageManager;
import me.sanjy33.amavyaadmin.mute.MuteManager;
import me.sanjy33.amavyaadmin.periodicmessage.PeriodicMessageManager;
import me.sanjy33.amavyaadmin.staffapplication.StaffApplicationManager;
import me.sanjy33.amavyaadmin.teleport.TeleportManager;
import me.sanjy33.amavyaadmin.inventory.Inventory;
import me.sanjy33.amavyaadmin.util.UUIDManager;
import me.sanjy33.amavyaadmin.vanish.VanishManager;
import net.luckperms.api.LuckPerms;

public class AmavyaAdmin extends JavaPlugin implements Listener{

	private final AAListener playerListener = new AAListener(this);
	private final AACommandExecutor commandExecutor = new AACommandExecutor(this);
	public Map<Player, World> prevWorld = new HashMap<Player, World>();
	public String lockdownMessage = "The server is under lockdown! Come back later!";
	public String tipsEnabledMessage;
	public String tipsDisabledMessage;
	public String mutedMessage;
	public Boolean lockdown = false;
	public Inventory starterKit;
	public List<String> rules = new ArrayList<String>();
	public String staffPermissionGroup = "mod";
	public String defaultPermissionGroup = "default";
	public LuckPerms luckPermsApi;
	public MessageManager messageManager;
	public PeriodicMessageManager periodicMessageManager;
	public StaffApplicationManager staffApplicationManager;
	public JailManager jailManager;
	public VanishManager vanishManager;
	public HomeManager homeManager;
	public UUIDManager uuidManager;
	public MuteManager muteManager;
	public TeleportManager teleportManager;
	public InventoryManager inventoryManager;
	public SpyManager spyManager;
	public SleepManager sleepManager;
	public AmavyaParticleLibHook particleLibHook;
	public MessageOfTheDay messageOfTheDay;
	private boolean debug = false;
	public OperatorTabCompleter operatorTabCompleter;
	public PermissionTabCompleter permissionTabCompleter;
	
	@Override
	public void onDisable() {
		save();
		getLogger().info(ChatColor.GREEN + "[AmavyaAdmin] AmavyaAdmin Disabled");
	}

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		if (!setupLuckPerms()) {
        	getLogger().info(ChatColor.RED + "[AmavyaAdmin] ERROR: Failed to load LuckPerms API!");
        }
		operatorTabCompleter = new OperatorTabCompleter();
		permissionTabCompleter = new PermissionTabCompleter();
		periodicMessageManager = new PeriodicMessageManager(this);
		messageManager = new MessageManager(this);
		staffApplicationManager = new StaffApplicationManager(this);
		jailManager = new JailManager(this);
		vanishManager = new VanishManager(this);
		homeManager = new HomeManager(this);
		uuidManager = new UUIDManager(this);
		muteManager = new MuteManager(this);
		teleportManager = new TeleportManager(this);
		inventoryManager = new InventoryManager(this);
		spyManager = new SpyManager(this);
		sleepManager = new SleepManager(this);
		setupAmavyaParticleLib();
		messageOfTheDay = new MessageOfTheDay(this);
		reload();
		registerCommands();
		getServer().getPluginManager().registerEvents(playerListener, this);
		PluginDescriptionFile pdfFile = this.getDescription();
        getLogger().info( ChatColor.GREEN + pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}
	
	private boolean setupLuckPerms() {
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (provider != null) {
			luckPermsApi = provider.getProvider();
			return true;
		}
		return false;
	}

	private void setupAmavyaParticleLib() {
		if (Bukkit.getPluginManager().getPlugin("AmavyaParticleLib") != null) {
			this.particleLibHook = new AmavyaParticleLibWrapper();
			getLogger().info("Particle effects enabled!");
		}else{
			this.particleLibHook = new AmavyaParticleLibDefault();
			getLogger().info("AmavyaParticleLib not found. Particle effects disabled!");
		}
	}

	private static final String[] opCommands = {"lockdown","aareload"};
	private static final String[] commands = {"fly","motd","ts","rules","setkit","tips","knownaliases"};
	
	private void registerCommands(){
		Utils.registerAndSetupCommands(this,opCommands,commandExecutor,operatorTabCompleter);
		Utils.registerAndSetupCommands(this,commands,commandExecutor,permissionTabCompleter);
	}
	
	public void reload() {
		reloadConfig();
		SystemManager.getManagers().forEach(SystemManager::reload);
		lockdown = this.getConfig().getBoolean("lockdown");
		lockdownMessage = this.getConfig().getString("messages.lockdown");
		tipsEnabledMessage = this.getConfig().getString("messages.tipsenabled");
		tipsDisabledMessage = this.getConfig().getString("messages.tipsdisabled");
		mutedMessage = this.getConfig().getString("messages.muted");
		debug = this.getConfig().getBoolean("debug",false);
	    if (this.getConfig().contains("staffapplications.permissiongroups.staff")) {
	    	staffPermissionGroup = this.getConfig().getString("staffapplications.permissiongroups.staff");
	    }
	    if (this.getConfig().contains("staffapplications.permissiongroups.default")) {
	    	defaultPermissionGroup = this.getConfig().getString("staffapplications.permissiongroups.default");
	    }
	    if (this.getConfig().contains("rules")) {
	    	rules.clear();
	    	List<String> stringList = this.getConfig().getStringList("rules");
	    	stringList.forEach(rule -> rules.add(ChatColor.translateAlternateColorCodes('&',rule)));
	    }
	    if (this.getConfig().contains("kits.starter")){
	    	starterKit = Inventory.load(this.getConfig().getConfigurationSection("kits.starter"));
	    }else {
			getLogger().info("[AmavyaAdmin] Did not find starter kit in config.yml Set one with /setkit in-game");
		}

	}
	
	public void save(){
		SystemManager.getManagers().forEach(SystemManager::save);
		this.saveConfig();
	}

	public boolean isDebug() {
		return debug;
	}
}
