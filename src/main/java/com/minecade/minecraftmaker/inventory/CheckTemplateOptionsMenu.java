package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.CommonMenuItem;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.items.CheckTemplateOptionItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class CheckTemplateOptionsMenu extends AbstractSharedMenu {

	private static CheckTemplateOptionsMenu instance;

	public static CheckTemplateOptionsMenu getInstance() {
		if (instance == null) {
			instance = new CheckTemplateOptionsMenu(MinecraftMakerPlugin.getInstance());
		}
		return instance;
	}

	private CheckTemplateOptionsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 9);
		init();
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.check-template-options.title";
	}

	private void init() {
		int i = 0;
		for (CheckTemplateOptionItem item : CheckTemplateOptionItem.values()) {
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
		if (!mPlayer.isCheckingTemplate()) {
			Bukkit.getLogger().warning(String.format("CheckTemplateOptionsMenu.onClick - This menu should be available to template checkers only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(clickedItem, CheckTemplateOptionItem.EXIT.getDisplayName())) {
			mPlayer.getCurrentLevel().exitChecking();
		}
		return MenuClickResult.CANCEL_CLOSE;
	}

}
