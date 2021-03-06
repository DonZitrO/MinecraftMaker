package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.CommonMenuItem;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.items.EditorPlayLevelOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class EditorPlayLevelOptionsMenu extends AbstractSharedMenu {

	private static EditorPlayLevelOptionsMenu instance;

	public static EditorPlayLevelOptionsMenu getInstance() {
		if (instance == null) {
			instance = new EditorPlayLevelOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private EditorPlayLevelOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 9);
		init();
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.editor-play-options.title";
	}

	private void init() {
		int i = 0;
		for (EditorPlayLevelOptionItem item : EditorPlayLevelOptionItem.values()) {
			items[i] = item.getItem();
			i++;
		}
		items[8] = CommonMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, Inventory clickedInventory, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, clickedInventory, slot, clickType);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		if (!mPlayer.isPlayingLevel() && !mPlayer.hasClearedLevel()) {
			Bukkit.getLogger().warning(String.format("EditorPlayLevelOptionsMenu.onClick - This menu should be available to level editors while playing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(clickedItem, EditorPlayLevelOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().exitPlaying();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, EditorPlayLevelOptionItem.EDIT.getDisplayName())) {
			mPlayer.getCurrentLevel().continueEditing();
			return MenuClickResult.CANCEL_CLOSE;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

}
