package com.minecade.minecraftmaker.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.i18n.TranslatableItem;
import com.minecade.mcore.item.ItemBuilder;
import com.minecade.mcore.item.ItemStackBuilder;

public enum MakerLobbyItem implements TranslatableItem {

	SERVER_BROWSER(Material.WATCH),
	STEVE_CHALLENGE(Material.SKULL_ITEM, 1, (short) 3),
	CREATE_LEVEL(Material.EMPTY_MAP),
	PLAYER_LEVELS(Material.NAME_TAG),
	LEVEL_BROWSER(Material.BOOKSHELF),
	SPECTATE(Material.EYE_OF_ENDER),
	QUIT(Material.TNT);

	private final ItemStackBuilder builder;

	private MakerLobbyItem(Material material) {
		this(material, 1);
	}

	private MakerLobbyItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private MakerLobbyItem(Material material, int amount, short data) {
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
		return "lobby.item";
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
