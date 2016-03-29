package com.minecade.core.item;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import com.minecade.minecraftmaker.nms.NMSPotionUtils;

public class PotionBuilder extends Potion implements ItemStackBuilder {

	private int amount;
	private List<PotionEffect> potionEffects;
	private boolean linger;

	private String displayName;
	private List<String> lore;

	public PotionBuilder(PotionType type, int amount, int level) {
		super(type, level);
		this.amount = amount;
	}

	@Override
	public ItemStack build() {
		ItemStack is = NMSPotionUtils.toItemStack(this, amount);
		PotionMeta meta = (PotionMeta)is.getItemMeta();
		if (!StringUtils.isBlank(displayName)) {
			meta.setDisplayName(displayName);
		}
		if (lore != null && !lore.isEmpty()) {
			meta.setLore(lore);
		}
		if (potionEffects != null && !potionEffects.isEmpty()) {
			for (PotionEffect eff:potionEffects) {
				meta.addCustomEffect(eff, true);
			}
		}
		is.setItemMeta(meta);
		return is;
	}

	@Override
	public PotionBuilder extend() {
		return (PotionBuilder)super.extend();
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public PotionBuilder splash() {
		return (PotionBuilder)super.splash();
	}

	public PotionBuilder linger() {
		this.linger = true;
		return this;
	}

	public boolean isLinger() {
		return linger;
	}

	@Override
	public PotionBuilder withDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public PotionBuilder withLore(List<String> lore) {
		this.lore = lore;
		return this;
	}

	public PotionBuilder withPotionEffects(List<PotionEffect> potionEffects) {
		this.potionEffects = potionEffects;
		return this;
	}

}
