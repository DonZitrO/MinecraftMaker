package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.i18n.TranslatableItem;
import com.minecade.mcore.item.ItemBuilder;
import com.minecade.mcore.item.ItemStackBuilder;

public enum SteveLevelClearOptionItem implements TranslatableItem {

	CONTINUE(Material.SKULL_ITEM, 1, (short) 3),
	LIKE(Material.DIAMOND),
	DISLIKE(Material.POISONOUS_POTATO),
	FAVORITE(Material.NETHER_STAR),
	EXIT(Material.TRAP_DOOR);

	private final ItemStackBuilder builder;

	private SteveLevelClearOptionItem(Material material) {
		this(material, 1);
	}

	private SteveLevelClearOptionItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private SteveLevelClearOptionItem(Material material, int amount, short data) {
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
		return "menu.steve-level-clear-options";
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
