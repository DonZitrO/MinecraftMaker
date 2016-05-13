package com.minecade.minecraftmaker.inventory;

import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelTemplateItem;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelTemplateMenu extends AbstractMakerMenu {

	private static LevelTemplateMenu instance;

	public static LevelTemplateMenu getInstance() {
		if (instance == null) {
			instance = new LevelTemplateMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private LevelTemplateMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage("menu.level-template.title"), 9);
		init();
	}

	private void init() {
		int i = 0;
		for (LevelTemplateItem item : LevelTemplateItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		if (slot >= items.length) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		// TODO: enhance this to allow better templates for empty levels
		if (slot < LevelTemplateItem.values().length) {
			plugin.getController().createEmptyLevel(mPlayer, MakerPlayableLevel.DEFAULT_LEVEL_WIDTH_CHUNKS, slot);
			return MenuClickResult.CANCEL_CLOSE;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

	@Override
	public void update() {

	}

	@Override
	public boolean isShared() {
		return true;
	}

}
