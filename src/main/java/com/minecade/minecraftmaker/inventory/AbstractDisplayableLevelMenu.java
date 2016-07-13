package com.minecade.minecraftmaker.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.item.ItemBuilder;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractDisplayableLevelMenu extends AbstractMakerMenu {

	protected static final int LEVELS_PER_PAGE = 28;

	protected static boolean isLevelSlot(int index) {
		if (index > 9 && index < 17) {
			return true;
		}
		if (index > 18 && index < 26) {
			return true;
		}
		if (index > 27 && index < 35) {
			return true;
		}
		if (index > 36 && index < 44) {
			return true;
		}
		return false;
	}

	protected static ItemStack getLevelItem(Internationalizable plugin, MakerDisplayableLevel level) {
		EntityType data = EntityType.GHAST;
		if (level.getAuthorRank() != null) {
			switch (level.getAuthorRank()) {
			case GUEST:
				break;
			case VIP:
				data = EntityType.SLIME;
				break;
			case PRO:
				data = EntityType.SQUID;
				break;
			case ELITE:
				data = EntityType.BLAZE;
				break;
			case TITAN:
			case LEGENDARY:
				data = EntityType.MAGMA_CUBE;
				break;
			default:
				data = EntityType.CHICKEN;
				break;
			}
		}
		ItemBuilder builder = new ItemBuilder(data);
		builder.withDisplayName(plugin.getMessage("menu.level-browser.level.display-name", level.getLevelName()));
		List<String> lore = new ArrayList<>();
		lore.add(plugin.getMessage("menu.level-browser.level.serial", level.getLevelSerial()));
		lore.add(StringUtils.EMPTY);
		lore.add(plugin.getMessage("menu.level-browser.level.created-by", level.getAuthorName()));
		if (level.getDatePublished() != null) {
			lore.add(plugin.getMessage("menu.level-browser.level.created-by-rank", level.getAuthorRank().getDisplayName()));
			lore.add(StringUtils.EMPTY);
			lore.add(plugin.getMessage("menu.level-browser.level.likes", level.getLikes()));
			lore.add(plugin.getMessage("menu.level-browser.level.dislikes", level.getDislikes()));
			lore.add(plugin.getMessage("menu.level-browser.level.favorites", level.getFavs()));
			lore.add(plugin.getMessage("menu.level-browser.level.publish-date", level.getDatePublished()));
		}
		lore.add(StringUtils.EMPTY);
		builder.withLore(lore);
		return builder.build();
	}

	protected ItemStack getLevelItem(MakerDisplayableLevel level) {
		return getLevelItem(plugin, level);
	}

	public AbstractDisplayableLevelMenu(MinecraftMakerPlugin plugin, int size) {
		super(plugin, size);
	}

	public void update(Collection<MakerDisplayableLevel> currentPageLevels) {

		for (int j = 10; j < 44; j++) {
			if (isLevelSlot(j)) {
				items[j] = getGlassPane();
			}
		}

		int i = 10;
		levelSlots: for (MakerDisplayableLevel level : currentPageLevels) {
			while (!isLevelSlot(i)) {
				i++;
				if (i >= items.length) {
					break levelSlots;
				}
			}
			ItemStack item = getLevelItem(level);
			if (item != null) {
				items[i] = item;
			} else {
				items[i] = getBlackGlassPane();
			}
			i++;
		}
		for (; i < items.length; i++) {
			if (isLevelSlot(i)) {
				items[i] = getBlackGlassPane();
			}
		}
		inventory.setContents(items);
	}

}
