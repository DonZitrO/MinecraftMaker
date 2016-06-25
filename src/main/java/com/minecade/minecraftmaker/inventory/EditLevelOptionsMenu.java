package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.data.Rank;
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
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("EditLevelOptionsMenu.onClick - This menu should be available to level editors only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.SAVE.getDisplayName())) {
			mPlayer.getCurrentLevel().saveLevel();
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.PLAY.getDisplayName())) {
			mPlayer.getCurrentLevel().saveAndPlay();
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.PUBLISH.getDisplayName())) {
			if (!mPlayer.canPublishLevel()) {
				mPlayer.sendMessage(plugin, "level.publish.error.published-limit", mPlayer.getPublishedLevelsCount());
				mPlayer.sendMessage(plugin, "level.publish.error.published-limit.unpublish-delete");
				if (!mPlayer.hasRank(Rank.TITAN)) {
					mPlayer.sendMessage(plugin, "upgrade.rank.increase.limits.or");
					mPlayer.sendMessage(plugin, "upgrade.rank.published.limits");
				}
				return MenuClickResult.CANCEL_CLOSE;
			}
			mPlayer.getCurrentLevel().publishLevel();
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.TOOLS.getDisplayName())) {
			if(!mPlayer.hasRank(Rank.VIP)){
				mPlayer.sendMessage(plugin, "upgrade.rank.build.tools");
				return MenuClickResult.CANCEL_CLOSE;
			} else {
				mPlayer.updateInventory();
				mPlayer.openLevelToolsMenu();
			}
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().exitEditing();
		}
		return MenuClickResult.CANCEL_CLOSE;
	}

	@Override
	public void update() {

	}

	@Override
	public boolean isShared() {
		return true;
	}

}
