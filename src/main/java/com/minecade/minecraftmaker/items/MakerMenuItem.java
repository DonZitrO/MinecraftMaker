package com.minecade.minecraftmaker.items;

import org.bukkit.Material;

import com.minecade.mcore.i18n.TranslatableItem;
import com.minecade.mcore.item.ItemBuilder;
import com.minecade.mcore.item.ItemStackBuilder;

public enum MakerMenuItem implements TranslatableItem {

	CHECK_TEMPLATE_OPTIONS(Material.ENDER_CHEST),
	CONTINUE_EDITING(Material.EMPTY_MAP),
	EDIT_LEVEL_OPTIONS(Material.ENDER_CHEST),
	EDITOR_PLAY_LEVEL_OPTIONS(Material.ENDER_CHEST),
	GUEST_EDIT_LEVEL_OPTIONS(Material.ENDER_CHEST),
	PLAY_LEVEL_OPTIONS(Material.ENDER_CHEST),
	STEVE_LEVEL_OPTIONS(Material.ENDER_CHEST);

	private final ItemStackBuilder builder;

	private MakerMenuItem(Material material) {
		this(material, 1);
	}

	private MakerMenuItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private MakerMenuItem(Material material, int amount, short data) {
		this.builder = new ItemBuilder(material, amount, data);
	}

	private MakerMenuItem(String uniqueId, String texture){
		this.builder = new ItemBuilder(uniqueId, texture);
	}

	@Override
	public ItemStackBuilder getBuilder() {
		return builder;
	}

	@Override
	public String getName() {
		return name();
	}

	@Override
	public String getTranslationKeyBase() {
		return "menu.maker-item";
	}

}
