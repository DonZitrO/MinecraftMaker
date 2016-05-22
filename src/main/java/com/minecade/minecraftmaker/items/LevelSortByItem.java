/*
package com.minecade.minecraftmaker.items;


import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum LevelSortByItem implements TranslatableItem {

	LIKES(Material.DIAMOND),
	OWNED(Material.SKULL_ITEM, 1, (short) 3);

	private final ItemStackBuilder builder;

	private LevelSortByItem(Material material) {
		this(material, 1);
	}

	private LevelSortByItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private LevelSortByItem(Material material, int amount, short data) {
		this.builder = new ItemBuilder(material, amount, data);
	}

	public ItemStack getItem() {
		return builder.build();
	}

	public String getDisplayName() {
		return builder.getDisplayName() != null ? builder.getDisplayName() : name();
	}

	@Override
	public String getTranslationKeyBase() {
		return "menu.level-sortby";
	}

	@Override
	public String getName() {
		return name();
	}

	@Override
	public ItemStackBuilder getBuilder() {
		return builder;
	}

	@Override
	public void setDisplayName(String displayName) {
		getBuilder().withDisplayName(displayName);
	}

}
*/
