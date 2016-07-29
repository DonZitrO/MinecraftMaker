package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.SteveLevelClearOptionItem;
import com.minecade.minecraftmaker.items.SteveLevelOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class SteveLevelClearOptionsMenu extends AbstractSharedMenu {

	private static SteveLevelClearOptionsMenu instance;

	public static SteveLevelClearOptionsMenu getInstance() {
		if (instance == null) {
			instance = new SteveLevelClearOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private SteveLevelClearOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 9);
		init();
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.steve-level-clear-options.title";
	}

	private void init() {
		int i = 0;
		for (SteveLevelClearOptionItem item : SteveLevelClearOptionItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, slot, clickType);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		if (!mPlayer.hasClearedLevel() || !mPlayer.isInSteve()) {
			Bukkit.getLogger().warning(String.format("SteveLevelClearOptionsMenu.onClick - This menu should be available to steve challenge players after they clear a level only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(clickedItem, SteveLevelOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().finishSteveChallenge();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, SteveLevelClearOptionItem.LIKE.getDisplayName())) {
			plugin.getDatabaseAdapter().likeLevelAsync(mPlayer.getCurrentLevel().getLevelId(), mPlayer.getUniqueId(), false);
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, SteveLevelClearOptionItem.DISLIKE.getDisplayName())) {
			plugin.getDatabaseAdapter().likeLevelAsync(mPlayer.getCurrentLevel().getLevelId(), mPlayer.getUniqueId(), true);
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, SteveLevelClearOptionItem.CONTINUE.getDisplayName())) {
			mPlayer.getCurrentLevel().continueSteveChallenge();
			return MenuClickResult.CANCEL_CLOSE;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

}
