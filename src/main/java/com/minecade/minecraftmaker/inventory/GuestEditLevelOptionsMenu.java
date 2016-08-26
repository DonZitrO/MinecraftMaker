package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.data.Rank;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.GuestEditLevelOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class GuestEditLevelOptionsMenu extends AbstractSharedMenu {

	private static GuestEditLevelOptionsMenu instance;

	public static GuestEditLevelOptionsMenu getInstance() {
		if (instance == null) {
			instance = new GuestEditLevelOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private GuestEditLevelOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 9);
		init();
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.guest-edit-level-options.title";
	}

	private void init() {
		int i = 0;
		for (GuestEditLevelOptionItem item : GuestEditLevelOptionItem.values()) {
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
		if (!mPlayer.isGuestEditingLevel()) {
			Bukkit.getLogger().warning(String.format("GuestEditLevelOptionsMenu.onClick - This menu should be available to level editors only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(clickedItem, GuestEditLevelOptionItem.TOOLS.getDisplayName())) {
			if(!mPlayer.hasRank(Rank.VIP)){
				mPlayer.sendMessage("upgrade.rank.build.tools");
				return MenuClickResult.CANCEL_CLOSE;
			} else {
				mPlayer.updateInventory();
				mPlayer.openVIPLevelToolsMenu();
			}
		} else if (ItemUtils.itemNameEquals(clickedItem, GuestEditLevelOptionItem.EXIT.getDisplayName())) {
			plugin.getController().addPlayerToMainLobby(mPlayer);
		}
		return MenuClickResult.CANCEL_CLOSE;
	}

}
