package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelBrowserMenu extends AbstractMakerMenu {

	private static final int LEVELS_PER_PAGE = 36;
	private static final int MIN_INTERVAL_PER_LOAD_REQUEST_MILLIS = 10000;

	private static Map<UUID, ItemStack> levelItems = new HashMap<>();
	private static Map<UUID, MakerLevel> levelMap = new HashMap<>();
	private static Map<UUID, LevelBrowserMenu> userLevelBrowserMenuMap = new HashMap<>();
	private static TreeSet<MakerLevel> levelsByLikes = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> compareLikesAndSerial(l1, l2));
	private static TreeSet<MakerLevel> levelsByName = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> l1.getLevelName().compareToIgnoreCase(l2.getLevelName()));
	private static TreeSet<MakerLevel> levelsBySerial = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial())));
	private static TreeSet<MakerLevel> levelsByPublishDate = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> comparePublishDatesAndSerial(l1, l2));
	private static TreeSet<MakerLevel> levelsByRank = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> compareRanksAndSerial(l1, l2));

	//private static ItemStack[] loadingPaneItems;

	private static long lastLoadRequest;
	private static int records;

	private static void addLevelItemToPages(Internationalizable plugin, MakerLevel level) {
		levelItems.put(level.getLevelId(), getLevelItem(plugin, level));
	}

	public static void addOrUpdateLevel(Internationalizable plugin, MakerLevel level) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		addOrUpdateLevel(plugin, level, true);
	}

	private static void addOrUpdateLevel(Internationalizable plugin, MakerLevel level, boolean update) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		addLevelItemToPages(plugin, level);
		levelMap.put(level.getLevelId(), level);
		levelsByName.add(level);
		levelsBySerial.add(level);
		levelsByLikes.add(level);
		levelsByPublishDate.add(level);
		levelsByRank.add(level);;
		if (update) {
			updateAllMenues();
		}
	}

	public static void addOrUpdateLevels(MinecraftMakerPlugin plugin, Collection<MakerLevel> levels, int records) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerDatabaseAdapter.addOrUpdateLevels - total levels: [%s]", levels.size()));
		}
		for (MakerLevel level : levels) {
			addOrUpdateLevel(plugin, level, false);
		}
		LevelBrowserMenu.records = records;
		updateAllMenues();
	}

	private static int compareLikesAndSerial(MakerLevel l1, MakerLevel l2) {
		int diff = Long.valueOf(l2.getLikes()).compareTo(Long.valueOf(l1.getLikes()));
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
	}

	private static int comparePublishDatesAndSerial(MakerLevel l1, MakerLevel l2) {
		int diff = l1.getDatePublished().compareTo(l2.getDatePublished());
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
	}

	private static int compareRanksAndSerial(MakerLevel l1, MakerLevel l2) {
		int diff = l1.getAuthorRank().compareTo(l2.getAuthorRank());
		if (diff != 0) {
			return diff;
		}
		return Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial()));
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

    public static Map<UUID, MakerLevel> getLevelsMap(){
        return levelMap;
    }

	private static ItemStack getLevelItem(Internationalizable plugin, MakerLevel level) {
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

//	private static ItemStack[] getLoadingPaneItems() {
//		if (loadingPaneItems == null) {
//			loadingPaneItems = new ItemStack[36];
//			for (int i = 0; i < loadingPaneItems.length; i++) {
//				if (i == 20 || i == 22 || i == 24) {
//					loadingPaneItems[i] = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)15);
//				} else {
//					loadingPaneItems[i] = new ItemStack(Material.STAINED_GLASS_PANE);
//				}
//			}
//		}
//		return loadingPaneItems;
//    }    return loadingPaneItems;
//	}

	public static String getTitleKey() {
        // return StringUtils.center(MinecraftMakerPlugin.getInstance().getMessage("menu.level-browser.title"), 32, StringUtils.EMPTY);
		return "menu.level-browser.title";
	}

	public static void loadDefaultPage(MinecraftMakerPlugin plugin) {
		lastLoadRequest = System.currentTimeMillis();
		plugin.getDatabaseAdapter().loadPublishedLevelsAsync(LevelSortBy.LIKES, 0, LEVELS_PER_PAGE);
	}

	private static void updateAllMenues() {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		for (LevelBrowserMenu menu : userLevelBrowserMenuMap.values()) {
			menu.update();
		}
	}

	public static void updateLevelLikes(Internationalizable plugin, UUID levelId, long totalLikes, long totalDislikes) {
		MakerLevel level = levelMap.get(levelId);
		if (level == null) {
			return;
		}
		level.setLikes(totalLikes);
		level.setDislikes(totalDislikes);
		addLevelItemToPages(plugin, level);

		updateLevelOwnerLikes(level);
	}

	private static void updateLevelOwnerLikes(MakerLevel makerLevel){
		MakerPlayer makerPlayer = MinecraftMakerPlugin.getInstance().getController().getPlayer(makerLevel.getAuthorId());
		if(makerPlayer != null){
			makerPlayer.setLevelsLikes(makerPlayer.getLevelsLikes() + 1);
		}
	}

	private int currentPage = 1;
	private LevelSortBy sortBy = LevelSortBy.LEVEL_SERIAL;
	private final UUID viewerId;
	private boolean nextRequest = false;

	private LevelBrowserMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin, plugin.getMessage(getTitleKey()), 54);
		this.viewerId = viewerId;
		init();
	}

	@Override
	public void destroy() {
		super.destroy();
		userLevelBrowserMenuMap.remove(getViewerId());
	}

