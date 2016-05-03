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

import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class PlayerLevelsMenu extends AbstractMakerMenu {

	private static int LEVELS_PER_PAGE = 36;

	private static Map<UUID, ItemStack> levelItems = new HashMap<>();
	private static ItemStack[] loadingPaneItems;
	private static Map<UUID, PlayerLevelsMenu> userLevelBrowserMenuMap = new HashMap<>();

	private static void addLevelItemToPages(Internationalizable plugin, MakerLevel level) {
		levelItems.put(level.getLevelId(), getLevelItem(plugin, level));
	}

	public static PlayerLevelsMenu getInstance(MinecraftMakerPlugin plugin, UUID viewerId) {
		checkNotNull(plugin);
		checkNotNull(viewerId);
		PlayerLevelsMenu menu = userLevelBrowserMenuMap.get(viewerId);
		if (menu == null) {
			menu = new PlayerLevelsMenu(plugin, viewerId);
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
		return "menu.player-levels.title";
	}

	private TreeSet<MakerLevel> ownedLevelsBySerial = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial())));
	private LevelSortBy sortBy = LevelSortBy.LEVEL_SERIAL;
	private int totalOwnedLevels = -1;

	private final UUID viewerId;

	private PlayerLevelsMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin, plugin.getMessage(getTitleKey()), 54);
		this.viewerId = viewerId;
		init();
	}

	@Override
	public void destroy() {
		super.destroy();
		userLevelBrowserMenuMap.remove(getViewerId());
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	private void init() {
		items[0] = GeneralMenuItem.SORT.getItem();
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
		for (int i = 9; i < inventory.getSize(); i++) {
			items[i] = new ItemStack(Material.STAINED_GLASS_PANE);
		}
	}

	@Override
	public boolean isShared() {
		return false;
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
		Bukkit.getLogger().info(String.format("PlayerLevelsMenu.onClick - clicked item material: [%s]", clickedItem.getType()));
		if (clickedItem.getType().equals(Material.SIGN_POST) || clickedItem.getType().equals(Material.SIGN)) {
			String serial = ItemUtils.getLoreLine(clickedItem, 0);
			if (StringUtils.isBlank(serial) || !StringUtils.isNumeric(serial)) {
				Bukkit.getLogger().severe(String.format("PlayerLevelsMenu.onClick - unable to get level serial from lore: [%s]", serial));
				return true;
			}
			plugin.getController().loadLevelForEditingBySerial(mPlayer, Long.valueOf(serial));
			return true;
//		} else if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.SORT.getDisplayName())) {
//			mPlayer.openLevelSortbyMenu();
//			return true;
		}
		return true;
	}

	public void removeUnpublishedLevel(MakerLevel makerLevel) {
		ownedLevelsBySerial.remove(makerLevel);
		update();
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
		switch (sortBy) {
		default:
			allLevels = ownedLevelsBySerial;
			break;
		}

		int offset = 0; //(Math.max(0, (currentPage - 1) * LEVELS_PER_PAGE));
		List<MakerLevel> currentPageLevels = allLevels.stream().skip(offset).limit(LEVELS_PER_PAGE).collect(Collectors.toList());

		if (currentPageLevels == null || currentPageLevels.size() == 0) {
			if (totalOwnedLevels < 0) {
				plugin.getDatabaseAdapter().loadUnpublishedLevelsByAuthorIdAsync(this);
			}
			ItemStack[] loadingPane = getLoadingPaneItems();
			for (int i = 9; i < (loadingPane.length + 9); i++) {
				items[i] = loadingPane[i-9];
			}
			inventory.setContents(items);
			return;
		}

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

	public void updateOwnedLevels(Collection<MakerLevel> levels) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		for (MakerLevel level : levels) {
			addLevelItemToPages(plugin, level);
		}
		ownedLevelsBySerial.addAll(levels);
		totalOwnedLevels = ownedLevelsBySerial.size();
		update();
	}

	public void updateOwnedLevel(MakerLevel level) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		addLevelItemToPages(plugin, level);
		ownedLevelsBySerial.add(level);
		totalOwnedLevels = ownedLevelsBySerial.size();
		update();
	}

}
