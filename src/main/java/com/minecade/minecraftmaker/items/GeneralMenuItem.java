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
	NEXT_PAGE("15f49744-9b61-46af-b1c3-71c6261a0d0e", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWI2ZjFhMjViNmJjMTk5OTQ2NDcyYWVkYjM3MDUyMjU4NGZmNmY0ZTgzMjIxZTU5NDZiZDJlNDFiNWNhMTNiIn19fQ=="),
	PLAY_LEVEL_OPTIONS(Material.ENDER_CHEST),
	PREVIOUS_PAGE("69b9a08d-4e89-4878-8be8-551caeacbf2a", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2ViZjkwNzQ5NGE5MzVlOTU1YmZjYWRhYjgxYmVhZmI5MGZiOWJlNDljNzAyNmJhOTdkNzk4ZDVmMWEyMyJ9fX0="),
	SEARCH(Material.COMPASS),
	SORT(Material.HOPPER);

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

	private GeneralMenuItem(String uniqueId, String texture){
		this.builder = new ItemBuilder(uniqueId, texture);
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
