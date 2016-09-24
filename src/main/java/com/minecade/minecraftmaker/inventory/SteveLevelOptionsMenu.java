package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.CommonMenuItem;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.items.PlayLevelOptionItem;
import com.minecade.minecraftmaker.items.SteveLevelOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class SteveLevelOptionsMenu extends AbstractSharedMenu {

	private static SteveLevelOptionsMenu instance;

	public static SteveLevelOptionsMenu getInstance() {
		if (instance == null) {
			instance = new SteveLevelOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private SteveLevelOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 9);
		init();
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.steve-level-options.title";
	}

	private void init() {
		int i = 0;
		for (SteveLevelOptionItem item : SteveLevelOptionItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = CommonMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, slot, clickType);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		if (!mPlayer.isPlayingLevel() || !mPlayer.isInSteve()) {
			Bukkit.getLogger().warning(String.format("SteveLevelOptionsMenu.onClick - This menu should be available to steve level players only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(clickedItem, SteveLevelOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().finishSteveChallenge();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, SteveLevelOptionItem.LIKE.getDisplayName())) {
			plugin.getDatabaseAdapter().likeLevelAsync(mPlayer.getCurrentLevel().getLevelId(), mPlayer.getUniqueId(), false);
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, SteveLevelOptionItem.DISLIKE.getDisplayName())) {
			plugin.getDatabaseAdapter().likeLevelAsync(mPlayer.getCurrentLevel().getLevelId(), mPlayer.getUniqueId(), true);
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, SteveLevelOptionItem.SKIP.getDisplayName())) {
			mPlayer.getCurrentLevel().skipSteveLevel();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, PlayLevelOptionItem.RESTART.getDisplayName())) {
			mPlayer.getCurrentLevel().restartPlaying();
			return MenuClickResult.CANCEL_CLOSE;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

}
