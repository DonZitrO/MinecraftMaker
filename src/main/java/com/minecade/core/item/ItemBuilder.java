package com.minecade.core.item;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

public class ItemBuilder implements ItemStackBuilder, Cloneable {

	private final Material material;
	private final int amount;
	private final short data;

	private String displayName;
	private List<String> lore;

	private List<EnchantmentWithLevel> enchantments;

	public ItemBuilder(Material material) {
		this(material, 1);
	}

	public ItemBuilder(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	public ItemBuilder(Material material, int amount, short data) {
		this.material = material;
		this.amount = amount;
		this.data = data;
	}

	@SuppressWarnings("deprecation")
	public ItemBuilder(MaterialData materialData) {
		this(materialData.getItemType(), 1, (short)materialData.getData());
	}

	@SuppressWarnings("deprecation")
	public ItemBuilder(MaterialData materialData, int amount) {
		this(materialData.getItemType(), amount, (short)materialData.getData());
	}

	public ItemStack build() {
		ItemStack item = new ItemStack(material, amount, data);
		ItemMeta meta = item.getItemMeta();
		if (!StringUtils.isBlank(displayName)) {
			meta.setDisplayName(displayName);
		}
		if (lore != null && !lore.isEmpty()) {
			meta.setLore(lore);
		}
		item.setItemMeta(meta);
		if (enchantments != null && !enchantments.isEmpty()) {
			for (EnchantmentWithLevel ench : enchantments) {
				item.addUnsafeEnchantment(ench.getEnchantment(), ench.getLevel());
			}
		}
		return item;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	public ItemBuilder withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public ItemBuilder withLore(List<String> lore) {
		this.lore = lore;
		return this;
	}

	public ItemBuilder withEnchantments(List<EnchantmentWithLevel> enchantments) {
		this.enchantments = enchantments;
		return this;
	}

	public ItemBuilder withEnchantment(EnchantmentWithLevel enchantment) {
		if (this.enchantments == null) {
			this.enchantments = new ArrayList<EnchantmentWithLevel>();
		}
		this.enchantments.add(enchantment);
		return this;
	}

	@Override
	public ItemBuilder clone() {
		return new ItemBuilder(material, amount, data).withDisplayName(displayName).withLore(lore != null ? new ArrayList<>(lore) : null).withEnchantments(enchantments != null ? new ArrayList<>(enchantments) : null);
	}

}