//	private void displayLoadingPage() {
//		items[4] = GeneralMenuItem.LOADING_PAGE.getItem();
//		ItemStack[] loadingPane = getLoadingPaneItems();
//		for (int i = 9; i < (loadingPane.length + 9); i++) {
//			items[i] = loadingPane[i - 9];
//		}
//		inventory.setContents(items);
//	}

	private int getCurrentOffset() {
		return Math.max(0, (currentPage - 1) * LEVELS_PER_PAGE);
	}

	private List<MakerLevel> getCurrentPageLevels(Collection<MakerLevel> allLevels) {
		return allLevels.stream().skip(getCurrentOffset()).limit(LEVELS_PER_PAGE).collect(Collectors.toList());
	}

	private int getTotalPages() {
		//return (levelItems.size() + LEVELS_PER_PAGE - 1) / LEVELS_PER_PAGE;
		return (records + LEVELS_PER_PAGE - 1) / LEVELS_PER_PAGE;
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	private void init() {
		for (int i = 0; i < inventory.getSize(); i++) {
			items[i] = new ItemStack(Material.STAINED_GLASS_PANE, 1);
		}

		items[2] = GeneralMenuItem.SEARCH.getItem();
		items[6] = GeneralMenuItem.SORT.getItem();
		items[18] = GeneralMenuItem.PREVIOUS_PAGE.getItem();
		items[26] = GeneralMenuItem.NEXT_PAGE.getItem();
		items[47] = GeneralMenuItem.CURRENT_PAGE.getItem();
		items[51] = GeneralMenuItem.EXIT_MENU.getItem();

		updateCurrentPageItem();
		updatePreviousPageItem();
		updateNextPageItem();
		updateSortItem(null);
	}

    private boolean isLevelSlot(int index){
        if(index>9 && index<17) return true;
        if(index>18 && index<26) return true;
        if(index>27 && index<35) return true;
        if(index>36 && index<44) return true;

        return false;
	}

	@Override
	public boolean isShared() {
		return false;
	}

	private void nextPage() {
		if (nextRequest) {
			return;
		}
		if (currentPage < getTotalPages()) {
			currentPage++;
			update();
		} else {
			if (lastLoadRequest + MIN_INTERVAL_PER_LOAD_REQUEST_MILLIS < System.currentTimeMillis()) {
				nextRequest = true;
				lastLoadRequest = System.currentTimeMillis();
				plugin.getDatabaseAdapter().loadPublishedLevelsAsync(sortBy, getCurrentOffset(), LEVELS_PER_PAGE * 2);
				// displayLoadingPage();
			} else {
				// too fast load requests - no-op
			}
		}
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		if (slot >= items.length) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		Bukkit.getLogger().info(String.format("LevelBrowserMenu.onClick - clicked item material: [%s]", clickedItem.getType()));
        if (clickedItem.getType().equals(Material.MONSTER_EGG)) {
            String serial = ItemUtils.getLoreLine(clickedItem, 1);
            if (StringUtils.isBlank(serial) || !StringUtils.isNumeric(serial)) {
                Bukkit.getLogger().severe(String.format("LevelBrowserMenu.onClick - unable to get level serial from lore: [%s]", serial));
                return MenuClickResult.CANCEL_UPDATE;
            }
            plugin.getController().loadLevelForPlayingBySerial(mPlayer, Long.valueOf(serial));
            return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.SORT.getDisplayName())) {
			mPlayer.openLevelBrowserMenu(MinecraftMakerPlugin.getInstance(), mPlayer.getNextLevelSortBy(), false);
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

	public void sortBy(LevelSortBy sortBy) {
		checkNotNull(sortBy);
		if (sortBy.equals(this.sortBy)) {
			return;
		}
		this.sortBy = sortBy;
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
		Collection<MakerLevel> allLevels = null;
		switch (sortBy) {
		case DATE_PUBLISHED:
			allLevels = levelsByPublishDate;
			break;
		case LEVEL_NAME:
			allLevels = levelsByName;
			break;
		case LIKES:
			allLevels = levelsByLikes;
			break;
		case RANK:
			allLevels = levelsByRank;
			break;
		default:
			allLevels = levelsBySerial;
			break;
		}

		if (nextRequest) {
			currentPage++;
		}
		List<MakerLevel> currentPageLevels = getCurrentPageLevels(allLevels);
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelBrowserMenu.update - current page size: [%s] - offset: [%s] - limit: [%s]", currentPageLevels.size(), getCurrentOffset(), LEVELS_PER_PAGE));
		}
		if (nextRequest) {
			nextRequest = false;
			if (currentPageLevels.size() == 0) {
				currentPage--;
				currentPageLevels = getCurrentPageLevels(allLevels);
			}
		}

		MakerPlayer makerPlayer = MinecraftMakerPlugin.getInstance().getController().getPlayer(this.viewerId);
		updateCurrentPageItem();
		updatePreviousPageItem();
		updateNextPageItem();
		if(makerPlayer != null){
			for (int i= 10; i<items.length; i++) {
				if (isLevelSlot(i)) {
					int currentPageLevelIndex = i - 10;
					if(currentPageLevels.size() > currentPageLevelIndex){
						MakerLevel makerLevel = currentPageLevels.get(currentPageLevelIndex);
						if(makerLevel != null){
							ItemStack item = levelItems.get(makerLevel.getLevelId());
							if(item != null) items[i] = item;
						}
					} else if(currentPageLevels.size() > 0){
						items[i] = new ItemStack(Material.STAINED_GLASS_PANE);
					}
				}
			}

			updateSortItem(makerPlayer.getLevelSortBy());
		}
		inventory.setContents(items);
	}

	private void updateCurrentPageItem() {
		if (items[47] == null) items[47] = GeneralMenuItem.CURRENT_PAGE.getItem();

		ItemMeta currentPageMeta = items[47].getItemMeta();
		currentPageMeta.setDisplayName(String.format("%s %s/%s",
				GeneralMenuItem.CURRENT_PAGE.getDisplayName(), currentPage, getTotalPages()));
		items[47].setItemMeta(currentPageMeta);
	}

	private void updateNextPageItem(){
		if (items[26] == null) items[26] = GeneralMenuItem.NEXT_PAGE.getItem();

		int nextPage = currentPage < getTotalPages() ? currentPage + 1 : currentPage;
		ItemMeta nextPageMeta = items[26].getItemMeta();
		nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", nextPage)));
		items[26].setItemMeta(nextPageMeta);
	}

	private void updatePreviousPageItem(){
		if (items[18] == null) items[18] = GeneralMenuItem.PREVIOUS_PAGE.getItem();

		int previousPage = currentPage > 1 ? currentPage - 1 : currentPage;
		ItemMeta previousPageMeta = items[18].getItemMeta();
		previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", previousPage)));
		items[18].setItemMeta(previousPageMeta);
	}

	private void updateSortItem(LevelSortBy levelSortBy){
		if (items[6] == null) items[6] = GeneralMenuItem.SORT.getItem();

		ItemMeta sortMeta = items[6].getItemMeta();
		sortMeta.setLore(Arrays.asList(
				StringUtils.EMPTY,
				LevelSortBy.LEVEL_NAME.getDisplayName(levelSortBy),
				LevelSortBy.LEVEL_SERIAL.getDisplayName(levelSortBy),
				LevelSortBy.LIKES.getDisplayName(levelSortBy),
				LevelSortBy.DATE_PUBLISHED.getDisplayName(levelSortBy),
				LevelSortBy.RANK.getDisplayName(levelSortBy)));
		items[6].setItemMeta(sortMeta);
	}
}
