package me.sanjy33.amavyaadmin.sleep;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SleepManager extends SystemManager {

    private final AmavyaAdmin plugin;
    private float sleepPercentageRequired = 0.5f;
    private final List<Player> sleeping = new ArrayList<>();

    private final HashSet<Player> sleepIgnoredPlayers;

    public SleepManager(AmavyaAdmin plugin) {
        super();
        this.plugin = plugin;
        reload();
        sleepIgnoredPlayers = new HashSet<>();
    }

    private void checkSleep(World world) {
        //World must be a normal world:
        if (!world.getEnvironment().equals(World.Environment.NORMAL)) {
            return;
        }
        //Valid players are in this world, not in spectator mode, and visible.
        int totalValidPlayers = 0;
        int sleepingPlayers = 0;

        sleeping.clear();
        for (Player player : world.getPlayers()) {
            if (!player.getGameMode().equals(GameMode.SPECTATOR) && !plugin.vanishManager.isPlayerInvisible(player)) {
                totalValidPlayers++;
            }
            if (player.isSleeping()) {
                sleepingPlayers ++;
                sleeping.add(player);
            }
        }

        int requiredPopulation = (int) Math.max(1.0, Math.ceil(((float)totalValidPlayers) * sleepPercentageRequired));

        if (sleepingPlayers > 0) {
            sendSleepingMessage(world,requiredPopulation,totalValidPlayers);
        }

        if (sleepingPlayers >= requiredPopulation) {
            startSleep(world);
        }
    }

    private void startSleep(World world) {
        for (Player player : world.getPlayers()) {
            if (!player.isSleeping()) {
                addSleepIgnored(player);
            }
        }
    }

    private void addSleepIgnored(Player player) {
        sleepIgnoredPlayers.add(player);
        player.setSleepingIgnored(true);
    }

    public void clearSleepingIgnored() {
        for (Player player : sleepIgnoredPlayers) {
            if (player != null && player.isValid() && player.isOnline()) {
                player.setSleepingIgnored(false);
            }
        }

        sleepIgnoredPlayers.clear();
    }


    public void checkSleepDelayed(World world) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> checkSleep(world), 1);
    }

    public void reload() {
        sleepPercentageRequired = (float) plugin.getConfig().getDouble("sleepPercentageRequired",0.5d);
    }

    private void sendSleepingMessage(World world, int required, int total) {
        TextColor col = sleeping.size() >= required ? NamedTextColor.GREEN : NamedTextColor.GRAY;

        StringBuilder sb = new StringBuilder();
        for (Player p : sleeping) {
            sb.append(p.getName()).append(",\n");
        }
        sb.delete(sb.length()-2,sb.length());
        Component sleepingComponent = Component.text(String.valueOf(sleeping.size()), col)
                .hoverEvent(HoverEvent.showText(Component.text(sb.toString())))
                .append(Component.text("/" + total + " players are now sleeping.", NamedTextColor.GOLD));
        for (Player player : world.getPlayers()) {
            player.sendMessage(sleepingComponent);
        }
    }
}
