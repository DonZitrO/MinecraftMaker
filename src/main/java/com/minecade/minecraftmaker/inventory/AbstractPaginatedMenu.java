package com.minecade.minecraftmaker.inventory;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.CommonMenuItem;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractPaginatedMenu extends AbstractMakerMenu {

	public static final int ITEMS_PER_PAGE = 28;

	public static int getPageOffset(int page) {
		return Math.max(0, (page - 1) * ITEMS_PER_PAGE);
	}

	protected static boolean isItemSlot(int index) {
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

	protected int currentPage = 1;
	//protected int totalItemsCount = 0;

	protected AbstractPaginatedMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 54);
	}

	protected abstract int getTotalItemsCount();

	protected int getTotalPages() {
		return Math.max(1, (getTotalItemsCount() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
	}

	protected void init() {
		for (int i = 0; i < inventory.getSize(); i++) {
			items[i] = getGlassPane();
		}
		items[9] = CommonMenuItem.PREVIOUS_PAGE.getItem();
		items[18] = CommonMenuItem.PREVIOUS_10TH_PAGE.getItem();
		items[27] = CommonMenuItem.PREVIOUS_100TH_PAGE.getItem();
		items[36] = CommonMenuItem.PREVIOUS_1000TH_PAGE.getItem();
		items[17] = CommonMenuItem.NEXT_PAGE.getItem();
		items[26] = CommonMenuItem.NEXT_10TH_PAGE.getItem();
		items[35] = CommonMenuItem.NEXT_100TH_PAGE.getItem();
		items[44] = CommonMenuItem.NEXT_1000TH_PAGE.getItem();
		items[47] = CommonMenuItem.CURRENT_PAGE.getItem();
		items[51] = CommonMenuItem.EXIT_MENU.getItem();
	}

	private void next1000thPage() {
		if (currentPage + 1000 < getTotalPages()) {
			currentPage += 1000;
		}
		update();
	}

	private void next100thPage() {
		if (currentPage + 100 < getTotalPages()) {
			currentPage += 100;
		}
		update();
	}

	private void next10thPage() {
		if (currentPage + 10 < getTotalPages()) {
			currentPage += 10;
		}
		update();
	}

	private void nextPage() {
		if (currentPage < getTotalPages()) {
			currentPage++;
		}
		update();
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, Inventory clickedInventory, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, clickedInventory, slot, clickType);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(clickedItem, CommonMenuItem.NEXT_PAGE.getDisplayName())) {
			nextPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, CommonMenuItem.NEXT_10TH_PAGE.getDisplayName())) {
			next10thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, CommonMenuItem.NEXT_100TH_PAGE.getDisplayName())) {
			next100thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, CommonMenuItem.NEXT_1000TH_PAGE.getDisplayName())) {
			next1000thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, CommonMenuItem.PREVIOUS_PAGE.getDisplayName())) {
			previousPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, CommonMenuItem.PREVIOUS_10TH_PAGE.getDisplayName())) {
			previous10thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, CommonMenuItem.PREVIOUS_100TH_PAGE.getDisplayName())) {
			previous100thPage();
			return MenuClickResult.CANCEL_UPDATE;
		} else if (ItemUtils.itemNameEquals(clickedItem, CommonMenuItem.PREVIOUS_1000TH_PAGE.getDisplayName())) {
			previous1000thPage();
			return MenuClickResult.CANCEL_UPDATE;
		}
		return MenuClickResult.ALLOW;
	}

	private void previous1000thPage() {
		if (currentPage > 1000) {
			currentPage -= 1000;
			update();
		}
	}

	private void previous100thPage() {
		if (currentPage > 100) {
			currentPage -= 100;
			update();
		}
	}

	private void previous10thPage() {
		if (currentPage > 10) {
			currentPage -= 10;
			update();
		}
	}

	private void previousPage() {
		if (currentPage > 1) {
			currentPage--;
			update();
		}
	}

	private void updateCurrentPageItem() {
		if (items[47] == null) {
			items[47] = CommonMenuItem.CURRENT_PAGE.getItem();
		}

		ItemMeta currentPageMeta = items[47].getItemMeta();
		currentPageMeta.setDisplayName(String.format("%s %s/%s", CommonMenuItem.CURRENT_PAGE.getDisplayName(), currentPage, getTotalPages()));
		items[47].setItemMeta(currentPageMeta);
	}

	private void updateNextPageItems() {
		if (currentPage == getTotalPages()) {
			items[17] = getGlassPane();
		} else {
			items[17] = CommonMenuItem.NEXT_PAGE.getItem();
			ItemMeta nextPageMeta = items[17].getItemMeta();
			nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 1)));
			items[17].setItemMeta(nextPageMeta);
		}
		if (currentPage + 10 > getTotalPages()) {
			items[26] = getGlassPane();
		} else {
			items[26] = CommonMenuItem.NEXT_10TH_PAGE.getItem();
			ItemMeta nextPageMeta = items[26].getItemMeta();
			nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 10)));
			items[26].setItemMeta(nextPageMeta);
		}
		if (currentPage + 100 > getTotalPages()) {
			items[35] = getGlassPane();
		} else {
			items[35] = CommonMenuItem.NEXT_100TH_PAGE.getItem();
			ItemMeta nextPageMeta = items[35].getItemMeta();
			nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 100)));
			items[35].setItemMeta(nextPageMeta);
		}
		if (currentPage + 1000 > getTotalPages()) {
			items[44] = getGlassPane();
		} else {
			items[44] = CommonMenuItem.NEXT_1000TH_PAGE.getItem();
			ItemMeta nextPageMeta = items[44].getItemMeta();
			nextPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F%s --->", currentPage + 1000)));
			items[44].setItemMeta(nextPageMeta);
		}
	}

	protected void updatePaginationItems() {
		updateCurrentPageItem();
		updatePreviousPageItems();
		updateNextPageItems();
	}

	private void updatePreviousPageItems() {
		if (currentPage == 1) {
			items[9] = getGlassPane();
		} else {
			items[9] = CommonMenuItem.PREVIOUS_PAGE.getItem();
			ItemMeta previousPageMeta = items[9].getItemMeta();
			previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 1)));
			items[9].setItemMeta(previousPageMeta);
		}
		if (currentPage <= 10) {
			items[18] = getGlassPane();
		} else {
			items[18] = CommonMenuItem.PREVIOUS_10TH_PAGE.getItem();
			ItemMeta previousPageMeta = items[18].getItemMeta();
			previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 10)));
			items[18].setItemMeta(previousPageMeta);
		}
		if (currentPage <= 100) {
			items[27] = getGlassPane();
		} else {
			items[27] = CommonMenuItem.PREVIOUS_100TH_PAGE.getItem();
			ItemMeta previousPageMeta = items[27].getItemMeta();
			previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 100)));
			items[27].setItemMeta(previousPageMeta);
		}
		if (currentPage <= 1000) {
			items[36] = getGlassPane();
		} else {
			items[36] = CommonMenuItem.PREVIOUS_1000TH_PAGE.getItem();
			ItemMeta previousPageMeta = items[36].getItemMeta();
			previousPageMeta.setLore(Arrays.asList(StringUtils.EMPTY, String.format("§F<--- %s", currentPage - 1000)));
			items[36].setItemMeta(previousPageMeta);
		}
	}

}
