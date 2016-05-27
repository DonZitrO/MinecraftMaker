package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.collect.Iterators;
import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelBrowserMenu extends AbstractMakerMenu {

	private static final int LEVELS_PER_PAGE = 28;

	private static Map<UUID, ItemStack> levelItems = new HashMap<>();
	private static Map<UUID, MakerDisplayableLevel> byIdMap = new HashMap<>();
	private static Map<Long, MakerDisplayableLevel> bySerialMap = new HashMap<>();
	private static Map<UUID, LevelBrowserMenu> userLevelBrowserMenuMap = new HashMap<>();

	private static TreeSet<MakerDisplayableLevel> levelsByAuthorName = new TreeSet<MakerDisplayableLevel>((MakerDisplayableLevel l1, MakerDisplayableLevel l2) -> compareAuthorNameAndSerial(l1, l2));
	private static TreeSet<MakerDisplayableLevel> levelsByDislikes = new TreeSet<MakerDisplayableLevel>((MakerDisplayableLevel l1, MakerDisplayableLevel l2) -> compareDislikesAndSerial(l1, l2));
	private static TreeSet<MakerDisplayableLevel> levelsByLikes = new TreeSet<MakerDisplayableLevel>((MakerDisplayableLevel l1, MakerDisplayableLevel l2) -> compareLikesAndSerial(l1, l2));
	private static TreeSet<MakerDisplayableLevel> levelsByName = new TreeSet<MakerDisplayableLevel>((MakerDisplayableLevel l1, MakerDisplayableLevel l2) -> compareNamesAndSerial(l1, l2));
	private static TreeSet<MakerDisplayableLevel> levelsByPublishDate = new TreeSet<MakerDisplayableLevel>((MakerDisplayableLevel l1, MakerDisplayableLevel l2) -> comparePublishDatesAndSerial(l1, l2));
	private static TreeSet<MakerDisplayableLevel> levelsByRank = new TreeSet<MakerDisplayableLevel>((MakerDisplayableLevel l1, MakerDisplayableLevel l2) -> compareRanksAndSerial(l1, l2));
	private static TreeSet<MakerDisplayableLevel> levelsBySerial = new TreeSet<MakerDisplayableLevel>((MakerDisplayableLevel l1, MakerDisplayableLevel l2) -> Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial())));

	// TODO: https://bukkit.org/threads/class-anvilgui-use-the-anvil-gui-to-retrieve-strings.211849/
	//private static Inventory searchInventory = Bukkit.createInventory(null, InventoryType.ANVIL, MinecraftMakerPlugin.getInstance().getMessage("menu.search-item.title"));

	private static ItemStack glassPane;
	private static int levelCount;

	private static void addLevelItemToPages(Internationalizable plugin, MakerDisplayableLevel level) {
		levelItems.put(level.getLevelId(), getLevelItem(plugin, level));
	}

	public synchronized static void addOrUpdateLevel(Internationalizable plugin, MakerDisplayableLevel level) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		addLevelItemToPages(plugin, level);
		MakerDisplayableLevel former = byIdMap.put(level.getLevelId(), level);
		if (former != null) {
			bySerialMap.remove(former.getLevelSerial());
			levelsByAuthorName.remove(former);
			levelsByName.remove(former);
			levelsBySerial.remove(former);
			levelsByLikes.remove(former);
			levelsByDislikes.remove(former);
			levelsByPublishDate.remove(former);
			levelsByRank.remove(former);
		}
		bySerialMap.put(level.getLevelSerial(), level);
		levelsByAuthorName.add(level);
		levelsByName.add(level);
		levelsBySerial.add(level);
		levelsByLikes.add(level);
		levelsByDislikes.add(level);
		levelsByPublishDate.add(level);
		levelsByRank.add(level);
	}

	private static int compareAuthorNameAndSerial(MakerDisplayableLevel l1, MakerDisplayableLevel l2) {
		int diff = Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
		if (diff == 0) {
			// avoid duplicated levels
			return diff;
		}
		diff = String.valueOf(l1.getAuthorName()).compareToIgnoreCase(String.valueOf(l2.getAuthorName()));
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
	}

	private static int compareDislikesAndSerial(MakerDisplayableLevel l1, MakerDisplayableLevel l2) {
		int diff = Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
		if (diff == 0) {
			// avoid duplicated levels
			return diff;
		}
		diff = Long.valueOf(l1.getDislikes()).compareTo(Long.valueOf(l1.getDislikes()));
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
	}

	private static int compareLikesAndSerial(MakerDisplayableLevel l1, MakerDisplayableLevel l2) {
		int diff = Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
		if (diff == 0) {
			// avoid duplicated levels
			return diff;
		}
		diff = Long.valueOf(l1.getLikes()).compareTo(Long.valueOf(l2.getLikes()));
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
	}

	private static int compareNamesAndSerial(MakerDisplayableLevel l1, MakerDisplayableLevel l2) {
		int diff = Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
		if (diff == 0) {
			// avoid duplicated levels
			return diff;
		}
		diff = String.valueOf(l1.getLevelName()).compareToIgnoreCase(String.valueOf(l2.getLevelName()));
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
	}

	private static int comparePublishDatesAndSerial(MakerDisplayableLevel l1, MakerDisplayableLevel l2) {
		int diff = Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
		if (diff == 0) {
			// avoid duplicated levels
			return diff;
		}
		diff = l1.getDatePublished().compareTo(l2.getDatePublished());
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
	}

	private static int compareRanksAndSerial(MakerDisplayableLevel l1, MakerDisplayableLevel l2) {
		int diff = Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
		if (diff == 0) {
			// avoid duplicated levels
			return diff;
		}
		diff = l1.getAuthorRank().compareTo(l2.getAuthorRank());
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
	}

	private static ItemStack getGlassPane(){
		if(glassPane == null){
			ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(StringUtils.EMPTY);
			itemStack.setItemMeta(itemMeta);
			glassPane = itemStack;
		}
		return glassPane;
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

	private static ItemStack getLevelItem(Internationalizable plugin, MakerDisplayableLevel level) {
		ItemBuilder builder = new ItemBuilder(Material.MONSTER_EGG);
		builder.withDisplayName(plugin.getMessage("menu.level-browser.level.display-name", level.getLevelName()));
		List<String> lore = new ArrayList<>();
		lore.add(plugin.getMessage("menu.level-browser.level.serial", level.getLevelSerial()));
		lore.add(StringUtils.EMPTY);
		lore.add(plugin.getMessage("menu.level-browser.level.created-by", level.getAuthorName()));
		lore.add(plugin.getMessage("menu.level-browser.level.created-by-rank", level.getAuthorRank().getDisplayName()));
		lore.add(StringUtils.EMPTY);
		lore.add(plugin.getMessage("menu.level-browser.level.likes", level.getLikes()));
		lore.add(plugin.getMessage("menu.level-browser.level.dislikes", level.getDislikes()));
		lore.add(plugin.getMessage("menu.level-browser.level.favorites", level.getFavs()));
		lore.add(plugin.getMessage("menu.level-browser.level.publish-date", level.getDatePublished()));
		lore.add(StringUtils.EMPTY);
		builder.withLore(lore);
		return builder.build();
	}

	public static synchronized Set<Long> getLevelPageSerials(LevelSortBy sortBy, boolean reverse, int offset, int limit) {
		TreeSet<MakerDisplayableLevel> allLevels = null;
		switch (sortBy) {
		case AUTHOR_RANK:
			allLevels = levelsByRank;
			break;
		case DISLIKES:
			allLevels = levelsByDislikes;
			break;
		case AUTHOR_NAME:
			allLevels = levelsByAuthorName;
			break;
		case DATE_PUBLISHED:
			allLevels = levelsByPublishDate;
			break;
		case LEVEL_NAME:
			allLevels = levelsByName;
			break;
		case LIKES:
			allLevels = levelsByLikes;
			break;
		case LEVEL_SERIAL:
		default:
			allLevels = levelsBySerial;
			break;
		}
		if (reverse) {
			return allLevels.descendingSet().stream().skip(offset).limit(limit).map(level -> level.getLevelSerial()).collect(Collectors.toSet());
		}
		return allLevels.stream().skip(offset).limit(limit).map(level -> level.getLevelSerial()).collect(Collectors.toSet());
	}

	public static int getLevelsPerPage() {
		return LEVELS_PER_PAGE;
	}

	public static int getPageOffset(int page) {
		return Math.max(0, (page - 1) * LEVELS_PER_PAGE);
	}

	public static String getTitleKey() {
		return "menu.level-browser.title";
	}

	public static void removeLevelBySerial(long levelSerial) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		MakerDisplayableLevel former = bySerialMap.remove(levelSerial);
		if (former != null) {
			byIdMap.remove(former.getLevelId());
			levelsByAuthorName.remove(former);
			levelsByName.remove(former);
			levelsBySerial.remove(former);
			levelsByLikes.remove(former);
			levelsByDislikes.remove(former);
			levelsByPublishDate.remove(former);
			levelsByRank.remove(former);
			removeLevelItemToPages(former.getLevelId());
		}
		// TODO: update all menus?
	}

	private static void removeLevelItemToPages(UUID levelId) {
		levelItems.remove(levelId);
	}

	public static void updateLevelCount(int levelCount) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		if (levelCount != LevelBrowserMenu.levelCount) {
			LevelBrowserMenu.levelCount = levelCount;
		}
	}

	public static void updateLevelLikes(Internationalizable plugin, UUID levelId, long totalLikes, long totalDislikes) {
		MakerDisplayableLevel level = byIdMap.get(levelId);
		if (level == null) {
			return;
		}
		levelsByLikes.remove(level);
		level.setLikes(totalLikes);
		levelsByLikes.add(level);
		levelsByDislikes.remove(level);
		level.setDislikes(totalDislikes);
		levelsByDislikes.add(level);
		addLevelItemToPages(plugin, level);
	}
	public static void updatePlayerMenu(UUID playerId) {
		LevelBrowserMenu menu = userLevelBrowserMenuMap.get(playerId);
		if (menu != null) {
			menu.update();
		}
	}
	private final Iterator<LevelSortBy> cycleSortBy;
	private int currentPage = 1;
	private LevelSortBy sortBy;

	private boolean reverseSortBy;

	private final UUID viewerId;
