package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.TimeItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelTimeMenu extends AbstractSharedMenu {

	private static LevelTimeMenu instance;

	public static LevelTimeMenu getInstance() {
		if (instance == null) {
			instance = new LevelTimeMenu(MinecraftMakerPlugin.getInstance());
			instance.init();
		}
		return instance;
	}

	private LevelTimeMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 45);
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-time.title";
	}

	private void init() {
		loadGlassPanes(items);
		items[21] = TimeItem.NOON.getItem();
		items[23] = TimeItem.MIDNIGHT.getItem();
		items[44] = LevelToolsItem.EXIT.getItem();
		inventory.setContents(items);
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isAuthorEditingLevel()) {
			Bukkit.getLogger().warning(String.format("LevelTimeMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		
		ItemStack itemStack = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openLevelToolsMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}
		TimeItem timeItem = TimeItem.getTimeItemByDisplayName(itemStack.getItemMeta().getDisplayName());
		if (timeItem == null) {
			Bukkit.getLogger().warning(String.format("LevelTimeMenu.onClick - No time item for display name: [%s]", itemStack.getItemMeta().getDisplayName()));
			return MenuClickResult.CANCEL_UPDATE;
		}
		switch (timeItem) {
		case NOON:
			mPlayer.getCurrentLevel().requestTimeAndWeatherChange(mPlayer.getCurrentLevel().getTimeAndWeather().toNoon());
			break;
		case MIDNIGHT:
			mPlayer.getCurrentLevel().requestTimeAndWeatherChange(mPlayer.getCurrentLevel().getTimeAndWeather().toMidnight());
			break;
		}
		return MenuClickResult.CANCEL_CLOSE;
	}

}
