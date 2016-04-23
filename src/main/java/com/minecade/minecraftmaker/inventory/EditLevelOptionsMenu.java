package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.EditLevelOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class EditLevelOptionsMenu extends AbstractMakerMenu {

	private static EditLevelOptionsMenu instance;

	public static EditLevelOptionsMenu getInstance() {
		if (instance == null) {
			instance = new EditLevelOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private EditLevelOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage("menu.edit-level-options.title"), 9);
		init();
	}

	private void init() {
		int i = 0;
		for (EditLevelOptionItem item : EditLevelOptionItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public boolean onClick(MakerPlayer mPlayer, int slot) {
		if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("EditLevelOptionsMenu.onClick - This menu should be available to level editors only! - clicked by: [%s]", mPlayer.getName()));
			return true;
		}
		if (slot >= items.length) {
			return true;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return true;
		}
		if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.SAVE.getDisplayName())) {
			mPlayer.getCurrentLevel().saveLevel();
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.PLAY.getDisplayName())) {
			mPlayer.getCurrentLevel().saveAndPlay();
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.PUBLISH.getDisplayName())) {
			mPlayer.getCurrentLevel().publishLevel();
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().exitEditing();
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
