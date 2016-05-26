package com.minecade.core.i18n;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.ChatColor;

public class I18NUtils {

	public static List<String> translateAndSplitSingleLine(Internationalizable plugin, String lineKey) {
		return translateAndSplitSingleLine(plugin, lineKey, "\n");
	}

	public static List<String> translateAndSplitSingleLine(Internationalizable plugin, String lineKey, String lineSeparator) {
		return translateAndSplitSingleLine(plugin, lineKey, lineSeparator, '&');
	}

	public static List<String> translateAndSplitSingleLine(Internationalizable plugin, String lineKey, String lineSeparator, char alternateColorCode) {
		if (StringUtils.isNotBlank(lineKey)) {
			List<String> lore = new ArrayList<>();
			for (String translatedLine : StringUtils.split(ChatColor.translateAlternateColorCodes(alternateColorCode, plugin.getMessage(lineKey)), lineSeparator)) {
				// Bukkit.getLogger().info(String.format("[DEBUG] | I18NUtils.translateAndSplitSingleLine - line: [%s]", translatedLine));
				if (translatedLine != null) {
					lore.add(translatedLine);
				}
			}
			return lore;
		}
		return new ArrayList<String>();
	}

	public static String formatEnum(Enum<?> e) {
		return WordUtils.capitalizeFully(e.name().replaceAll("_", " ").trim());
	}

	private I18NUtils() {
		super();
	}

}
