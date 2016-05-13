package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.PlayLevelOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class PlayLevelOptionsMenu extends AbstractMakerMenu {

	private static PlayLevelOptionsMenu instance;

	public static PlayLevelOptionsMenu getInstance() {
		if (instance == null) {
			instance = new PlayLevelOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private PlayLevelOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage("menu.play-level-options.title"), 9);
		init();
	}

	private void init() {
		int i = 0;
		for (PlayLevelOptionItem item : PlayLevelOptionItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		if (!mPlayer.isPlayingLevel() && !mPlayer.hasClearedLevel()) {
			Bukkit.getLogger().warning(String.format("PlayLevelOptionsMenu.onClick - This menu should be available to level players only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		if (slot >= items.length) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		if (ItemUtils.itemNameEquals(clickedItem, PlayLevelOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().exitPlaying();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, PlayLevelOptionItem.LIKE.getDisplayName())) {
			plugin.getDatabaseAdapter().likeLevelAsync(mPlayer.getCurrentLevel().getLevelId(), mPlayer.getUniqueId(), false);
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, PlayLevelOptionItem.DISLIKE.getDisplayName())) {
			plugin.getDatabaseAdapter().likeLevelAsync(mPlayer.getCurrentLevel().getLevelId(), mPlayer.getUniqueId(), true);
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, PlayLevelOptionItem.RESTART.getDisplayName())) {
			mPlayer.getCurrentLevel().restartPlaying();
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
