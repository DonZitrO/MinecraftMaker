package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum GeneralMenuItem implements TranslatableItem {

	CONTINUE_EDITING(Material.EMPTY_MAP),
	CURRENT_PAGE(Material.PAPER),
	EDIT_LEVEL_OPTIONS(Material.ENDER_CHEST),
	EDITOR_PLAY_LEVEL_OPTIONS(Material.ENDER_CHEST),
	EXIT_MENU(Material.ARROW),
	LOADING_PAGE(Material.WATCH),
	NEXT_PAGE(Material.SHEARS),
	PLAY_LEVEL_OPTIONS(Material.ENDER_CHEST),
	PREVIOUS_PAGE(Material.RAW_FISH), 
	SEARCH(Material.COMPASS), 
	SORT(Material.RAILS);

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

	@Override
	public ItemStackBuilder getBuilder() {
		return builder;
	}

	public String getDisplayName() {
		return builder.getDisplayName() != null ? builder.getDisplayName() : name();
	}

	public ItemStack getItem() {
		return builder.build();
	}

	@Override
	public String getName() {
		return name();
	}

	@Override
	public String getTranslationKeyBase() {
		return "menu.general-item";
	}

}
