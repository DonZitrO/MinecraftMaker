package com.minecade.core.item;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class SkullItemBuilder implements ItemStackBuilder {

	private String displayName;
	private List<String> lore;

	private String owner;

	public SkullItemBuilder() {
		this(null);
	}

	public SkullItemBuilder(String owner) {
		this.owner = owner;
	}

	@Override
	public ItemStack build() {
		ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
		SkullMeta meta = (SkullMeta)skull.getItemMeta();
		if (owner != null) {
			meta.setOwner(owner);
		}
		if (!StringUtils.isBlank(displayName)) {
			meta.setDisplayName(displayName);
		}
		if (lore != null && !lore.isEmpty()) {
			if (StringUtils.isNotBlank(lore.get(0).trim())) {
				lore.add(0, StringUtils.EMPTY);
			}
			meta.setLore(lore);
		}
		skull.setItemMeta(meta);
		return skull;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	public SkullItemBuilder withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public SkullItemBuilder withLore(List<String> lore) {
		this.lore = lore;
		return this;
	}

}
