package com.minecade.minecraftmaker.inventory;

import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelSortByItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelSortByMenu extends AbstractMakerMenu {

	private static LevelSortByMenu instance;

	public static LevelSortByMenu getInstance() {
		if (instance == null) {
			instance = new LevelSortByMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private LevelSortByMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage("menu.level-sortby.title"), 9);
		init();
	}

	private void init() {
		int i = 0;
		for (LevelSortByItem item : LevelSortByItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public boolean onClick(MakerPlayer mPlayer, int slot) {
		if (slot >= items.length) {
			return true;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return true;
		}
		if (ItemUtils.itemNameEquals(clickedItem, LevelSortByItem.LIKES.getDisplayName())) {
			mPlayer.openLevelBrowserMenu(plugin, LevelSortBy.LIKES, false);
			return true;
//		} else if (ItemUtils.itemNameEquals(clickedItem, LevelSortByItem.OWNED.getDisplayName())) {
//			mPlayer.openLevelBrowserMenu(plugin, null, false);
//			return true;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.EXIT_MENU.getDisplayName())) {
			mPlayer.openLevelBrowserMenu(plugin, null, false);
			return true;
		}
		return true;
	}

	@Override
	public void update() {

	}

	@Override
	public boolean isShared() {
		return true;
	}

}
