package com.minecade.core.item;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class ItemUtils {

	public static boolean itemNameEquals(ItemStack stack, ItemStack stack2) {
		if (stack.getItemMeta() == null || stack2.getItemMeta() == null) {
			return false;
		}
		String name1 = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
		String name2 = ChatColor.stripColor(stack2.getItemMeta().getDisplayName());
		if (name1 == null || name2 == null) {
			return false;
		}
		return (name1.equalsIgnoreCase(name2));
	}

	public static boolean itemNameEquals(ItemStack stack, String itemName) {
		if (stack.getItemMeta() == null || itemName == null) {
			return false;
		}
		String s = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
		if (s == null) {
			return false;
		}

		return (ChatColor.stripColor(itemName).equals(ChatColor.stripColor(stack.getItemMeta().getDisplayName())));
	}

	public static boolean itemNameContainsIgnoreCase(ItemStack stack, String text) {
		return stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName() && stack.getItemMeta().getDisplayName().toLowerCase().contains(text.toLowerCase());
	}

	public static boolean hasDisplayName(ItemStack stack) {
		return stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName();
	}

	public static String getDisplayName(ItemStack stack) {
		return hasDisplayName(stack) ? stack.getItemMeta().getDisplayName():"";
	}

	private ItemUtils() {
		super();
	}

}