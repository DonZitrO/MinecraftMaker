package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.collect.Iterators;
import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelBrowserMenu extends AbstractDisplayableLevelMenu {

	private static Map<UUID, LevelBrowserMenu> userLevelBrowserMenuMap = new HashMap<>();
	private static int levelCount = 0;

	public static LevelBrowserMenu getInstance(MinecraftMakerPlugin plugin, UUID viewerId) {
		checkNotNull(plugin);
		checkNotNull(viewerId);
		LevelBrowserMenu menu = userLevelBrowserMenuMap.get(viewerId);
		if (menu == null) {
			menu = new LevelBrowserMenu(plugin, viewerId);
		}
		userLevelBrowserMenuMap.put(viewerId, menu);
		return menu;
	}

	public static void updateLevelCount(int levelCount) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		if (levelCount != LevelBrowserMenu.levelCount) {
			LevelBrowserMenu.levelCount = levelCount;
		}
	}

	public static void updatePlayerMenu(UUID playerId, Set<MakerDisplayableLevel> levels) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		LevelBrowserMenu menu = userLevelBrowserMenuMap.get(playerId);
		if (menu != null) {
			menu.update(levels);
		}
	}

	private final UUID viewerId;

	private final Iterator<LevelSortBy> cycleSortBy;
	//private final List<MakerDisplayableLevel> currentPageLevels = new LinkedList<>();

	private LevelSortBy sortBy;
	private boolean reverseSortBy;

	private boolean busy;

	private LevelBrowserMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin);
		this.cycleSortBy = Iterators.cycle(LevelSortBy.values());
		this.sortBy = cycleSortBy.next();
		this.reverseSortBy = sortBy.isReversedDefault(); 
		this.viewerId = viewerId;
		init();
	}

	@Override
	public void disable() {
		super.disable();
		userLevelBrowserMenuMap.remove(getViewerId());
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-browser.title";
	}

	@Override
	protected int getTotalItemsCount() {
		return levelCount;
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	@Override
	protected void init() {
		super.init();
		items[2] = GeneralMenuItem.SEARCH.getItem();
		items[6] = GeneralMenuItem.SORT.getItem();
		updateSortItems();
	}

	public boolean isBusy() {
		return busy;
	}

	@Override
	public boolean isShared() {
		return false;
	}

//	public void sortBy(LevelSortBy sortBy) {
//		checkNotNull(sortBy);
//		if (sortBy.equals(this.sortBy)) {
//			return;
//		}
//		this.sortBy = sortBy;
//		reverseSortBy = false;
//		update();
//	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		if (isBusy()) {
			return MenuClickResult.CANCEL_UPDATE;
		}
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
				Bukkit.getLogger().severe(String.format("LevelBrowserMenu.onClick - unable to get level serial from lore: [%s]", serial));
				return MenuClickResult.CANCEL_UPDATE;
			}
			plugin.getController().loadLevelForPlayingBySerial(mPlayer, Long.valueOf(serial));
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.SEARCH.getDisplayName())) {
			mPlayer.sendMessage("command.level.search.usage");
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.SORT.getDisplayName())) {
			sortByNext();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.SORT_DIRECTION_UP.getDisplayName())) {
			// both items have the same display name
			if (reverseSortBy) {
				sortAscending();
			} else {
				sortDescending();
			}
			return MenuClickResult.CANCEL_UPDATE;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

	private void sortAscending() {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.sortAscending - reversed: [%s]", reverseSortBy));
		}
		reverseSortBy = false;
		currentPage = 1;
		update();
	}

	private void sortByNext() {
		sortBy = cycleSortBy.next();
		reverseSortBy = sortBy.isReversedDefault();
		currentPage = 1;
		update();
	}

	private void sortDescending() {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.sortDescending - reversed: [%s]", reverseSortBy));
		}
		currentPage = 1;
		reverseSortBy = true;
		update();
	}

	@Override
	public void update() {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.update - viewer: [%s]", getViewerId()));
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.update - offset: [%s] - limit: [%s]", getPageOffset(currentPage), LEVELS_PER_PAGE));
		}
		update(null);
		plugin.getController().requestLevelPageUpdate(sortBy, reverseSortBy, currentPage, getViewerId());
	}

	@Override
	public void update(Collection<MakerDisplayableLevel> currentPageLevels) {
		updateSortItems();
		super.update(currentPageLevels);
	}

	private void updateSortDirectionItem() {
		items[7] = this.sortBy.isReversible() ? this.reverseSortBy ? GeneralMenuItem.SORT_DIRECTION_DOWN.getItem() : GeneralMenuItem.SORT_DIRECTION_UP.getItem() : getGlassPane();
	}

	private void updateSortItem() {
		if (items[6] == null) {
			items[6] = GeneralMenuItem.SORT.getItem();
		}

		ItemMeta sortMeta = items[6].getItemMeta();
		List<String> lore = sortMeta.getLore();
		if (lore == null) {
			lore = new ArrayList<>();
		}
		lore.clear();
		lore.add(StringUtils.EMPTY);
		for (LevelSortBy sortBy:LevelSortBy.values()) {
			if (sortBy.equals(this.sortBy)) {
				lore.add(String.format("§E%s", sortBy.getDisplayName()));
			} else {
				lore.add(String.format("§F%s", sortBy.getDisplayName()));
			}
		}
		sortMeta.setLore(lore);
		items[6].setItemMeta(sortMeta);
	}

	private void updateSortItems() {
		updateSortItem();
		updateSortDirectionItem();
	}

}
