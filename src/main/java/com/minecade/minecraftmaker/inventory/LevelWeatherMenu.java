package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.data.MakerUnlockable;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.WeatherItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelWeatherMenu extends AbstractMakerMenu {

	private static Map<UUID, LevelWeatherMenu> userMenuMap = new HashMap<>();

	public static LevelWeatherMenu getInstance(MinecraftMakerPlugin plugin, UUID viewerId) {
		checkNotNull(plugin);
		checkNotNull(viewerId);
		LevelWeatherMenu menu = userMenuMap.get(viewerId);
		if (menu == null) {
			menu = new LevelWeatherMenu(plugin, viewerId);
		}
		userMenuMap.put(viewerId, menu);
		return menu;
	}

	private final UUID viewerId;

	private LevelWeatherMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin, 45);
		this.viewerId = viewerId;
		init();
	}

	@Override
	public void disable() {
		super.disable();
		userMenuMap.remove(getViewerId());
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-weather.title";
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	private void init() {
		loadGlassPanes(items);
		items[21] = WeatherItem.CLEAR.getItem();
		items[23] = WeatherItem.RAINY.getItem();
		items[44] = LevelToolsItem.EXIT.getItem();
		inventory.setContents(items);
	}

	@Override
	public boolean isShared() {
		return false;
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, Inventory clickedInventory, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, clickedInventory, slot, clickType);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isAuthorEditingLevel()) {
			Bukkit.getLogger().warning(String.format("LevelWeatherMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		
		ItemStack itemStack = inventory.getItem(slot);
		if (itemStack == null) {
			return MenuClickResult.ALLOW;
		}
		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openConfigLevelMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}
		WeatherItem weatherItem = WeatherItem.getWeatherItem(itemStack.getType());
		if (weatherItem == null) {
			Bukkit.getLogger().warning(String.format("LevelWeatherMenu.onClick - No weather item for type: [%s]", itemStack.getType()));
			return MenuClickResult.CANCEL_UPDATE;
		}
		switch (weatherItem) {
		case RAINY:
			if (mPlayer.hasUnlockable(MakerUnlockable.RAINY_LEVEL)) {
				mPlayer.getCurrentLevel().requestTimeAndWeatherChange(mPlayer.getCurrentLevel().getTimeAndWeather().toRainy());
			} else {
				mPlayer.sendMessage("command.unlock.confirm1", MakerUnlockable.RAINY_LEVEL.getCost());
				mPlayer.sendMessage("command.unlock.confirm2", MakerUnlockable.RAINY_LEVEL.name().toLowerCase());
			}
			break;
		case CLEAR:
			mPlayer.getCurrentLevel().requestTimeAndWeatherChange(mPlayer.getCurrentLevel().getTimeAndWeather().toClear());
			break;
		}
		return MenuClickResult.CANCEL_CLOSE;
	}

	@Override
	public void update() {
		MakerPlayer mPlayer = plugin.getController().getPlayer(getViewerId());
		if (mPlayer == null) {
			return;
		}
		if (items[23] == null || !items[23].hasItemMeta() || !items[23].getItemMeta().hasLore()) {
			return;
		}
		ItemMeta meta = items[23].getItemMeta();
		List<String> lore = meta.getLore();
		while (lore.size() < 6) {
			lore.add("");
		}
		if (mPlayer.hasUnlockable(MakerUnlockable.RAINY_LEVEL)) {
			lore.set(3, plugin.getMessage("unlockable.unlocked"));
			lore.set(5, plugin.getMessage("unlockable.click-to-use"));
		} else {
			lore.set(3, plugin.getMessage("unlockable.cost", MakerUnlockable.RAINY_LEVEL.getCost()));
			lore.set(5, plugin.getMessage("unlockable.click-to-unlock"));
		}
		meta.setLore(lore);
		items[23].setItemMeta(meta);
	}

}
