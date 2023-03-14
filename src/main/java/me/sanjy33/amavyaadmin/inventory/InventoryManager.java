package me.sanjy33.amavyaadmin.inventory;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import me.sanjy33.amavyaadmin.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class InventoryManager extends SystemManager {

    private final AmavyaAdmin plugin;
    private final String fileName = "inventories.yml";
    public static final int MAX_STORED_INVENTORIES = 10;

    private final Map<UUID, Map<String, Inventory>> inventories = new HashMap<>();

    public InventoryManager(AmavyaAdmin plugin) {
        super();
        this.plugin = plugin;
        registerPermissions();
        registerCommands();
        load();
    }

    public void savePlayerInventoryAndExperience(Player player, String inventoryKey) {
        UUID uuid = player.getUniqueId();
        Map<String, Inventory> playerInventories = inventories.computeIfAbsent(uuid, k -> new HashMap<>());
        Inventory inventory = new Inventory(player);
        playerInventories.put(inventoryKey, inventory);
        if (plugin.isDebug()) {
            plugin.getLogger().log(Level.INFO,"[Inventory Manager] Stored inventory for " + player.getName() + " with key " + inventoryKey);
        }
    }

    public Inventory getStoredInventory(UUID uuid, String inventoryKey) {
        Map<String, Inventory> playerInventories = inventories.get(uuid);
        if (playerInventories == null) {
            return null;
        }
        if (plugin.isDebug()) {
            plugin.uuidManager.getName(uuid, ((name, uuid1) -> {
                plugin.getLogger().log(Level.INFO,"[Inventory Manager] Retrieved inventory for " + name + " with key " + inventoryKey);
            }));
        }
        return playerInventories.get(inventoryKey);
    }

    public boolean isInventoryStored(UUID uuid, String key) {
        if (inventories.containsKey(uuid)) {
            Map<String, Inventory> map = inventories.get(uuid);
            return map.containsKey(key);
        }
        return false;
    }

    public Inventory clearStoredInventory(UUID uuid, String key) {
        if (plugin.isDebug()) {
            plugin.uuidManager.getName(uuid, ((name, uuid1) -> {
                plugin.getLogger().log(Level.INFO,"[Inventory Manager] Retrieved and cleared inventory for " + name + " with key " + key);
            }));
        }
        if (inventories.containsKey(uuid)) {
            Map<String, Inventory> map = inventories.get(uuid);
            return map.remove(key);
        }
        return null;
    }

    public int getStoredInventoryCount(UUID uuid) {
        if (inventories.containsKey(uuid)) {
            return inventories.get(uuid).size();
        }
        return 0;
    }

    private static final String[] commands = {
            "inventory"
    };
    private void registerCommands() {
        InventoryCommandExecutor commandExecutor = new InventoryCommandExecutor(this);
        Utils.registerAndSetupCommands(plugin,commands, commandExecutor,plugin.permissionTabCompleter);
    }

    private void registerPermissions() {
        for (int i = 1; i <= MAX_STORED_INVENTORIES; i ++) {
            Bukkit.getPluginManager().addPermission(new Permission("aadmin.inventory.max."+i, PermissionDefault.FALSE));
        }
    }

    @Override
    public void save() {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        for (UUID uuid : inventories.keySet()) {
            Map<String, Inventory> playerInventories = inventories.get(uuid);
            if (playerInventories == null) {
                continue;
            }
            for (String key : playerInventories.keySet()) {
                Inventory inventory = playerInventories.get(key);
                ConfigurationSection section = c.createSection(uuid.toString() + "." + key);
                inventory.save(section);
            }
        }
        try {
            c.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
        }
    }

    private void load() {
        reload();
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        Set<String> uuids = c.getKeys(false);
        for (String u : uuids) {
            UUID uuid = UUID.fromString(u);
            ConfigurationSection section = c.getConfigurationSection(u);
            if (section == null) continue;
            Set<String> keys = section.getKeys(false);
            if (keys.size() < 1) continue;
            Map<String, Inventory> playerInventories = new HashMap<>();
            for (String key : keys) {
                Inventory inventory = Inventory.load(section.getConfigurationSection(key));
                playerInventories.put(key, inventory);
            }
            inventories.put(uuid, playerInventories);
        }
    }
}
