package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.minecade.mcore.util.BukkitUtils.verifyPrimaryThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.data.Rank;
import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.ItemBuilder;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class PlayerLevelsMenu extends AbstractDisplayableLevelMenu {

	private static Map<UUID, PlayerLevelsMenu> userLevelBrowserMenuMap = new HashMap<>();

	public static PlayerLevelsMenu getInstance(MinecraftMakerPlugin plugin, UUID viewerId) {
		checkNotNull(plugin);
		checkNotNull(viewerId);
		PlayerLevelsMenu menu = userLevelBrowserMenuMap.get(viewerId);
		if (menu == null) {
			menu = new PlayerLevelsMenu(plugin, viewerId);
		}
		userLevelBrowserMenuMap.put(viewerId, menu);
		return menu;
	}

	private final UUID viewerId;
	private final Map<Integer, MakerDisplayableLevel> slotLevelMap = new HashMap<>();
	private int levelCount = 0;

	private PlayerLevelsMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin);
		this.viewerId = viewerId;
		init();
	}

	@Override
	public void disable() {
		super.disable();
		userLevelBrowserMenuMap.remove(getViewerId());
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.player-levels.title";
	}

	@Override
	protected int getTotalItemsCount() {
		return levelCount;
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	@Override
	public boolean isShared() {
		return false;
	}

	public void loadLevelsCallback(int totalLevelCount, Collection<MakerDisplayableLevel> levels) {
		verifyPrimaryThread();
		this.levelCount = totalLevelCount;
		update(levels);
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, slot, clickType);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		MakerDisplayableLevel clickedLevel = slotLevelMap.get(slot);
		if (clickedLevel != null) {
			if (ClickType.LEFT.equals(clickType)) {
				if (clickedLevel.isPublished()) {
					if (!mPlayer.canCreateLevel()) {
						mPlayer.sendMessage("level.create.error.unpublished-limit", mPlayer.getUnpublishedLevelsCount());
						mPlayer.sendMessage("level.create.error.unpublished-limit.publish-delete");
						if (!mPlayer.hasRank(Rank.TITAN)) {
							mPlayer.sendMessage("upgrade.rank.increase.limits.or");
							mPlayer.sendMessage("upgrade.rank.unpublished.limits");
						}
						return MenuClickResult.CANCEL_CLOSE;
					}
					plugin.getController().copyAndLoadLevelForEditingBySerial(mPlayer, clickedLevel.getLevelSerial());
				} else {
					plugin.getController().loadLevelForEditingBySerial(mPlayer, clickedLevel.getLevelSerial());
				}
				return MenuClickResult.CANCEL_CLOSE;
			}
			if (ClickType.RIGHT.equals(clickType)) {
				if (clickedLevel.isPublished() && !clickedLevel.isUnpublished()) {
					plugin.getController().unpublishLevel(mPlayer, clickedLevel.getLevelSerial());
				} else {
					plugin.getController().deleteLevel(mPlayer, clickedLevel.getLevelSerial());
				}
				return MenuClickResult.CANCEL_CLOSE;
			}
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

	@Override
	public void update() {
		verifyPrimaryThread();
		update(null);
		plugin.getDatabaseAdapter().loadDisplayableLevelsPageByAuthorIdAsync(this, getPageOffset(currentPage), ITEMS_PER_PAGE);
	}

	@Override
	public void update(Collection<MakerDisplayableLevel> currentPageLevels) {
		verifyPrimaryThread();
		slotLevelMap.clear();
		updatePaginationItems();
		for (int j = 10; j < 44; j++) {
			if (isItemSlot(j)) {
				items[j] = getGlassPane();
			}
		}

		int i = 10;
		if (currentPageLevels != null && currentPageLevels.size() > 0) {
			levelSlots: for (MakerDisplayableLevel level : currentPageLevels) {
				while (!isItemSlot(i)) {
					i++;
					if (i >= items.length) {
						break levelSlots;
					}
				}
				ItemStack item = getLevelItem(level);
				if (item != null) {
					slotLevelMap.put(i, level);
					items[i] = item;
				} else {
					items[i] = getBlackGlassPane();
				}
				i++;
			}
		}
		for (; i < items.length; i++) {
			if (isItemSlot(i)) {
				items[i] = getBlackGlassPane();
			}
		}
		inventory.setContents(items);
	}

	@Override
	protected ItemStack getLevelItem(MakerDisplayableLevel level) {
		EntityType data = null;
		if (level.isUnpublished()) {
			data = EntityType.MUSHROOM_COW;
		} else if (level.isPublished()) {
			data = EntityType.CREEPER;
		} else {
			data = EntityType.SKELETON;
		}
		ItemBuilder builder = new ItemBuilder(data);
		builder.withDisplayName(plugin.getMessage("menu.level-browser.level.display-name", level.getLevelName()));
		List<String> lore = new ArrayList<>();
		lore.add(plugin.getMessage("menu.level-browser.level.serial", level.getLevelSerial()));
		lore.add(StringUtils.EMPTY);
		if (!level.isPublished()) {
			lore.add(plugin.getMessage("menu.level-browser.level.status-notpublished"));
			lore.add(StringUtils.EMPTY);
			lore.add(plugin.getMessage("menu.level-browser.level.click-to-edit"));
			lore.add(plugin.getMessage("menu.level-browser.level.right-click-to-delete"));
			//lore.add(plugin.getMessage("menu.player-levels.level.delete", level.getLevelSerial()));
		} else {
			lore.add(plugin.getMessage("menu.level-browser.level.likes", level.getLikes()));
			lore.add(plugin.getMessage("menu.level-browser.level.dislikes", level.getDislikes()));
			lore.add(plugin.getMessage("menu.level-browser.level.favorites", level.getFavs()));
			lore.add(plugin.getMessage("menu.level-browser.level.publish-date", level.getDatePublished()));
			lore.add(StringUtils.EMPTY);
			if (level.isUnpublished()) {
				lore.add(plugin.getMessage("menu.level-browser.level.status-unpublished"));
				lore.add(StringUtils.EMPTY);
				lore.add(plugin.getMessage("menu.level-browser.level.click-to-copy"));
					lore.add(plugin.getMessage("menu.level-browser.level.right-click-to-delete"));
				//lore.add(plugin.getMessage("menu.player-levels.level.delete", level.getLevelSerial()));
			} else {
				lore.add(plugin.getMessage("menu.level-browser.level.status-published"));
				lore.add(StringUtils.EMPTY);
				lore.add(plugin.getMessage("menu.level-browser.level.click-to-copy"));
				lore.add(plugin.getMessage("menu.level-browser.level.right-click-to-unpublish"));
				//lore.add(plugin.getMessage("menu.player-levels.level.unpublish", level.getLevelSerial()));
			}
		}
		builder.withLore(lore);
		return builder.build();
	}

}
