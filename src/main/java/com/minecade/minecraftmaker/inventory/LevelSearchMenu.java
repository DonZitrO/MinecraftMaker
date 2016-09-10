package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelSearchMenu extends AbstractDisplayableLevelMenu {

	private static Map<UUID, LevelSearchMenu> userLevelSearchMenuMap = new HashMap<>();

	public static LevelSearchMenu getInstance(MinecraftMakerPlugin plugin, UUID viewerId) {
		checkNotNull(plugin);
		checkNotNull(viewerId);
		LevelSearchMenu menu = userLevelSearchMenuMap.get(viewerId);
		if (menu == null) {
			menu = new LevelSearchMenu(plugin, viewerId);
		}
		userLevelSearchMenuMap.put(viewerId, menu);
		return menu;
	}

	private final UUID viewerId;

	private String searchString;
	private int totalSearchResults;

	private LevelSearchMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin);
		this.viewerId = viewerId;
		init();
	}

	@Override
	public void disable() {
		super.disable();
		userLevelSearchMenuMap.remove(getViewerId());
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-search.title";
	}

	@Override
	protected int getTotalItemsCount() {
		return totalSearchResults;
	}

	public UUID getViewerId() {
		return this.viewerId;
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
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.onClick - clicked item material: [%s]", clickedItem.getType()));
		}
		if (clickedItem.getType().equals(Material.MONSTER_EGG)) {
			String serial = ItemUtils.getLoreLine(clickedItem, 1);
			if (StringUtils.isBlank(serial) || !StringUtils.isNumeric(serial)) {
				Bukkit.getLogger().severe(
						String.format("LevelBrowserMenu.onClick - unable to get level serial from lore: [%s]", serial));
				return MenuClickResult.CANCEL_UPDATE;
			}
			plugin.getController().loadLevelForPlayingBySerial(mPlayer, Long.valueOf(serial));
			return MenuClickResult.CANCEL_CLOSE;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

	public void reset() {
		currentPage = 1;
		totalSearchResults = 0;
		update(null);
	}

	@Override
	public void update() {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		update(null);
		plugin.getDatabaseAdapter().searchPublishedLevelsPageByNameAsync(getViewerId(), searchString, getPageOffset(currentPage), ITEMS_PER_PAGE);
	}

	public void update(String searchString, int totalResults, Collection<MakerDisplayableLevel> currentPageLevels) {
		this.searchString = searchString;
		this.totalSearchResults = totalResults;
		update(currentPageLevels);
	}

}
