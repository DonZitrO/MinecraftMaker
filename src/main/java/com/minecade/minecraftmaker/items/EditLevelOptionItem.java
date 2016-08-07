package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum EditLevelOptionItem implements TranslatableItem {

	SAVE(Material.BOOK_AND_QUILL),
	PLAY(Material.FIREWORK),
	CONFIG(Material.WORKBENCH),
	TOOLS(Material.END_CRYSTAL),
	INVITE(Material.SKULL_ITEM, 1, (short) 3),
	PUBLISH(Material.ITEM_FRAME),
	EXIT(Material.TRAP_DOOR);

	private final ItemStackBuilder builder;

	private EditLevelOptionItem(Material material) {
		this(material, 1);
	}

	private EditLevelOptionItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private EditLevelOptionItem(Material material, int amount, short data) {
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
		return "menu.edit-level-options";
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
