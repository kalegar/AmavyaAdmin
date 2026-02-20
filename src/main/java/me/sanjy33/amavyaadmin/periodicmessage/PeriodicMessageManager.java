package me.sanjy33.amavyaadmin.periodicmessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import me.sanjy33.amavyaadmin.AmavyaAdmin;
import me.sanjy33.amavyaadmin.SystemManager;
import me.sanjy33.amavyaadmin.util.TimeParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PeriodicMessageManager extends SystemManager{

	private final String fileName = "periodic_messages.yml";
	private FileConfiguration config;
	private final Map<String,PeriodicMessage> messages = new HashMap<>();
	private final Set<UUID> playerTimedMessagesDisabled = new HashSet<>();
	private final AmavyaAdmin plugin;
	
	public PeriodicMessageManager(AmavyaAdmin plugin) {
		super();
		this.plugin = plugin;
		registerCommands();
		load();
	}

	private void registerCommands() {
		LiteralArgumentBuilder<CommandSourceStack> commandBuilder =
				Commands.literal("pm")
						.requires(sender -> sender.getSender().hasPermission("aadmin.pm.edit"))
						.then(Commands.literal("category")
								.then(Commands.literal("add")
										.then(Commands.argument("category", StringArgumentType.word())
												.executes(ctx -> {
													CommandSender sender = ctx.getSource().getSender();
													String category = StringArgumentType.getString(ctx, "category");
													if (messages.containsKey(category)) {
														sender.sendMessage(Component.text("A periodic message category already exists with that name.", NamedTextColor.RED));
														return Command.SINGLE_SUCCESS;
													}
													messages.put(category, new PeriodicMessage(plugin, this, category));
													sender.sendMessage(Component.text("Added new periodic message category '" + category + "'",NamedTextColor.GREEN));
													save();
													return Command.SINGLE_SUCCESS;
												})))
								.then(Commands.literal("delete")
										.then(Commands.argument("category", StringArgumentType.word())
												.executes(ctx -> {
													CommandSender sender = ctx.getSource().getSender();
													String category = StringArgumentType.getString(ctx, "category");
													if (!messages.containsKey(category)) {
														sender.sendMessage(Component.text("A periodic message category called '"+category+"' does not exist.", NamedTextColor.RED));
														return Command.SINGLE_SUCCESS;
													}
													messages.remove(category);
													sender.sendMessage(Component.text("Removed periodic message category called '"+category+"'.",NamedTextColor.GREEN));
													save();
													return Command.SINGLE_SUCCESS;
												})))
								.then(Commands.literal("frequency")
										.then(Commands.argument("category",StringArgumentType.word())
											.then(Commands.argument("frequency", StringArgumentType.word())
													.executes(ctx -> {
														CommandSender sender = ctx.getSource().getSender();
														String category = StringArgumentType.getString(ctx, "category");
														if (!messages.containsKey(category)) {
															sender.sendMessage(Component.text("A periodic message category called '"+category+"' does not exist.", NamedTextColor.RED));
															return Command.SINGLE_SUCCESS;
														}
														String freqString = StringArgumentType.getString(ctx, "frequency");
														long frequency = TimeParser.parseString(freqString);
														if (frequency <= 0) {
															sender.sendMessage(Component.text("Invalid frequency. Format: 5m10s",NamedTextColor.RED));
															return Command.SINGLE_SUCCESS;
														}
														PeriodicMessage message = messages.get(category);
														message.cancelTask();
														message.setFrequency(frequency);
														message.start();
														messages.put(category,message);
														save();
														sender.sendMessage(Component.text("Frequency set to '" + freqString + ".",NamedTextColor.GREEN));
														return Command.SINGLE_SUCCESS;
													}))))
								.then(Commands.literal("permission")
										.then(Commands.argument("category",StringArgumentType.word())
												.then(Commands.argument("permission", StringArgumentType.word())
														.executes(ctx -> {
															CommandSender sender = ctx.getSource().getSender();
															String category = StringArgumentType.getString(ctx, "category");
															if (!messages.containsKey(category)) {
																sender.sendMessage(Component.text("A periodic message category called '"+category+"' does not exist.", NamedTextColor.RED));
																return Command.SINGLE_SUCCESS;
															}
															String permission = StringArgumentType.getString(ctx, "permission");
															PeriodicMessage message = messages.get(category);
															message.setPermission(permission);
															messages.put(category,message);
															save();
															sender.sendMessage(Component.text("Permission set to '" + message.getPermission(),NamedTextColor.GREEN));
															return Command.SINGLE_SUCCESS;
														}))))
								.then(Commands.literal("mode")
										.then(Commands.argument("category",StringArgumentType.word())
												.then(Commands.argument("mode", IntegerArgumentType.integer(0,1))
														.executes(ctx -> {
															CommandSender sender = ctx.getSource().getSender();
															String category = StringArgumentType.getString(ctx, "category");
															if (!messages.containsKey(category)) {
																sender.sendMessage(Component.text("A periodic message category called '"+category+"' does not exist.", NamedTextColor.RED));
																return Command.SINGLE_SUCCESS;
															}
															int mode = IntegerArgumentType.getInteger(ctx, "mode");
															PeriodicMessage message = messages.get(category);
															message.setMode(mode);
															messages.put(category,message);
															save();
															sender.sendMessage(Component.text("Mode set to '" + mode,NamedTextColor.GREEN));
															return Command.SINGLE_SUCCESS;
														})))))
						.then(Commands.literal("message")
								.then(Commands.literal("add")
										.then(Commands.argument("category",StringArgumentType.word())
												.then(Commands.argument("message", ArgumentTypes.component())
														.executes(ctx -> {
															CommandSender sender = ctx.getSource().getSender();
															String category = StringArgumentType.getString(ctx, "category");
															if (!messages.containsKey(category)) {
																sender.sendMessage(Component.text("A periodic message category called '"+category+"' does not exist.", NamedTextColor.RED));
																return Command.SINGLE_SUCCESS;
															}
															final Component component = ctx.getArgument("message", Component.class);
															PeriodicMessage message = messages.get(category);
															message.addMessage(component);
															messages.put(category,message);
															save();
															sender.sendMessage(Component.text("Added new message to category '"+category+"' with index '"+(message.getMessages().size()-1)+"': ",NamedTextColor.GREEN).append(component));
															return Command.SINGLE_SUCCESS;
														}))))
								.then(Commands.literal("list")
										.then(Commands.argument("category", StringArgumentType.word())
												.executes(ctx -> {
													CommandSender sender = ctx.getSource().getSender();
													String category = StringArgumentType.getString(ctx, "category");
													if (!messages.containsKey(category)) {
														sender.sendMessage(Component.text("A periodic message category called '"+category+"' does not exist.", NamedTextColor.RED));
														return Command.SINGLE_SUCCESS;
													}
													PeriodicMessage message = messages.get(category);
													List<Component> messages = message.getMessages();
													for (int i = 0; i < messages.size(); i++) {
														sender.sendMessage(Component.text("["+i+"]: ").append(messages.get(i)));
													}
													return Command.SINGLE_SUCCESS;
												})))
								.then(Commands.literal("delete")
										.then(Commands.argument("category", StringArgumentType.word())
												.then(Commands.argument("index", IntegerArgumentType.integer(0))
														.executes(ctx -> {
															CommandSender sender = ctx.getSource().getSender();
															String category = StringArgumentType.getString(ctx, "category");
															if (!messages.containsKey(category)) {
																sender.sendMessage(Component.text("A periodic message category called '"+category+"' does not exist.", NamedTextColor.RED));
																return Command.SINGLE_SUCCESS;
															}
															int index = IntegerArgumentType.getInteger(ctx, "index");
															PeriodicMessage message = messages.get(category);
															if (message.deleteMessage(index)) {
																sender.sendMessage(Component.text("Deleted message at index " + index,NamedTextColor.GREEN));
																save();
															}else{
																sender.sendMessage(Component.text("No message with index " + index,NamedTextColor.RED));
															}
															return Command.SINGLE_SUCCESS;
														})))));

		LiteralCommandNode<CommandSourceStack> pmCommand = commandBuilder.build();

		LiteralCommandNode<CommandSourceStack> tipsCommand = Commands.literal("tips")
				.requires(sender -> sender.getSender().hasPermission("aadmin.tips") && sender.getExecutor() instanceof Player)
					.executes(ctx -> {
						Player player = (Player) ctx.getSource().getExecutor();
						Component status = toggleMessagesDisabled(player.getUniqueId()) ? Component.text("ON",NamedTextColor.GREEN) : Component.text("OFF",NamedTextColor.RED);
						player.sendMessage(Component.text("Messages toggled ",NamedTextColor.AQUA).append(status).append(Component.text("!",NamedTextColor.AQUA)));
						return Command.SINGLE_SUCCESS;
					}).build();

		plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
			commands.registrar().register(pmCommand);
			commands.registrar().register(tipsCommand);
		});
	}
	
	public boolean areMessagesDisabled(UUID uuid) {
		return playerTimedMessagesDisabled.contains(uuid);
	}
	
	public boolean toggleMessagesDisabled(UUID uuid) {
		if (playerTimedMessagesDisabled.contains(uuid)) {
			playerTimedMessagesDisabled.remove(uuid);
			return true;
		}else {
			playerTimedMessagesDisabled.add(uuid);
			return false;
		}
	}

	private void load() {
		File file = new File(plugin.getDataFolder(), fileName);
		config = YamlConfiguration.loadConfiguration(file);

		List<String> list = config.getStringList("disabled");
		list.forEach(u -> playerTimedMessagesDisabled.add(UUID.fromString(u)));

	}
	
	@Override
	public void reload() {
		for (PeriodicMessage tm : messages.values()){
			if (tm.isStarted())
				tm.cancelTask();
			tm.unRegisterPermission();
		}
		messages.clear();
		File file = new File(plugin.getDataFolder(), fileName);
		config = YamlConfiguration.loadConfiguration(file);

		InputStream input = plugin.getResource(fileName);
		//Look for defaults in jar
		if (input != null) {
			Reader defConfigStream = new InputStreamReader(input, StandardCharsets.UTF_8);
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			if (!file.exists()) {
				try {
					defConfig.save(file);
				} catch (IOException e) {
					Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
				}
				config = defConfig;
			}
		}

		ConfigurationSection messageSection = config.getConfigurationSection("periodicmessages");
		if (messageSection != null) {
			for (String key : messageSection.getKeys(false)) {
				ConfigurationSection subSection = messageSection.getConfigurationSection(key);
				try {
					PeriodicMessage tm = new PeriodicMessage(
							plugin,
							this,
							subSection
					);
					tm.registerPermission();
					tm.start();
					messages.put(tm.getName(),tm);
					plugin.getLogger().info("Started Periodic Message Task '" + key + "'");
				} catch (Exception e) {
					plugin.getLogger().warning("Error loading periodic message category '" + key + "'");
				}
    		}
        }
	}

	@Override
	public void save() {
		if (config == null) return;
		config.set("disabled", playerTimedMessagesDisabled.stream().map(UUID::toString).collect(Collectors.toList()));
		ConfigurationSection messageSection = config.getConfigurationSection("periodicmessages");
		if (messageSection == null)
			messageSection = config.createSection("periodicmessages");
		for (PeriodicMessage message : messages.values()) {
			message.toConfigSection(messageSection);
		}
		try {
			File file = new File(plugin.getDataFolder(), fileName);
			config.save(file);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Error occurred while saving "+fileName+": " + e.getMessage());
		}
	}
}
