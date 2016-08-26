package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.minecade.mcore.util.BukkitUtils.verifyPrimaryThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.data.Rank;
import com.minecade.mcore.item.ItemBuilder;
import com.minecade.minecraftmaker.data.MakerUnlockable;
import com.minecade.minecraftmaker.level.MakerLevelTemplate;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelTemplatesMenu extends AbstractPaginatedMenu {

	private static Map<UUID, LevelTemplatesMenu> userLevelTemplateMenuMap = new HashMap<>();
	private static List<MakerLevelTemplate> templatesByName = new LinkedList<MakerLevelTemplate>();

	public static LevelTemplatesMenu getInstance(MinecraftMakerPlugin plugin, UUID viewerId) {
		checkNotNull(plugin);
		checkNotNull(viewerId);
		LevelTemplatesMenu menu = userLevelTemplateMenuMap.get(viewerId);
		if (menu == null) {
			menu = new LevelTemplatesMenu(plugin, viewerId);
		}
		userLevelTemplateMenuMap.put(viewerId, menu);
		return menu;
	}

	public static void updateTemplates(Collection<MakerLevelTemplate> templates) {
		verifyPrimaryThread();
		templatesByName.clear();
		templatesByName.addAll(templates);
	}

	private final UUID viewerId;
	private final Map<Integer, MakerLevelTemplate> slotTemplateMap = new HashMap<>();

	private int templatesCount = 0;

	private LevelTemplatesMenu(MinecraftMakerPlugin plugin, UUID viewerId) {
		super(plugin);
		this.viewerId = viewerId;
		init();
	}

	private boolean canUseUnlockableTemplate(MakerPlayer mPlayer, UUID templateId) {
		return mPlayer != null && mPlayer.hasUnlockable(MakerUnlockable.getUnlockableByUUID(templateId));
	}

	private boolean canUseVipTemplate(MakerPlayer mPlayer) {
		return mPlayer != null && mPlayer.hasRank(Rank.VIP);
	}

	@Override
	public void disable() {
		super.disable();
		userLevelTemplateMenuMap.remove(getViewerId());
	}

	protected ItemStack getTemplateItem(MakerLevelTemplate template) {
		EntityType data = null;
		List<String> unlockableLore =  new ArrayList<>();
		unlockableLore.add(StringUtils.EMPTY);
		MakerPlayer mPlayer = plugin.getController().getPlayer(getViewerId());
		if (template.isVipOnly()) {
			data = EntityType.CREEPER;
			unlockableLore.add(plugin.getMessage("menu.level-template.vip-only"));
			unlockableLore.add(StringUtils.EMPTY);
			if (canUseVipTemplate(mPlayer)) {
				unlockableLore.add(plugin.getMessage("menu.level-template.click-to-use"));
			} else {
				unlockableLore.add(plugin.getMessage("menu.level-template.upgrade-to-use"));
			}
		} else if (!template.isFree()) {
			data = EntityType.HORSE;
			if (canUseUnlockableTemplate(mPlayer, template.getTemplateId())) {
				unlockableLore.add(plugin.getMessage("unlockable.unlocked"));
				unlockableLore.add(StringUtils.EMPTY);
				unlockableLore.add(plugin.getMessage("menu.level-template.click-to-use"));
			} else {
				unlockableLore.add(plugin.getMessage("unlockable.cost", MakerLevelTemplate.DEFAULT_COIN_COST));
				unlockableLore.add(StringUtils.EMPTY);
				unlockableLore.add(plugin.getMessage("unlockable.click-to-unlock"));
				unlockableLore.add(plugin.getMessage("menu.level-template.right-click-to-check"));
			}
		} else {
			data = EntityType.SKELETON;
			unlockableLore.add(plugin.getMessage("unlockable.free"));
			unlockableLore.add(StringUtils.EMPTY);
			unlockableLore.add(plugin.getMessage("menu.level-template.click-to-use"));
		}
		ItemBuilder builder = new ItemBuilder(data);
		builder.withDisplayName(plugin.getMessage("menu.level-template.template.display-name", template.getTemplateName()));
		List<String> lore = new ArrayList<>();
		lore.add(StringUtils.EMPTY);
		lore.add(plugin.getMessage("menu.level-template.template.author-name", template.getAuthorName()));
		lore.add(StringUtils.EMPTY);
		lore.addAll(unlockableLore);
		builder.withLore(lore);
		return builder.build();
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-template.title";
	}

	@Override
	protected int getTotalItemsCount() {
		return templatesCount;
	}

	public UUID getViewerId() {
		return this.viewerId;
	}

	@Override
	public boolean isShared() {
		return false;
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, slot, clickType);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		}
		MakerLevelTemplate clickedTemplate = slotTemplateMap.get(slot);
		if (clickedTemplate == null) {
			return MenuClickResult.CANCEL_UPDATE;
		}
		if (clickedTemplate.isVipOnly()) {
			if (canUseVipTemplate(mPlayer)) {
				plugin.getController().createEmptyLevel(mPlayer, clickedTemplate);
				return MenuClickResult.CANCEL_CLOSE;
			} else {
				if (ClickType.RIGHT.equals(clickType)) {
					plugin.getController().checkTemplate(mPlayer, clickedTemplate);
					return MenuClickResult.CANCEL_CLOSE;
				}
				return MenuClickResult.CANCEL_UPDATE;
			}
		} else if (!clickedTemplate.isFree()) {
			if (canUseUnlockableTemplate(mPlayer, clickedTemplate.getTemplateId())) {
				plugin.getController().createEmptyLevel(mPlayer, clickedTemplate);
				return MenuClickResult.CANCEL_CLOSE;
			} else {
				if (ClickType.RIGHT.equals(clickType)) {
					plugin.getController().checkTemplate(mPlayer, clickedTemplate);
					return MenuClickResult.CANCEL_CLOSE;
				}
				MakerUnlockable unlockable = MakerUnlockable.getUnlockableByUUID(clickedTemplate.getTemplateId());
				if (unlockable != null) {
					mPlayer.sendMessage("command.unlock.confirm1", unlockable.getCost());
					mPlayer.sendMessage("command.unlock.confirm2", unlockable.name().toLowerCase());
				}
				return MenuClickResult.CANCEL_UPDATE;
			}
		} else {
			plugin.getController().createEmptyLevel(mPlayer, clickedTemplate);
			return MenuClickResult.CANCEL_CLOSE;
		}
	}

	@Override
	public void update() {
		verifyPrimaryThread();
		update(templatesByName.stream().skip(getPageOffset(currentPage)).limit(ITEMS_PER_PAGE).collect(Collectors.toList()));
	}

	public void update(Collection<MakerLevelTemplate> currentPageLevels) {
		verifyPrimaryThread();
		slotTemplateMap.clear();
		updatePaginationItems();
		for (int j = 10; j < 44; j++) {
			if (isItemSlot(j)) {
				items[j] = getGlassPane();
			}
		}

		int i = 10;
		if (currentPageLevels != null && currentPageLevels.size() > 0) {
			templateSlots: for (MakerLevelTemplate template : currentPageLevels) {
				while (!isItemSlot(i)) {
					i++;
					if (i >= items.length) {
						break templateSlots;
					}
				}
				ItemStack item = getTemplateItem(template);
				if (item != null) {
					slotTemplateMap.put(i, template);
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

}
