package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum LevelTemplateItem implements TranslatableItem {

	EMPTY_FLOOR(Material.GLASS),
	STONE_FLOOR(Material.STONE),
	GRASS_FLOOR(Material.GRASS),
	DIRT_FLOOR(Material.DIRT);

	private final ItemStackBuilder builder;

	private LevelTemplateItem(Material material) {
		this(material, 1);
	}

	private LevelTemplateItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private LevelTemplateItem(Material material, int amount, short data) {
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
		return "menu.level-template";
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
