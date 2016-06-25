package com.minecade.minecraftmaker.inventory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Maps;
import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.SkullItem;
import com.minecade.minecraftmaker.items.SkullTypeItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class ToolsSkullMenu extends AbstractSharedMenu {

	private final static Map<SkullTypeItem, ToolsSkullMenu> BY_SKULL_TYPE = Maps.newHashMap();

	public static ToolsSkullMenu getInstance(SkullTypeItem skullType) {
		checkNotNull(skullType);
		ToolsSkullMenu instance = BY_SKULL_TYPE.get(skullType);
		if (instance == null) {
			instance = new ToolsSkullMenu(MinecraftMakerPlugin.getInstance(), skullType);
			instance.init();
		}
		return instance;
	}

	private final SkullTypeItem skullType;

	private ToolsSkullMenu(MinecraftMakerPlugin plugin, SkullTypeItem skullType) {
		super(plugin, plugin.getMessage(getTitleKey(skullType)), 45);
		this.skullType = skullType;
	}

	private void init() {
		loadGlassPanes(items);
		int i = 0;
		for(SkullItem skull : SkullItem.getSkullsByType(skullType)){
			items[i++] = skull.getItem();
		}
		
		items[44] = LevelToolsItem.EXIT.getItem();
		inventory.setContents(items);
	}

	public static String getTitleKey(SkullTypeItem skullType) {
		checkNotNull(skullType);
		return String.format("menu.%s-skulls.title", skullType.name().toLowerCase());
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);

		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("ToolsSkullMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		ItemStack itemStack = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openToolsSkullTypeMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}
		SkullItem skullItem = SkullItem.getSkullItemByDisplayName(itemStack.getItemMeta().getDisplayName());
		if (skullItem == null) {
			Bukkit.getLogger().warning(String.format("ToolsSkullMenu.onClick - No skull item for display name: [%s]", itemStack.getItemMeta().getDisplayName()));
			return MenuClickResult.CANCEL_UPDATE;
		}
		mPlayer.getPlayer().getInventory().addItem(skullItem.getItem());
		return MenuClickResult.CANCEL_CLOSE;
	}

}
