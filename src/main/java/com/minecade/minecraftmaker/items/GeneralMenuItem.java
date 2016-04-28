package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum GeneralMenuItem implements TranslatableItem {

	SEARCH(Material.COMPASS),
	SORT(Material.RAILS),
	CURRENT_PAGE(Material.PAPER),
	PREVIOUS_PAGE(Material.RAW_FISH),
	NEXT_PAGE(Material.SHEARS),
	EXIT_MENU(Material.ARROW),
	EDIT_LEVEL_OPTIONS(Material.ENDER_CHEST), 
	PLAY_LEVEL_OPTIONS(Material.ENDER_CHEST);

	private final ItemStackBuilder builder;

	private GeneralMenuItem(Material material) {
		this(material, 1);
	}

	private GeneralMenuItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private GeneralMenuItem(Material material, int amount, short data) {
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
		return "menu.general-item";
	}

	@Override
	public String getName() {
		return name();
	}

	@Override
	public ItemStackBuilder getBuilder() {
		return builder;
	}

}
