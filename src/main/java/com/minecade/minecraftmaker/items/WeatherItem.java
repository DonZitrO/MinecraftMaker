package com.minecade.minecraftmaker.items;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Maps;
import com.minecade.core.i18n.TranslatableItem;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum WeatherItem implements TranslatableItem {

	RAINY(Material.WATER_BUCKET, WeatherType.DOWNFALL),
	SUNNY(Material.DOUBLE_PLANT, WeatherType.CLEAR);

	private final static Map<Material, WeatherItem> BY_MATERIAL = Maps.newHashMap();

	static {
		for (WeatherItem item : values()) {
			BY_MATERIAL.put(item.getItem().getType(), item);
		}
	}

	public static WeatherItem getWeatherItem(final Material material) {
		return BY_MATERIAL.get(material);
	}

	private final ItemStackBuilder builder;

	protected WeatherType weatherType;

	private WeatherItem(Material material, WeatherType weatherType) {
		this(material, 1);
		this.weatherType = weatherType;
	}

	private WeatherItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private WeatherItem(Material material, int amount, short data) {
		this.builder = new ItemBuilder(material, amount, data);
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
		return "menu.weather";
	}

	public WeatherType getWeatherType() {
		return this.weatherType;
	}

	@Override
	public void setDisplayName(String displayName) {
		getBuilder().withDisplayName(displayName);
	}

}
