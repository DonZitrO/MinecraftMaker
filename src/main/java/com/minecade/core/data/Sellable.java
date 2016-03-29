package com.minecade.core.data;

import com.minecade.core.i18n.Translatable;

public interface Sellable extends Translatable {

	public String getName();

	public int getPrice();

	public Rank getRank();

	public boolean isOneTimePurchase();

	public boolean isOnlineStoreOnly();

	public void setPrice(int price);

	public String getDatabaseColumnName();

}
