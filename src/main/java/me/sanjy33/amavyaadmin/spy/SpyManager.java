package me.sanjy33.amavyaadmin.spy;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import org.bukkit.command.CommandSender;

import java.util.*;

public class SpyManager extends SystemManager {

    private final AmavyaAdmin plugin;
    private SpyCommandExecutor commandExecutor;

    private Map<UUID, Set<CommandSender>> spyTargets = new HashMap<>();

    public SpyManager(AmavyaAdmin plugin) {
        super();
        this.plugin = plugin;
        registerCommands();
    }

    private void registerCommands() {
        commandExecutor = new SpyCommandExecutor(plugin, this);
        plugin.getCommand("spy").setExecutor(commandExecutor);
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
