package com.minecade.minecraftmaker.items;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Maps;
import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum TimeItem implements TranslatableItem {

	NOON(6000),
	MIDNIGHT(18000);

	private final static Map<String, TimeItem> BY_DISPLAY_NAME = Maps.newHashMap();

	public static TimeItem getTimeItemByDisplayName(final String displayName) {
		return BY_DISPLAY_NAME.get(displayName);
	}

	private final ItemStackBuilder builder;

	private long time;

	private TimeItem(long time) {
		this(Material.WATCH, 1);
		this.time = time;
	}

	private TimeItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private TimeItem(Material material, int amount, short data) {
		this.builder = new ItemBuilder(material, amount, data);
	}

	public long getTime(){
		return this.time;
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
		return "menu.time";
	}

	@Override
	public void setDisplayName(String displayName) {
		getBuilder().withDisplayName(displayName);
		BY_DISPLAY_NAME.put(displayName, this);
	}

}
