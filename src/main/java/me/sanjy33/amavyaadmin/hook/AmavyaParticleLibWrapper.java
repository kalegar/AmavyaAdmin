package me.sanjy33.amavyaadmin.hook;

import me.sanjy33.amavyaparticlelib.AmavyaParticleLib;
import me.sanjy33.amavyaparticlelib.BurstParticleEvent;
import me.sanjy33.amavyaparticlelib.DoubleSpiralParticleEvent;
import me.sanjy33.amavyaparticlelib.SpiralParticleEvent;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class AmavyaParticleLibWrapper implements AmavyaParticleLibHook{

    public void addSpiralEffect(Player player, Particle particle, long duration, float length, double radius) {
        AmavyaParticleLib.addParticleEvent(new SpiralParticleEvent(player, particle, duration, length, radius));
    }

    public void addDoubleSpiralEffect(Player player, Particle particle, long duration, float length, double radius) {
        AmavyaParticleLib.addParticleEvent(new DoubleSpiralParticleEvent(player, particle, duration, length, radius));
    }

    public void addBurstEffect(Player player, Particle particle, long duration, float period, double radius, int steps) {
        AmavyaParticleLib.addParticleEvent(new BurstParticleEvent(player, particle, duration, period, 0.25, radius, steps));
    }
}
