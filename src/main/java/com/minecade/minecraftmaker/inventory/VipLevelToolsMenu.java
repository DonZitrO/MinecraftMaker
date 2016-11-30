package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.CommonMenuItem;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class VipLevelToolsMenu extends AbstractSharedMenu {

	private static VipLevelToolsMenu instance;

	private VipLevelToolsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 45);
	}

	public static VipLevelToolsMenu getInstance() {
		if (instance == null) {
			instance = new VipLevelToolsMenu(MinecraftMakerPlugin.getInstance());
			instance.init();
		}
		return instance;
	}

	private void init() {
		loadGlassPanes(items);
		items[22] = LevelToolsItem.SKULL.getItem();
		items[44] = CommonMenuItem.EXIT_MENU.getItem();
		inventory.setContents(items);
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.vip-level-tools.title";
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot, ClickType clickType) {
		MenuClickResult result = super.onClick(mPlayer, slot, clickType);

		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isEditing()) {
			Bukkit.getLogger().warning(String.format("VipLevelToolsMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		

		ItemStack itemStack = inventory.getItem(slot);
		if (itemStack == null) {
			return MenuClickResult.ALLOW;
		}

		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.SKULL.getDisplayName())) {
			mPlayer.openToolsSkullTypeMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}

		return MenuClickResult.CANCEL_UPDATE;
	}

}
