package com.minecade.minecraftmaker.inventory;

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
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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

	private static int LEVELS_PER_PAGE = 36;

	private static Map<UUID, ItemStack> levelItems = new HashMap<>();
	private static TreeSet<MakerLevel> levelsByName = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> l1.getLevelName().compareToIgnoreCase(l2.getLevelName()));
	private static TreeSet<MakerLevel> levelsBySerial = new TreeSet<MakerLevel>((MakerLevel l1, MakerLevel l2) -> Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial())));

	private static void addLevelItemToPages(Internationalizable plugin, MakerLevel level) {
		levelItems.put(level.getLevelId(), getLevelItem(plugin, level));
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

	public static String getTitleKey() {
		return "menu.level-browser.title";
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

	private LevelSortBy sortBy = LevelSortBy.LEVEL_SERIAL;
	private int currentPage = 1;
	private int totalPages = 1;

	public LevelBrowserMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage(getTitleKey()), 54);
		init();
		plugin.getDatabaseAdapter().loadLevelsAsync(sortBy, (currentPage - 1) * LEVELS_PER_PAGE, LEVELS_PER_PAGE);
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

	private void updateCurrentPageItem() {
		if (items[4] == null) {
			items[4] = GeneralMenuItem.CURRENT_PAGE.getItem();
		}
		ItemMeta currentPageMeta = items[4].getItemMeta();
		currentPageMeta.setDisplayName(String.format(GeneralMenuItem.CURRENT_PAGE.getDisplayName(), currentPage, totalPages));
		items[4].setItemMeta(currentPageMeta);
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
		Bukkit.getLogger().info(String.format("LevelBrowserMenu.onClick - clicked item material: [%s]", clickedItem.getType()));
		if (clickedItem.getType().equals(Material.SIGN_POST) || clickedItem.getType().equals(Material.SIGN)) {
			String serial = ItemUtils.getLoreLine(clickedItem, 0);
			if (StringUtils.isBlank(serial) || !StringUtils.isNumeric(serial)) {
				Bukkit.getLogger().severe(String.format("LevelBrowserMenu.onClick - unable to get level serial from lore: [%s]", serial));
				return true;
			}
			plugin.getController().loadLevelForPlayingBySerial(mPlayer, Long.valueOf(serial));
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

	private void nextPage() {
		// TODO Auto-generated method stub
	}

	@Override
	public void open(Player player) {
		update();
		super.open(player);
	}

	@Override
	public void update() {
		// TODO use local dirtyInventory
		// if (!dirtyInventory) return;

		Collection<MakerLevel> allLevels = null;
		switch (sortBy) {
		case LEVEL_NAME:
			allLevels = levelsByName;
			break;
		default:
			allLevels = levelsBySerial;
			break;
		}
	
		List<MakerLevel> currentPageLevels = allLevels.stream().skip((Math.max(0, (currentPage - 1) * LEVELS_PER_PAGE))).limit(LEVELS_PER_PAGE).collect(Collectors.toList());

		if (currentPageLevels != null) {
			int i = 18;
			for (MakerLevel level: currentPageLevels) {
				ItemStack item = levelItems.get(level.getLevelId());
				if (item != null) {
					items[i++] = item;
				} else {
					new ItemStack(Material.STAINED_GLASS_PANE);
				}
			}
			for (;i<inventory.getSize();i++) {
				items[i] = new ItemStack(Material.STAINED_GLASS_PANE);
			}
		}
	}

}