//	private boolean nextRequest = false;

	private LevelBrowserMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin, plugin.getMessage(getTitleKey()), 54);
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

	private synchronized List<MakerDisplayableLevel> getCurrentPageLevels() {
		TreeSet<MakerDisplayableLevel> allLevels = null;
		switch (sortBy) {
		case AUTHOR_RANK:
			allLevels = levelsByRank;
			break;
		case DISLIKES:
			allLevels = levelsByDislikes;
			break;
		case AUTHOR_NAME:
			allLevels = levelsByAuthorName;
			break;
		case DATE_PUBLISHED:
			allLevels = levelsByPublishDate;
			break;
		case LEVEL_NAME:
			allLevels = levelsByName;
			break;
		case LIKES:
			allLevels = levelsByLikes;
			break;
		case LEVEL_SERIAL:
		default:
			allLevels = levelsBySerial;
			break;
		}
		if (reverseSortBy) {
			return allLevels.descendingSet().stream().skip(getPageOffset(currentPage)).limit(LEVELS_PER_PAGE).collect(Collectors.toList());
		}
		return allLevels.stream().skip(getPageOffset(currentPage)).limit(LEVELS_PER_PAGE).collect(Collectors.toList());
	}

	private int getTotalPages() {
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
		items[5] = GeneralMenuItem.SORT.getItem();
		items[6] = this.reverseSortBy ? GeneralMenuItem.SORT_DIRECTION_DOWN.getItem(): GeneralMenuItem.SORT_DIRECTION_UP.getItem();
		items[18] = GeneralMenuItem.PREVIOUS_PAGE.getItem();
		items[26] = GeneralMenuItem.NEXT_PAGE.getItem();
		items[47] = GeneralMenuItem.CURRENT_PAGE.getItem();
		items[51] = GeneralMenuItem.EXIT_MENU.getItem();

		plugin.getController().requestLevelPageUpdate(sortBy, reverseSortBy, currentPage, getViewerId());
	}

	private boolean isLevelSlot(int index) {
		if (index > 9 && index < 17) {
			return true;
		}
		if (index > 18 && index < 26) {
			return true;
		}
		if (index > 27 && index < 35) {
			return true;
		}
		if (index > 36 && index < 44) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isShared() {
		return false;
	}

	private void nextPage() {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.nextPage - current page: [%s] - total pages: [%s] - items size: [%s] - level count: [%s]", currentPage, getTotalPages(), levelItems.size(), levelCount));
		}
		if (currentPage < getTotalPages()) {
			currentPage++;
		}
		plugin.getController().requestLevelPageUpdate(sortBy, reverseSortBy, currentPage, getViewerId());
		List<MakerDisplayableLevel> currentPageLevels = getCurrentPageLevels();
		update(currentPageLevels);
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
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
				Bukkit.getLogger().severe(
						String.format("LevelBrowserMenu.onClick - unable to get level serial from lore: [%s]", serial));
				return MenuClickResult.CANCEL_UPDATE;
			}
			plugin.getController().loadLevelForPlayingBySerial(mPlayer, Long.valueOf(serial));
			return MenuClickResult.CANCEL_CLOSE;
