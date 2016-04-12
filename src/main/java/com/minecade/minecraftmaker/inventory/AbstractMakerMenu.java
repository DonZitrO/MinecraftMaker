package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.inventory.ActiveItemEnchantment;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractMakerMenu {

	protected final MinecraftMakerPlugin plugin;

	protected Inventory inventory;
	protected ItemStack[] items;
	protected AbstractMakerMenu back;

	public AbstractMakerMenu(MinecraftMakerPlugin plugin, String title, int size) {
		this.plugin = plugin;
		this.items = new ItemStack[size];
		if (title.length() > 32) {
			title = title.substring(0, 32);
		}
		inventory = Bukkit.createInventory(null, items.length, title);
		inventory.setContents(items);
	}

	public String getName() {
		return inventory.getName();
	}

	public String getTitle() {
		return inventory.getTitle();
	}

	protected void setActive(int slot) {
		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (null != item) {
				if (slot != i) {
					item.removeEnchantment(ActiveItemEnchantment.getActiveItemEnchantment());
				} else {
					item.addUnsafeEnchantment(ActiveItemEnchantment.getActiveItemEnchantment(), 10);
				}
			}
		}
	}

	protected void setInactive(int slot) {
		ItemStack item = items[slot];
		if (null != item) {
			item.removeEnchantment(ActiveItemEnchantment.getActiveItemEnchantment());
		}
	}

	protected void toggleActive(int slot) {
		ItemStack item = items[slot];
		if (null != item) {
			if (item.containsEnchantment(ActiveItemEnchantment.getActiveItemEnchantment())) {
				item.removeEnchantment(ActiveItemEnchantment.getActiveItemEnchantment());
			} else {
				item.addEnchantment(ActiveItemEnchantment.getActiveItemEnchantment(), 10);
			}
		}
	}

	public abstract void onClick(MakerPlayer mPlayer, int slot);

	public void open(Player player) {
		if (player == null) {
			return;
		}
		inventory.setContents(items);
		player.openInventory(inventory);
	}

	public abstract void update();

	public abstract boolean isShared();

	protected void updateItems() {
		inventory.setContents(items);
	}

	public void destroy() {
		if (isShared()) {
			return;
		}
		if (inventory != null) {
			inventory.clear();
		}
		inventory = null;
		items = null;
		back = null;
	}

}
