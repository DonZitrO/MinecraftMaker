package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.minecade.mcore.inventory.ActiveItemEnchantment;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractMakerMenu {

	private static ItemStack glassPane;
	private static ItemStack blackGlassPane;

	protected final MinecraftMakerPlugin plugin;

	protected Inventory inventory;

	protected ItemStack[] items;
	protected AbstractMakerMenu back;

	public AbstractMakerMenu(MinecraftMakerPlugin plugin, int size) {
		this(plugin, size, null);
	}

	public AbstractMakerMenu(MinecraftMakerPlugin plugin, int size, String titleModifier) {
		this.plugin = plugin;
		this.items = new ItemStack[size];
		String title = plugin.getMessage(getTitleKey(titleModifier));
		if (title.length() > 32) {
			title = title.substring(0, 32);
		}
		inventory = Bukkit.createInventory(null, items.length, title);
		inventory.setContents(items);
	}

	public String getName() {
		return inventory.getName();
	}

	public String getTitle() {
		return inventory.getTitle();
	}

	protected void setActive(int slot) {
		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (null != item) {
				if (slot != i) {
					item.removeEnchantment(ActiveItemEnchantment.getActiveItemEnchantment());
				} else {
					item.addUnsafeEnchantment(ActiveItemEnchantment.getActiveItemEnchantment(), 10);
				}
			}
		}
	}

	protected static void loadGlassPanes(ItemStack[] items) {
		if (items == null) {
			return;
		}
		if (glassPane == null) {
			ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(" ");
			itemStack.setItemMeta(itemMeta);
			glassPane = itemStack;
		}
		for (int i = 0; i < items.length; i++) {
			items[i] = glassPane;
		}
	}

	protected static ItemStack getGlassPane() {
		if (glassPane == null) {
			ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(" ");
			itemStack.setItemMeta(itemMeta);
			glassPane = itemStack;
		}
		return glassPane;
	}

	protected static ItemStack getBlackGlassPane() {
		if (blackGlassPane == null) {
			ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)15);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(" ");
			itemStack.setItemMeta(itemMeta);
			blackGlassPane = itemStack;
		}
		return blackGlassPane;
	}

	protected void setInactive(int slot) {
		ItemStack item = items[slot];
		if (null != item) {
			item.removeEnchantment(ActiveItemEnchantment.getActiveItemEnchantment());
		}
	}

	protected void toggleActive(int slot) {
		ItemStack item = items[slot];
		if (null != item) {
			if (item.containsEnchantment(ActiveItemEnchantment.getActiveItemEnchantment())) {
				item.removeEnchantment(ActiveItemEnchantment.getActiveItemEnchantment());
			} else {
				item.addEnchantment(ActiveItemEnchantment.getActiveItemEnchantment(), 10);
			}
		}
	}

	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		if (slot >= items.length) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem == null || !ItemUtils.hasDisplayName(clickedItem)) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		if (ItemUtils.itemNameEquals(clickedItem, GeneralMenuItem.EXIT_MENU.getDisplayName())) {
			return MenuClickResult.CANCEL_CLOSE;
		}
		return MenuClickResult.ALLOW;
	}

	public void open(Player player) {
		if (player == null) {
			return;
		}
		inventory.setContents(items);
		player.openInventory(inventory);
	}

	public abstract void update();

	public abstract boolean isShared();

	public abstract String getTitleKey(String modifier);

//	public int getIdentityHashCode() {
//		if (inventory == null) {
//			return 0;
//		}
//		int identity = System.identityHashCode(inventory);
//		if (plugin.isDebugMode()) {
//			Bukkit.getLogger().info(String.format("[DEBUG] | AbstractMakerMenu.getIdentityHashCode - title: [%s] - identity: [%s]", getTitle(), identity));
//		}
//		return identity;
//	}

//	protected void updateItems() {
//		inventory.setContents(items);
//	}

	public void disable() {
		if (isShared()) {
			return;
		}
		if (inventory != null) {
			inventory.clear();
		}
		inventory = null;
		items = null;
		back = null;
	}

}
