package me.sanjy33.amavyaadmin.spy;

import org.bukkit.command.CommandSender;

import java.util.UUID;

public class Spy {

    private CommandSender agent;
    private UUID target;

    public Spy(CommandSender agent, UUID target) {
        this.agent = agent;
        this.target = target;
    }

    public CommandSender getAgent() {
        return agent;
    }

    public UUID getTarget() {
        return target;
    }
}
