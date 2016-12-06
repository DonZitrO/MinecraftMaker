package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.data.Rank;
import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.CommonMenuItem;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.items.EditLevelOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class EditLevelOptionsMenu extends AbstractSharedMenu {

	private static EditLevelOptionsMenu instance;

	public static EditLevelOptionsMenu getInstance() {
		if (instance == null) {
			instance = new EditLevelOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private EditLevelOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 9);
		init();
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.edit-level-options.title";
	}

	private void init() {
		int i = 0;
		for (EditLevelOptionItem item : EditLevelOptionItem.values()) {
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
		if (!mPlayer.isAuthorEditingLevel()) {
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
				mPlayer.sendMessage("level.publish.error.published-limit", mPlayer.getPublishedLevelsCount());
				mPlayer.sendMessage("level.publish.error.published-limit.unpublish-delete");
				if (!mPlayer.hasRank(Rank.TITAN)) {
					mPlayer.sendMessage("upgrade.rank.increase.limits.or");
					mPlayer.sendMessage("upgrade.rank.published.limits");
				}
				return MenuClickResult.CANCEL_CLOSE;
			}
			mPlayer.getCurrentLevel().publishLevel();
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.TOOLS.getDisplayName())) {
			if(!mPlayer.hasRank(Rank.VIP)){
				mPlayer.sendMessage("upgrade.rank.build.tools");
			} else {
				mPlayer.openVIPLevelToolsMenu();
			}
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.CONFIG.getDisplayName())) {
			mPlayer.openConfigLevelMenu();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.INVITE.getDisplayName())) {
			mPlayer.sendMessage("command.maker.invite.usage");
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, EditLevelOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().exitEditing();
		}
		return MenuClickResult.CANCEL_CLOSE;
	}

}
