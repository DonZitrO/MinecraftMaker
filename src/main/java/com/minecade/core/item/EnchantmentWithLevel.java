package com.minecade.core.item;

import org.bukkit.enchantments.Enchantment;

public class EnchantmentWithLevel {

	public static EnchantmentWithLevel getInstance(Enchantment enchantment, int level) {
		return new EnchantmentWithLevel(enchantment, level);
	}

	public static EnchantmentWithLevel getInstance(Enchantment enchantment) {
		return getInstance(enchantment, 1);
	}

	private final Enchantment enchantment;
	private final int level;

	public EnchantmentWithLevel(Enchantment enchantment, int level) {
		this.enchantment = enchantment;
		this.level = level;
	}

	public Enchantment getEnchantment() {
		return enchantment;
	}

	public int getLevel() {
		return level;
	}

}
