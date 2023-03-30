package me.sanjy33.amavyaadmin.teleport;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class TeleportRequest {

    private final Player to;
    private final Player from;
    private final String id;
    private BukkitTask timeoutTask;

    public TeleportRequest(AmavyaAdmin plugin, Player to, Player from, int timeoutTicks, TimeoutCallback onTimeout) {
        this.to = to;
        this.from = from;
        this.id = to.getUniqueId().toString() + "|" + from.getUniqueId().toString();

        this.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin,() -> {
            if (onTimeout != null) {
                onTimeout.onTimeout(this);
            }
        },timeoutTicks);
    }

    public Player getTo() {
        return to;
    }

    public Player getFrom() {
        return from;
    }

    public String getId() {
        return id;
    }

    public boolean involves(Player player) {
        return player.equals(to) || player.equals(from);
    }

    public interface TimeoutCallback {
        void onTimeout(TeleportRequest teleportRequest);
    }
}
