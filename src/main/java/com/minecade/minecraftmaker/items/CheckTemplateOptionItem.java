package com.minecade.minecraftmaker.items;

import org.bukkit.Material;

import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum CheckTemplateOptionItem implements TranslatableItem {

	EXIT(Material.TRAP_DOOR);

	private final ItemStackBuilder builder;

	private CheckTemplateOptionItem(Material material) {
		this(material, 1);
	}

	private CheckTemplateOptionItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private CheckTemplateOptionItem(Material material, int amount, short data) {
		this.builder = new ItemBuilder(material, amount, data);
	}

	@Override
	public String getTranslationKeyBase() {
		return "menu.check-template-options";
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
