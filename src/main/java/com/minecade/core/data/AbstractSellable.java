package com.minecade.core.data;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

public abstract class AbstractSellable implements Sellable, ConfigurationSerializable {

	private final String name;

	private String displayName;
	private String translationKey;
	private String databaseColumnName;
	private Rank rank;
	private int price;
	private boolean oneTimePurchase;
	private boolean onlineStoreOnly;

	public AbstractSellable(String name) {
		super();
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getName() {
		return name;
	}

	public String getTranslationKey() {
		return translationKey;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public int getPrice() {
		return price;
	}

	@Override
	public Rank getRank() {
		return rank;
	}

	@Override
	public boolean isOneTimePurchase() {
		return oneTimePurchase;
	}

	@Override
	public boolean isOnlineStoreOnly() {
		return onlineStoreOnly;
	}

	@Override
	public void setPrice(int price) {
		this.price = price;
	}

	@Override
	public String getDatabaseColumnName() {
		return databaseColumnName;
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		map.put("name", name);
		if (getPrice() > 0) {
			map.put("price", price);
		}
		if (!Rank.GUEST.equals(getRank())) {
			map.put("rank", rank);
		}
		return map;
	}

}
