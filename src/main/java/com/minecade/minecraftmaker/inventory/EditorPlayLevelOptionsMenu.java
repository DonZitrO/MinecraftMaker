package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.EditorPlayLevelOptionItem;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class EditorPlayLevelOptionsMenu extends AbstractMakerMenu {

	private static EditorPlayLevelOptionsMenu instance;

	public static EditorPlayLevelOptionsMenu getInstance() {
		if (instance == null) {
			instance = new EditorPlayLevelOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private EditorPlayLevelOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage("menu.editor-play-options.title"), 9);
		init();
	}

	private void init() {
		int i = 0;
		for (EditorPlayLevelOptionItem item : EditorPlayLevelOptionItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public boolean onClick(MakerPlayer mPlayer, int slot) {
		if (!mPlayer.isPlayingLevel() && !mPlayer.hasClearedLevel()) {
			Bukkit.getLogger().warning(String.format("EditorPlayLevelOptionsMenu.onClick - This menu should be available to level editors while playing only! - clicked by: [%s]", mPlayer.getName()));
			return true;
		}
		if (slot >= items.length) {
			return true;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return true;
		}
		if (ItemUtils.itemNameEquals(clickedItem, EditorPlayLevelOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().exitPlaying();
			return true;
		} else if (ItemUtils.itemNameEquals(clickedItem, EditorPlayLevelOptionItem.EDIT.getDisplayName())) {
			mPlayer.getCurrentLevel().continueEditing();
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
