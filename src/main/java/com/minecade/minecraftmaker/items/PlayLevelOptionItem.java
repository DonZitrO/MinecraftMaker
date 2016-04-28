package com.minecade.minecraftmaker.items;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.I18NUtils;
import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum PlayLevelOptionItem implements TranslatableItem {

	RESTART(Material.FLINT_AND_STEEL),
	LIKE(Material.DIAMOND),
	DISLIKE(Material.POISONOUS_POTATO),
	FAVORITE(Material.NETHER_STAR),
	EXIT(Material.TRAP_DOOR);

	private final ItemStackBuilder builder;

	private PlayLevelOptionItem(Material material) {
		this(material, 1);
	}

	private PlayLevelOptionItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private PlayLevelOptionItem(Material material, int amount, short data) {
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
		return "menu.play-level-options";
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
