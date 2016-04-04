package com.minecade.minecraftmaker.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelTemplateItem;
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
		super(plugin, plugin.getMessage("menu.leveltemplate.title"), 9);
		init();
	}

	private void init() {
		int i = 0;
		for (LevelTemplateItem item : LevelTemplateItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = GeneralMenuItem.BACK.getItem();
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
		if (ItemUtils.itemNameEquals(clickedItem, LevelTemplateItem.EMPTY_FLOOR.getDisplayName())) {
			plugin.getController().createEmptyLevel(mPlayer, Material.AIR);
			return;
		}
	}

	@Override
	public void update() {

	}

	@Override
	public boolean isShared() {
		return true;
	}

}
