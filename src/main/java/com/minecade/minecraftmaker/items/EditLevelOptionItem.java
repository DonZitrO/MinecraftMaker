package com.minecade.minecraftmaker.items;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.I18NUtils;
import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.i18n.Translatable;
import com.minecade.core.item.ItemBuilder;
import com.minecade.core.item.ItemStackBuilder;

public enum EditLevelOptionItem implements Translatable {

	PLAY(Material.LEVER),
	SAVE(Material.BOOK_AND_QUILL),
	PUBLISH(Material.ITEM_FRAME),
	EXIT(Material.TRAP_DOOR);

	private final ItemStackBuilder builder;

	private EditLevelOptionItem(Material material) {
		this(material, 1);
	}

	private EditLevelOptionItem(Material material, int amount) {
		this(material, amount, (short) 0);
	}

	private EditLevelOptionItem(Material material, int amount, short data) {
		this.builder = new ItemBuilder(material, amount, data);
	}

	public ItemStack getItem() {
		return builder.build();
	}

	@Override
	public void translate(Internationalizable plugin) {
		String translationKey = String.format("menu.edit-level-options.%s.display-name", name().toLowerCase().replace('_','-'));
		String displayName = plugin.getMessage(translationKey);
		if (displayName != translationKey) {
			builder.withDisplayName(displayName);
		}
		translationKey = String.format("menu.edit-level-options.%s.lore", name().toLowerCase().replace('_','-'));
		List<String> translatedLore = I18NUtils.translateAndSplitSingleLine(plugin, translationKey);
		if (!translatedLore.isEmpty() && !translationKey.equals(translatedLore.get(0))) {
			builder.withLore(translatedLore);
		}
	}

	public String getDisplayName() {
		return builder.getDisplayName() != null ? builder.getDisplayName() : name();
	}

}
