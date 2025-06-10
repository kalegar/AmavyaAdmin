package me.sanjy33.amavyaadmin.inventory;

import java.util.List;
import java.util.Optional;

import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Inventory {
	
	private ItemStack[] contents;
	private int level;
	private double exp;
	private Optional<GameMode> gameMode = Optional.empty();

	
	public Inventory(PlayerInventory inv) {
		this.contents = cloneItemStackArray(inv.getContents());
	}

	public Inventory(Player player) {
		this(player.getInventory());
		this.level = player.getLevel();
		this.exp = player.getExp();
		this.gameMode = Optional.of(player.getGameMode());
	}
	
	private Inventory() {
		
	}

	private ItemStack[] cloneItemStackArray(ItemStack[] array) {
		ItemStack[] result = new ItemStack[array.length];
		for (int i = 0; i < array.length; i++) {
			if (array[i] != null)
				result[i] = new ItemStack(array[i]);
			else
				result[i] = null;
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static Inventory load(ConfigurationSection section) {
		Inventory inv = new Inventory();
		try {
			inv.contents = ((List<ItemStack>) section.get("contents")).toArray(new ItemStack[0]);
			inv.level = section.getInt("level");
			inv.exp = section.getDouble("exp");
			if (section.contains("gamemode")) {
				try {
					inv.gameMode = Optional.of(GameMode.valueOf(section.getString("gamemode")));
				} catch (Exception e) {
					inv.gameMode = Optional.empty();
				}
			} else {
				inv.gameMode = Optional.empty();
			}
			return inv;
		} catch (Exception e) {
			return null;
		}
	}

	public void setPlayerInventory(Player player, boolean setExperience) {
		setPlayerInventory(player, setExperience, false);
	}
	
	public void setPlayerInventory(Player player, boolean setExperience, boolean setGamemode) {
		PlayerInventory inv = player.getInventory();
		if (setGamemode)
        	gameMode.ifPresent(player::setGameMode);
		if (contents != null)
			inv.setContents(contents);
		if (setExperience) {
			player.setLevel(level);
			player.setExp((float) exp);
		}
	}

	public ItemStack[] getContents() {
		return contents;
	}

	public void setContents(ItemStack[] contents) {
		this.contents = contents;
	}
	
	public void save(ConfigurationSection section) {
		if (section == null) return;
		section.set("contents", contents);
		section.set("level", level);
		section.set("exp", exp);
        gameMode.ifPresent(mode -> section.set("gamemode", mode.name()));
	}

}
