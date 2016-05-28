package com.minecade.core.item;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

public class PotionBuilder implements ItemStackBuilder {

	private PotionType type;
	private int amount;
	private List<PotionEffect> potionEffects;
	private boolean lingering;
	private boolean splash;
	private boolean extended;
	private boolean upgraded;

	private String displayName;
	private List<String> lore;

	public PotionBuilder(PotionType type, int amount) {
		this.type = type;
		this.amount = amount;
	}

	@Override
	public ItemStack build() {
		ItemStack item = null;
		if (isSplash()) {
			item = new ItemStack(Material.SPLASH_POTION, amount);
		} else if (isLingering()) {
			item = new ItemStack(Material.LINGERING_POTION, amount);
		} else {
			item = new ItemStack(Material.POTION, amount);
		}
		PotionData data = new PotionData(type, extended, upgraded);
		PotionMeta meta = (PotionMeta)item.getItemMeta();
		meta.setBasePotionData(data);
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
		item.setItemMeta(meta);
		return item;
	}

	public PotionBuilder extend() {
		extended = true;
		return this;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	public boolean isExtended() {
		return extended;
	}

	public boolean isLingering() {
		return lingering;
	}

	public boolean isSplash() {
		return splash;
	}

	public boolean isUpgraded() {
		return upgraded;
	}

	public PotionBuilder linger() {
		lingering = true;
		return this;
	}

	public PotionBuilder splash() {
		splash = true;
		return this;
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
