package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.data.MakerUnlockable;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.TimeItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelTimeMenu extends AbstractMakerMenu {

	private static Map<UUID, LevelTimeMenu> userMenuMap = new HashMap<>();

	public static LevelTimeMenu getInstance(MinecraftMakerPlugin plugin, UUID viewerId) {
		checkNotNull(plugin);
		checkNotNull(viewerId);
		LevelTimeMenu menu = userMenuMap.get(viewerId);
		if (menu == null) {
			menu = new LevelTimeMenu(plugin, viewerId);
		}
		userMenuMap.put(viewerId, menu);
		return menu;
	}

	private final UUID viewerId;

	private LevelTimeMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
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
		return "menu.level-time.title";
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	private void init() {
		loadGlassPanes(items);
		items[21] = TimeItem.NOON.getItem();
		items[23] = TimeItem.MIDNIGHT.getItem();
		items[44] = LevelToolsItem.EXIT.getItem();
		inventory.setContents(items);
	}

	@Override
	public boolean isShared() {
		return false;
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, slot, clickType);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isAuthorEditingLevel()) {
			Bukkit.getLogger().warning(String.format("LevelTimeMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
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
			if (mPlayer.hasUnlockable(MakerUnlockable.MIDNIGHT_LEVEL)) {
				mPlayer.getCurrentLevel().requestTimeAndWeatherChange(mPlayer.getCurrentLevel().getTimeAndWeather().toMidnight());
			} else {
				mPlayer.sendMessage("command.unlock.confirm1", MakerUnlockable.MIDNIGHT_LEVEL.getCost());
				mPlayer.sendMessage("command.unlock.confirm2", MakerUnlockable.MIDNIGHT_LEVEL.name().toLowerCase());
			}
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
		if (mPlayer.hasUnlockable(MakerUnlockable.MIDNIGHT_LEVEL)) {
			lore.set(3, plugin.getMessage("unlockable.unlocked"));
			lore.set(5, plugin.getMessage("unlockable.click-to-use"));
		} else {
			lore.set(3, plugin.getMessage("unlockable.cost", MakerUnlockable.MIDNIGHT_LEVEL.getCost()));
			lore.set(5, plugin.getMessage("unlockable.click-to-unlock"));
		}
		meta.setLore(lore);
		items[23].setItemMeta(meta);
	}

}
