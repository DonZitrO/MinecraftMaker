package com.minecade.core.i18n;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemStackBuilder;

public interface TranslatableItem extends Translatable {

	ItemStackBuilder getBuilder();

	@Override
	default String getDisplayName() {
		return getBuilder().getDisplayName() != null ? getBuilder().getDisplayName() : getName();
	}

	default ItemStack getItem() {
		return getBuilder().build();
	}

	@Override
	default void setDisplayName(String displayName) {
		getBuilder().withDisplayName(displayName);
	}

	@Override
	default void translate(Internationalizable plugin) {
		String translationKey = String.format("%s.%s.display-name", getTranslationKeyBase().toLowerCase(), getName().toLowerCase().replace('_', '-'));
		String displayName = plugin.getMessage(translationKey);
		if (displayName != translationKey) {
			setDisplayName(displayName);
		}
		translationKey = String.format("%s.%s.lore", getTranslationKeyBase().toLowerCase(), getName().toLowerCase().replace('_', '-'));
		List<String> translatedLore = I18NUtils.translateAndSplitSingleLine(plugin, translationKey);
		if (!translatedLore.isEmpty() && !translationKey.equals(translatedLore.get(0))) {
			getBuilder().withLore(translatedLore);
		}
	}

}
