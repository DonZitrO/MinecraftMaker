package com.minecade.core.inventory;

import java.lang.reflect.Field;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;

public class ActiveItemEnchantment extends EnchantmentWrapper {

	private static Enchantment activeEnchantment;

	public static Enchantment getActiveItemEnchantment() {
		if (null == activeEnchantment) {
			activeEnchantment = new ActiveItemEnchantment(120);
			try {
				Field f = Enchantment.class.getDeclaredField("acceptingNew");
				f.setAccessible(true);
				f.set(null, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				EnchantmentWrapper.registerEnchantment(activeEnchantment);
				return activeEnchantment;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return activeEnchantment;
	}

	private ActiveItemEnchantment(int id) {
		super(id);
	}

	@Override
	public boolean canEnchantItem(ItemStack item) {
		return true;
	}

	@Override
	public boolean conflictsWith(Enchantment other) {
		return false;
	}

	@Override
	public EnchantmentTarget getItemTarget() {
		return null;
	}

	@Override
	public int getMaxLevel() {
		return 10;
	}

	@Override
	public String getName() {
		return "Active";
	}

	@Override
	public int getStartLevel() {
		return 1;
	}

}
