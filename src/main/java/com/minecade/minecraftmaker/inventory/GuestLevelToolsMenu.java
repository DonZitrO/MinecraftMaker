package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class GuestLevelToolsMenu extends AbstractSharedMenu {

	private static GuestLevelToolsMenu instance;

	private GuestLevelToolsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 45);
	}

	public static GuestLevelToolsMenu getInstance() {
		if (instance == null) {
			instance = new GuestLevelToolsMenu(MinecraftMakerPlugin.getInstance());
			instance.init();
		}
		return instance;
	}

	private void init() {
		loadGlassPanes(items);
		items[22] = LevelToolsItem.SKULL.getItem();
		items[44] = GeneralMenuItem.EXIT_MENU.getItem();
		inventory.setContents(items);
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.guest-level-tools.title";
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);

		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("GuestLevelToolsMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		

		ItemStack itemStack = inventory.getItem(slot);

		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openLevelToolsMenu();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.SKULL.getDisplayName())) {
			mPlayer.openToolsSkullTypeMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}

		return MenuClickResult.CANCEL_UPDATE;
	}

}
