package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.i18n.Internationalizable;
import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.level.AbstractMakerLevel;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class PlayerLevelsMenu extends AbstractDisplayableLevelMenu {

	private static final long MIN_TIME_BETWEEN_REFRESHES_MILLIS = 300000;

	private static Map<UUID, ItemStack> levelItems = new HashMap<>();
	private static Map<UUID, PlayerLevelsMenu> userLevelBrowserMenuMap = new HashMap<>();

	private static void addLevelItem(Internationalizable plugin, MakerDisplayableLevel level) {
		levelItems.put(level.getLevelId(), getLevelItem(plugin, level));
	}

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

	@Override
	public String getTitleKey(String modifier) {
		return "menu.player-levels.title";
	}

	public static void removeLevelFromViewer(MakerDisplayableLevel level) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		PlayerLevelsMenu menu = userLevelBrowserMenuMap.get(level.getAuthorId());
		if (menu != null) {
			menu.removeOwnedLevel(level);
		}
	}

	private static void removeLevelItem(UUID levelId) {
		levelItems.remove(levelId);
	}
	private final TreeSet<MakerDisplayableLevel> ownedLevelsBySerial = new TreeSet<MakerDisplayableLevel>((AbstractMakerLevel l1, AbstractMakerLevel l2) -> Long.valueOf(l1.getLevelSerial()).compareTo(Long.valueOf(l2.getLevelSerial())));
	private final UUID viewerId;

	private long nextAllowedRefreshMillis;

	private PlayerLevelsMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin, 54);
		this.viewerId = viewerId;
		init();
	}

	@Override
	public void disable() {
		super.disable();
		userLevelBrowserMenuMap.remove(getViewerId());
	}

	@Override
	protected ItemStack getLevelItem(MakerDisplayableLevel level) {
		return levelItems.get(level.getLevelId());
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	private void init() {
		for (int i = 0; i < inventory.getSize(); i++) {
			items[i] = getGlassPane();
		}
		items[51] = GeneralMenuItem.EXIT_MENU.getItem();
	}

	@Override
	public boolean isShared() {
		return false;
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		ItemStack clickedItem = inventory.getItem(slot);
		if (clickedItem.getType().equals(Material.MONSTER_EGG)) {
			String serial = ItemUtils.getLoreLine(clickedItem, 1);
			if (StringUtils.isBlank(serial) || !StringUtils.isNumeric(serial)) {
				Bukkit.getLogger().severe(String.format("PlayerLevelsMenu.onClick - unable to get level serial from lore: [%s]", serial));
				return MenuClickResult.CANCEL_UPDATE;
			}
			plugin.getController().loadLevelForEditingBySerial(mPlayer, Long.valueOf(serial));
			return MenuClickResult.CANCEL_CLOSE;
		}
		return MenuClickResult.CANCEL_UPDATE;
	}

	public void removeOwnedLevel(MakerDisplayableLevel level) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		removeLevelItem(level.getLevelId());
		ownedLevelsBySerial.remove(level);
		update(ownedLevelsBySerial);
	}

	public boolean shouldRefreshAgain() {
		long currentTimeMillis = System.currentTimeMillis();
		if (nextAllowedRefreshMillis < currentTimeMillis) {
			nextAllowedRefreshMillis = currentTimeMillis + MIN_TIME_BETWEEN_REFRESHES_MILLIS;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void update() {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		update(ownedLevelsBySerial);
		if (shouldRefreshAgain()) {
			plugin.getDatabaseAdapter().loadUnpublishedLevelsByAuthorIdAsync(this);
		}
	}

	public void updateOwnedLevel(MakerDisplayableLevel level) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		addLevelItem(plugin, level);
		ownedLevelsBySerial.add(level);
		update(ownedLevelsBySerial);
	}

	public void updateOwnedLevels(Collection<MakerDisplayableLevel> levels) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		for (MakerDisplayableLevel level : levels) {
			addLevelItem(plugin, level);
		}
		ownedLevelsBySerial.addAll(levels);
		update(ownedLevelsBySerial);
	}

}
