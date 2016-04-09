package com.minecade.minecraftmaker.items;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.I18NUtils;
import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.i18n.Translatable;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum GeneralMenuItem implements Translatable {

	EXIT_MENU(Material.ARROW),
	EDIT_LEVEL_OPTIONS(Material.ENDER_CHEST), 
	PLAY_LEVEL_OPTIONS(Material.ENDER_CHEST);

	private final ItemStackBuilder builder;

	private GeneralMenuItem(Material material) {
		this(material, 1);
	}

	private GeneralMenuItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private GeneralMenuItem(Material material, int amount, short data) {
		this.builder = new ItemBuilder(material, amount, data);
	}

	public ItemStack getItem() {
		return builder.build();
	}

	@Override
	public void translate(Internationalizable plugin) {
		String translationKey = String.format("menu.general-item.%s.display-name", name().toLowerCase().replace('_','-'));
		String displayName = plugin.getMessage(translationKey);
		if (displayName != translationKey) {
			builder.withDisplayName(displayName);
		}
		translationKey = String.format("menu.general-item.%s.lore", name().toLowerCase().replace('_','-'));
		List<String> translatedLore = I18NUtils.translateAndSplitSingleLine(plugin, translationKey);
		if (!translatedLore.isEmpty() && !translationKey.equals(translatedLore.get(0))) {
			builder.withLore(translatedLore);
		}
	}

	public String getDisplayName() {
		return builder.getDisplayName() != null ? builder.getDisplayName() : name();
	}

}
