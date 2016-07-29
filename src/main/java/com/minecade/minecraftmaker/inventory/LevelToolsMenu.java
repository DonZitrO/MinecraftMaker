package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelToolsMenu extends AbstractSharedMenu {

	private static LevelToolsMenu instance;

	private LevelToolsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 45);
	}

	public static LevelToolsMenu getInstance() {
		if (instance == null) {
			instance = new LevelToolsMenu(MinecraftMakerPlugin.getInstance());
			instance.init();
		}
		return instance;
	}

	private void init() {
		loadGlassPanes(items);
		items[20] = LevelToolsItem.WEATHER.getItem();
		items[22] = LevelToolsItem.TIME.getItem();
		items[24] = LevelToolsItem.SKULL.getItem();
		items[44] = GeneralMenuItem.EXIT_MENU.getItem();
		inventory.setContents(items);
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-tools.title";
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, slot, clickType);

		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("LevelToolsMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		

		ItemStack itemStack = inventory.getItem(slot);

		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openLevelToolsMenu();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.TIME.getDisplayName())) {
			if (mPlayer.isAuthorEditingLevel()) {
				mPlayer.openLevelTimeMenu();
				return MenuClickResult.CANCEL_CLOSE;
			} else {
				mPlayer.sendMessage("level.edit.error.author-only");
				return MenuClickResult.CANCEL_UPDATE;
			}
		} else if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.WEATHER.getDisplayName())) {
			if (mPlayer.isAuthorEditingLevel()) {
				mPlayer.openLevelWeatherMenu();
				return MenuClickResult.CANCEL_CLOSE;
			} else {
				mPlayer.sendMessage("level.edit.error.author-only");
				return MenuClickResult.CANCEL_UPDATE;
			}
		} else if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.SKULL.getDisplayName())) {
			mPlayer.openToolsSkullTypeMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}

		return MenuClickResult.CANCEL_UPDATE;
	}

}
