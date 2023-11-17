package me.sanjy33.amavyaadmin.spy;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import me.sanjy33.amavyaadmin.util.Utils;
import org.bukkit.command.CommandSender;

import java.util.*;

public class SpyManager extends SystemManager {

    private final AmavyaAdmin plugin;

    private final Map<UUID, Set<CommandSender>> spyTargets = new HashMap<>();

    public SpyManager(AmavyaAdmin plugin) {
        super();
        this.plugin = plugin;
        registerCommands();
    }

    private static final String[] commands = {"spy"};
    private void registerCommands() {
        SpyCommandExecutor commandExecutor = new SpyCommandExecutor(plugin, this);
        Utils.registerAndSetupCommands(plugin,commands, commandExecutor,plugin.permissionTabCompleter);
    }

    public Set<CommandSender> getSpies(UUID target) {
        if (spyTargets.containsKey(target)) {
            return spyTargets.get(target);
        }
        Set<CommandSender> spies = new HashSet<>();
        spyTargets.put(target, spies);
        return spies;
    }

    public void removeAgent(CommandSender sender) {
        for (UUID target : spyTargets.keySet()) {
            Set<CommandSender> spies = spyTargets.get(target);
            spies.remove(sender);
        }
    }


}
