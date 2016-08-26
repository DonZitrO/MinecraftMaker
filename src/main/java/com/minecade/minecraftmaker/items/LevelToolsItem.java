package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.i18n.TranslatableItem;
import com.minecade.mcore.item.ItemBuilder;
import com.minecade.mcore.item.ItemStackBuilder;

public enum LevelToolsItem implements TranslatableItem {

	EXIT(Material.ARROW),
	SKULL(Material.SKULL_ITEM),
	TIME(Material.WATCH),
	WEATHER(Material.DOUBLE_PLANT);

	private final ItemStackBuilder builder;

	private LevelToolsItem(Material material) {
		this(material, 1);
	}

	private LevelToolsItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private LevelToolsItem(Material material, int amount, short data) {
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
		return "menu.level-tools";
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
