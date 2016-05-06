package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum EditorPlayLevelOptionItem implements TranslatableItem {

	EDIT(Material.EMPTY_MAP),
	EXIT(Material.TRAP_DOOR);

	private final ItemStackBuilder builder;

	private EditorPlayLevelOptionItem(Material material) {
		this(material, 1);
	}

	private EditorPlayLevelOptionItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private EditorPlayLevelOptionItem(Material material, int amount, short data) {
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
		return "menu.editor-play-options";
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
