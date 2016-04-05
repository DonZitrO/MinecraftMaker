package com.minecade.minecraftmaker.inventory;

import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelOptionsMenu extends AbstractMakerMenu {

	private static LevelOptionsMenu instance;

	public static LevelOptionsMenu getInstance() {
		if (instance == null) {
			instance = new LevelOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private LevelOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage("menu.level-options.title"), 9);
		init();
	}

	private void init() {
		int i = 0;
		for (LevelOptionItem item : LevelOptionItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public void onClick(MakerPlayer mPlayer, int slot) {
		if (slot >= items.length) {
			return;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return;
		}
		// TODO: wire up each option
	}

	@Override
	public void update() {

	}

	@Override
	public boolean isShared() {
		return true;
	}

}
