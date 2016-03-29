package com.minecade.core.item;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public interface ItemStackBuilder {

	public ItemStack build();

	public ItemStackBuilder withDisplayName(String displayName);

	public ItemStackBuilder withLore(List<String> lore);

	public String getDisplayName();

}
