package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
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
import com.minecade.minecraftmaker.level.LevelDisplay;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelBrowserMenu extends AbstractMakerMenu {

	private static Map<UUID, ItemStack> levelItems = new HashMap<>();
	private static int LEVELS_PER_PAGE = 36;

	private static TreeSet<MakerLevel> levelsByName = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> l1.getLevelName().compareToIgnoreCase(l2.getLevelName()));

	private static TreeSet<MakerLevel> levelsBySerial = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial())));
	private static ItemStack[] loadingPaneItems;
	private static int totalPublishedLevels = -1;
	private static Map<UUID, LevelBrowserMenu> userLevelBrowserMenuMap = new HashMap<>();

	private static void addLevelItemToPages(Internationalizable plugin, MakerLevel level) {
		levelItems.put(level.getLevelId(), getLevelItem(plugin, level));
	}

	public static void addPublishedLevel(Internationalizable plugin, MakerLevel level) {
		addLevelItemToPages(plugin, level);
		levelsByName.add(level);
		levelsBySerial.add(level);
		for (LevelBrowserMenu menu: userLevelBrowserMenuMap.values()) {
			menu.update();
		}
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

	private static ItemStack getLevelItem(Internationalizable plugin, MakerLevel level) {
		ItemBuilder builder = new ItemBuilder(Material.SIGN);
		builder.withDisplayName(plugin.getMessage("menu.level-browser.level.display-name", level.getLevelName()));
		List<String> lore = new ArrayList<>();
		lore.add(plugin.getMessage("menu.level-browser.level.serial", level.getLevelSerial()));
		lore.add("");
		lore.add(plugin.getMessage("menu.level-browser.level.created-by", level.getAuthorName()));
		lore.add("");
		lore.add(plugin.getMessage("menu.level-browser.level.likes", level.getLikes()));
		lore.add(plugin.getMessage("menu.level-browser.level.dislikes", level.getDislikes()));
		lore.add(plugin.getMessage("menu.level-browser.level.favorites", level.getFavs()));
		lore.add("");
		builder.withLore(lore);
		return builder.build();
	}

	private static ItemStack[] getLoadingPaneItems() {
		if (loadingPaneItems == null) {
			loadingPaneItems = new ItemStack[36];
			for (int i = 0; i < loadingPaneItems.length; i++) {
				if (i == 20 || i == 22 || i == 24) {
					loadingPaneItems[i] = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)15);
				} else {
					loadingPaneItems[i] = new ItemStack(Material.STAINED_GLASS_PANE);
				}
			}
		}
		return loadingPaneItems;
	}

	public static String getTitleKey() {
		return "menu.level-browser.title";
	}

	public static void loadDefaultPage(MinecraftMakerPlugin plugin) {
		plugin.getDatabaseAdapter().loadLevelsAsync(LevelSortBy.LEVEL_SERIAL, 0, LEVELS_PER_PAGE);
	}

	public static void updatePages(Internationalizable plugin, Collection<MakerLevel> levels, LevelSortBy sortBy) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		for (MakerLevel level : levels) {
			addLevelItemToPages(plugin, level);
		}
		switch (sortBy) {
		case LEVEL_NAME:
			levelsByName.addAll(levels);
			break;
		case LEVEL_SERIAL:
			levelsBySerial.addAll(levels);
			break;
		default:
			break;
		}
	}

	private int currentPage = 1;

	private LevelDisplay display = LevelDisplay.PUBLISHED;
	private TreeSet<MakerLevel> ownedLevelsBySerial = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial())));
	private LevelSortBy sortBy = LevelSortBy.LEVEL_SERIAL;
	private int totalOwnedLevels = -1;

	private final UUID viewerId;

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

	public void display(LevelDisplay display) {
		checkNotNull(display);
		if (display.equals(this.display)) {
			return;
		}
		this.display = display;
		update();
	}

	private int getTotalPages() {
		if (LevelDisplay.OWNED.equals(this.display) || totalPublishedLevels < 1) {
			return 1;
		}
		return ((totalPublishedLevels / LEVELS_PER_PAGE) + 1);
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	private void init() {
		items[0] = GeneralMenuItem.SORT.getItem();
		items[1] = GeneralMenuItem.SEARCH.getItem();
		items[3] = GeneralMenuItem.PREVIOUS_PAGE.getItem();
		updateCurrentPageItem();
		items[5] = GeneralMenuItem.NEXT_PAGE.getItem();
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
		for (int i = 9; i < inventory.getSize(); i++) {
			items[i] = new ItemStack(Material.STAINED_GLASS_PANE);
		}
	}

	@Override
	public boolean isShared() {
		return false;
	}

	private void nextPage() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onClick(MakerPlayer mPlayer, int slot) {
		if (slot >= items.length) {
			return true;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return true;
		}
		Bukkit.getLogger().info(String.format("LevelBrowserMenu.onClick - clicked item material: [%s]", clickedItem.getType()));
		if (clickedItem.getType().equals(Material.SIGN_POST) || clickedItem.getType().equals(Material.SIGN)) {
			String serial = ItemUtils.getLoreLine(clickedItem, 0);
			if (StringUtils.isBlank(serial) || !StringUtils.isNumeric(serial)) {
				Bukkit.getLogger().severe(String.format("LevelBrowserMenu.onClick - unable to get level serial from lore: [%s]", serial));
				return true;
			}
			if (LevelDisplay.OWNED.equals(this.display)) {
				plugin.getController().loadLevelForEditingBySerial(mPlayer, Long.valueOf(serial));
			} else {
				plugin.getController().loadLevelForPlayingBySerial(mPlayer, Long.valueOf(serial));
			}
			return true;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.SORT.getDisplayName())) {
			mPlayer.openLevelSortbyMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.NEXT_PAGE.getDisplayName())) {
			nextPage();
			return true;
		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.PREVIOUS_PAGE.getDisplayName())) {
			previousPage();
			return true;
		}
		return true;
	}

	private void previousPage() {
		// TODO Auto-generated method stub
	}

	public void removeUnpublishedLevel(MakerLevel makerLevel) {
		ownedLevelsBySerial.remove(makerLevel);
		if (LevelDisplay.OWNED.equals(this.display)) {
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
		// TODO use local dirtyInventory
		// if (!dirtyInventory) return;

		Collection<MakerLevel> allLevels = null;
		switch (display) {
		case OWNED:
			currentPage = 1;
			allLevels = ownedLevelsBySerial;
			break;
		default:
			switch (sortBy) {
			case LEVEL_NAME:
				allLevels = levelsByName;
				break;
			default:
				allLevels = levelsBySerial;
				break;
			}
			break;
		}

		int offset = (Math.max(0, (currentPage - 1) * LEVELS_PER_PAGE));
		List<MakerLevel> currentPageLevels = allLevels.stream().skip(offset).limit(LEVELS_PER_PAGE).collect(Collectors.toList());

		if (currentPageLevels == null || currentPageLevels.size() == 0) {
			switch (display) {
			case OWNED:
				if (totalOwnedLevels < 0) {
					plugin.getDatabaseAdapter().loadUnpublishedLevelsByAuthorIdAsync(this);
				}
				break;
			default:
				if (totalPublishedLevels < 0) {
					plugin.getDatabaseAdapter().loadLevelsAsync(sortBy, offset, LEVELS_PER_PAGE);
				}
				break;
			}
			ItemStack[] loadingPane = getLoadingPaneItems();
			for (int i = 9; i < (loadingPane.length + 9); i++) {
				items[i] = loadingPane[i-9];
			}
			inventory.setContents(items);
			return;
		}

		if (currentPageLevels != null) {
			updateCurrentPageItem();
			int i = 18;
			for (MakerLevel level : currentPageLevels) {
				ItemStack item = levelItems.get(level.getLevelId());
				if (item != null) {
					items[i++] = item;
				} else {
					new ItemStack(Material.STAINED_GLASS_PANE);
				}
			}
			for (; i < inventory.getSize(); i++) {
				items[i] = new ItemStack(Material.STAINED_GLASS_PANE);
			}
			inventory.setContents(items);
		}
	}

	private void updateCurrentPageItem() {
		if (items[4] == null) {
			items[4] = GeneralMenuItem.CURRENT_PAGE.getItem();
		}
		ItemMeta currentPageMeta = items[4].getItemMeta();
		currentPageMeta.setDisplayName(String.format(GeneralMenuItem.CURRENT_PAGE.getDisplayName(), currentPage, getTotalPages()));
		items[4].setItemMeta(currentPageMeta);
	}

	public void updateOwnedLevels(Collection<MakerLevel> levels) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		for (MakerLevel level : levels) {
			addLevelItemToPages(plugin, level);
		}
		ownedLevelsBySerial.addAll(levels);
		totalOwnedLevels = ownedLevelsBySerial.size();
		if (LevelDisplay.OWNED.equals(this.display)) {
			update();
		}
	}

	public void updateOwnedLevel(MakerLevel level) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		addLevelItemToPages(plugin, level);
		ownedLevelsBySerial.add(level);
		totalOwnedLevels = ownedLevelsBySerial.size();
		if (LevelDisplay.OWNED.equals(this.display)) {
			update();
		}
	}

}
