package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
	private static int levelCount;

	public static int getLevelsPerPage() {
		return LEVELS_PER_PAGE;
	}

	public static int getPageOffset(int page) {
		return Math.max(0, (page - 1) * LEVELS_PER_PAGE);
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-browser.title";
	}

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

	private final Iterator<LevelSortBy> cycleSortBy;
	private int currentPage = 1;
	private LevelSortBy sortBy;
	private boolean reverseSortBy;
	private final UUID viewerId;
	private final List<MakerDisplayableLevel> currentPageLevels = new LinkedList<>();
	private boolean busy;

	private LevelBrowserMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin, 54);
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

	private long getTotalPages() {
		return Math.max(1, (levelCount + LEVELS_PER_PAGE - 1) / LEVELS_PER_PAGE);
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	private void init() {
		for (int i = 0; i < inventory.getSize(); i++) {
			items[i] = getGlassPane();
		}
		items[2] = GeneralMenuItem.SEARCH.getItem();
		items[6] = GeneralMenuItem.SORT.getItem();
		updateSortDirectionItem();
		items[9] = GeneralMenuItem.PREVIOUS_PAGE.getItem();
		items[18] = GeneralMenuItem.PREVIOUS_10TH_PAGE.getItem();
		items[27] = GeneralMenuItem.PREVIOUS_100TH_PAGE.getItem();
		items[36] = GeneralMenuItem.PREVIOUS_1000TH_PAGE.getItem();
		items[17] = GeneralMenuItem.NEXT_PAGE.getItem();
		items[26] = GeneralMenuItem.NEXT_10TH_PAGE.getItem();
		items[35] = GeneralMenuItem.NEXT_100TH_PAGE.getItem();
		items[44] = GeneralMenuItem.NEXT_1000TH_PAGE.getItem();
		items[47] = GeneralMenuItem.CURRENT_PAGE.getItem();
		items[51] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public boolean isShared() {
		return false;
	}

	private void nextPage() {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.nextPage - current page: [%s] - total pages: [%s] - level count: [%s]", currentPage, getTotalPages(), levelCount));
		}
		if (currentPage < getTotalPages()) {
			currentPage++;
		}
		update();
	}

	private void next10thPage() {
		if (currentPage + 10 < getTotalPages()) {
			currentPage += 10;
		}
		update();
	}

	private void next100thPage() {
		if (currentPage + 100 < getTotalPages()) {
			currentPage += 100;
		}
		update();
	}

	private void next1000thPage() {
		if (currentPage + 1000 < getTotalPages()) {
			currentPage += 1000;
		}
		update();
	}

	public boolean isBusy() {
		return busy;
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		if (isBusy()) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		MenuClickResult result = super.onClick(mPlayer, slot);
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
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.NEXT_PAGE.getDisplayName())) {
			nextPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.NEXT_10TH_PAGE.getDisplayName())) {
			next10thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.NEXT_100TH_PAGE.getDisplayName())) {
			next100thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.NEXT_1000TH_PAGE.getDisplayName())) {
			next1000thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.PREVIOUS_PAGE.getDisplayName())) {
			previousPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.PREVIOUS_10TH_PAGE.getDisplayName())) {
			previous10thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.PREVIOUS_100TH_PAGE.getDisplayName())) {
			previous100thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.PREVIOUS_1000TH_PAGE.getDisplayName())) {
			previous1000thPage();
			return MenuClickResult.CANCEL_UPDATE;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

	private void previousPage() {
		if (currentPage > 1) {
			currentPage--;
			update();
		}
	}

	private void previous10thPage() {
		if (currentPage > 10) {
			currentPage -= 10;
			update();
		}
	}

	private void previous100thPage() {
		if (currentPage > 100) {
			currentPage -= 100;
			update();
		}
	}

	private void previous1000thPage() {
		if (currentPage > 1000) {
			currentPage -= 1000;
			update();
		}
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
		}

		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.update - current page size: [%s] - offset: [%s] - limit: [%s]", currentPageLevels.size(), getPageOffset(currentPage), LEVELS_PER_PAGE));
		}

		updatePaginationItems();
		currentPageLevels.clear();
		super.update(currentPageLevels);
		plugin.getController().requestLevelPageUpdate(sortBy, reverseSortBy, currentPage, getViewerId());
	}

	@Override
	public void update(Collection<MakerDisplayableLevel> currentPageLevels) {
		updatePaginationItems();
		this.currentPageLevels.clear();
		this.currentPageLevels.addAll(currentPageLevels);
		super.update(currentPageLevels);
	}

	private void updateCurrentPageItem() {
		if (items[47] == null) {
			items[47] = GeneralMenuItem.CURRENT_PAGE.getItem();
		}

		ItemMeta currentPageMeta = items[47].getItemMeta();
		currentPageMeta.setDisplayName(String.format("%s %s/%s", GeneralMenuItem.CURRENT_PAGE.getDisplayName(), currentPage, getTotalPages()));
		items[47].setItemMeta(currentPageMeta);
	}

	private void updateNextPageItems() {
		if (currentPage == getTotalPages()) {
			items[17] = getGlassPane();
		} else {
			items[17] = GeneralMenuItem.NEXT_PAGE.getItem();
			ItemMeta nextPageMeta = items[17].getItemMeta();
			nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 1)));
			items[17].setItemMeta(nextPageMeta);
		}
		if (currentPage + 10 > getTotalPages()) {
			items[26] = getGlassPane();
		} else {
			items[26] = GeneralMenuItem.NEXT_10TH_PAGE.getItem();
			ItemMeta nextPageMeta = items[26].getItemMeta();
			nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 10)));
			items[26].setItemMeta(nextPageMeta);
		}
		if (currentPage + 100 > getTotalPages()) {
			items[35] = getGlassPane();
		} else {
			items[35] = GeneralMenuItem.NEXT_100TH_PAGE.getItem();
			ItemMeta nextPageMeta = items[35].getItemMeta();
			nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 100)));
			items[35].setItemMeta(nextPageMeta);
		}
		if (currentPage + 1000 > getTotalPages()) {
			items[44] = getGlassPane();
		} else {
			items[44] = GeneralMenuItem.NEXT_1000TH_PAGE.getItem();
			ItemMeta nextPageMeta = items[44].getItemMeta();
			nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 1000)));
			items[44].setItemMeta(nextPageMeta);
		}
	}

	private void updatePaginationItems() {
		updateCurrentPageItem();
		updatePreviousPageItems();
		updateNextPageItems();
		updateSortItem();
		updateSortDirectionItem();
	}

	private void updatePreviousPageItems() {
		if (currentPage == 1) {
			items[9] = getGlassPane();
		} else {
			items[9] = GeneralMenuItem.PREVIOUS_PAGE.getItem();
			ItemMeta previousPageMeta = items[9].getItemMeta();
			previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 1)));
			items[9].setItemMeta(previousPageMeta);
		}
		if (currentPage <= 10) {
			items[18] = getGlassPane();
		} else {
			items[18] = GeneralMenuItem.PREVIOUS_10TH_PAGE.getItem();
			ItemMeta previousPageMeta = items[18].getItemMeta();
			previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 10)));
			items[18].setItemMeta(previousPageMeta);
		}
		if (currentPage <= 100) {
			items[27] = getGlassPane();
		} else {
			items[27] = GeneralMenuItem.PREVIOUS_100TH_PAGE.getItem();
			ItemMeta previousPageMeta = items[27].getItemMeta();
			previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 100)));
			items[27].setItemMeta(previousPageMeta);
		}
		if (currentPage <= 1000) {
			items[36] = getGlassPane();
		} else {
			items[36] = GeneralMenuItem.PREVIOUS_1000TH_PAGE.getItem();
			ItemMeta previousPageMeta = items[36].getItemMeta();
			previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 1000)));
			items[36].setItemMeta(previousPageMeta);
		}
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

}
