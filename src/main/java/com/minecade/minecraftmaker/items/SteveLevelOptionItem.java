package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum SteveLevelOptionItem implements TranslatableItem {

	RESTART(Material.FLINT_AND_STEEL),
	SKIP(Material.FEATHER),
	LIKE(Material.DIAMOND),
	DISLIKE(Material.POISONOUS_POTATO),
	FAVORITE(Material.NETHER_STAR),
	EXIT(Material.TRAP_DOOR);

	private final ItemStackBuilder builder;

	private SteveLevelOptionItem(Material material) {
		this(material, 1);
	}

	private SteveLevelOptionItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private SteveLevelOptionItem(Material material, int amount, short data) {
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
		return "menu.steve-level-options";
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
