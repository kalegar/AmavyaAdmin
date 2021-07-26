package me.sanjy33.amavyaadmin.hook;

import org.bukkit.Particle;
import org.bukkit.entity.Player;

public interface AmavyaParticleLibHook {

    void addSpiralEffect(Player player, Particle particle, long duration, float length, double radius);

    void addBurstEffect(Player player, Particle particle, long duration, float period, double radius, int steps);

    void addDoubleSpiralEffect(Player player, Particle particle, long duration, float length, double radius);
}
