package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.WeatherItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelWeatherMenu extends AbstractSharedMenu {

	private static LevelWeatherMenu instance;

	private LevelWeatherMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 45);
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-weather.title";
	}

	public static LevelWeatherMenu getInstance() {
		if (instance == null) {
			instance = new LevelWeatherMenu(MinecraftMakerPlugin.getInstance());
			instance.init();
		}
		return instance;
	}

	private void init() {
		loadGlassPanes(items);
		items[21] = WeatherItem.CLEAR.getItem();
		items[23] = WeatherItem.RAINY.getItem();
		items[44] = LevelToolsItem.EXIT.getItem();
		inventory.setContents(items);
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("LevelWeatherMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		
		ItemStack itemStack = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openLevelToolsMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}
		WeatherItem weatherItem = WeatherItem.getWeatherItem(itemStack.getType());
		if (weatherItem == null) {
			Bukkit.getLogger().warning(String.format("LevelWeatherMenu.onClick - No weather item for type: [%s]", itemStack.getType()));
			return MenuClickResult.CANCEL_UPDATE;
		}
		switch (weatherItem) {
		case RAINY:
			mPlayer.getCurrentLevel().requestTimeAndWeatherChange(mPlayer.getCurrentLevel().getTimeAndWeather().toRainy());
			break;
		case CLEAR:
			mPlayer.getCurrentLevel().requestTimeAndWeatherChange(mPlayer.getCurrentLevel().getTimeAndWeather().toClear());
			break;
		}
		return MenuClickResult.CANCEL_CLOSE;
	}

}