//		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.SEARCH.getDisplayName())) {
//		    mPlayer.getPlayer().openInventory(searchInventory);
//			return MenuClickResult.CANCEL_UPDATE;
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
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.PREVIOUS_PAGE.getDisplayName())) {
			previousPage();
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

	private void sortAscending() {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.sortAscending - reversed: [%s]", reverseSortBy));
		}
		reverseSortBy = false;
		currentPage = 1;
		update();
	}

	public void sortBy(LevelSortBy sortBy) {
		checkNotNull(sortBy);
		if (sortBy.equals(this.sortBy)) {
			return;
		}
		this.sortBy = sortBy;
		reverseSortBy = false;
		update();
	}

	private void sortByNext() {
		sortBy = cycleSortBy.next();
		reverseSortBy = false;
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

		List<MakerDisplayableLevel> currentPageLevels = getCurrentPageLevels();
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.update - current page size: [%s] - offset: [%s] - limit: [%s]", currentPageLevels.size(), getPageOffset(currentPage), LEVELS_PER_PAGE));
		}

		update(currentPageLevels);
	}

	public void update(List<MakerDisplayableLevel> currentPageLevels) {

		updatePaginationItems();

		for (int j = 10; j < 44; j++) {
			if (isLevelSlot(j)) {
				items[j] = getGlassPane();
			}
		}

		int i = 10;
		levelSlots: for (MakerDisplayableLevel level : currentPageLevels) {
			while (!isLevelSlot(i)) {
				i++;
				if (i >= items.length) {
					break levelSlots;
				}
			}
			ItemStack item = levelItems.get(level.getLevelId());
			if (item != null) {
				items[i] = item;
			} else {
				items[i] = getGlassPane();
			}
			i++;
		}
		for (; i < items.length; i++) {
			if (isLevelSlot(i)) {
				items[i] = getGlassPane();
			}
		}
		inventory.setContents(items);
	}

	private void updateCurrentPageItem() {
		if (items[47] == null) {
			items[47] = GeneralMenuItem.CURRENT_PAGE.getItem();
		}

		ItemMeta currentPageMeta = items[47].getItemMeta();
		currentPageMeta.setDisplayName(String.format("%s %s/%s", GeneralMenuItem.CURRENT_PAGE.getDisplayName(), currentPage, getTotalPages()));
		items[47].setItemMeta(currentPageMeta);
	}

	private void updateNextPageItem() {
		if (currentPage == getTotalPages()) {
			items[26] = getGlassPane();
			return;
		} 
		items[26] = GeneralMenuItem.NEXT_PAGE.getItem();
		ItemMeta nextPageMeta = items[26].getItemMeta();
		nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 1)));
		items[26].setItemMeta(nextPageMeta);
	}

	private void updatePaginationItems() {
		updateCurrentPageItem();
		updatePreviousPageItem();
		updateNextPageItem();
		updateSortItem();
		updateSortDirectionItem();
	}

	private void updatePreviousPageItem() {
		if (currentPage == 1) {
			items[18] = getGlassPane();
			return;
		}
		items[18] = GeneralMenuItem.PREVIOUS_PAGE.getItem();
		ItemMeta previousPageMeta = items[18].getItemMeta();
		previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 1)));
		items[18].setItemMeta(previousPageMeta);
	}

	private void updateSortDirectionItem() {
		items[6] = this.reverseSortBy ? GeneralMenuItem.SORT_DIRECTION_DOWN.getItem(): GeneralMenuItem.SORT_DIRECTION_UP.getItem();
	}

	private void updateSortItem() {
		if (items[5] == null) {
			items[5] = GeneralMenuItem.SORT.getItem();
		}

		ItemMeta sortMeta = items[5].getItemMeta();
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
		items[5].setItemMeta(sortMeta);
	}

}
