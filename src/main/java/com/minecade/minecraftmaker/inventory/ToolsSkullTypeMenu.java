package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.SkullTypeItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class ToolsSkullTypeMenu extends AbstractSharedMenu {

	private static ToolsSkullTypeMenu instance;

	private ToolsSkullTypeMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 45);
	}

	public static ToolsSkullTypeMenu getInstance() {
		if (instance == null) {
			instance = new ToolsSkullTypeMenu(MinecraftMakerPlugin.getInstance());
			instance.init();
		}
		return instance;
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.skulltype.title";
	}

	private void init() {
		loadGlassPanes(items);
		items[9] = SkullTypeItem.CHARACTERS.getItem();
		items[11] = SkullTypeItem.DEVICES.getItem();
		items[13] = SkullTypeItem.FOOD.getItem();
		items[15] = SkullTypeItem.GAMES.getItem();
		items[17] = SkullTypeItem.INTERIOR.getItem();
		items[18] = SkullTypeItem.MISC.getItem();
		items[20] = SkullTypeItem.MOBS.getItem();
		items[22] = SkullTypeItem.POKEMON.getItem();
		items[24] = SkullTypeItem.COLORS.getItem();
		items[26] = SkullTypeItem.HALLOWEEN.getItem();
		items[31] = GeneralMenuItem.CUSTOM_HEAD.getItem();
		items[44] = LevelToolsItem.EXIT.getItem();
		inventory.setContents(items);
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);

		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("ToolsSkullTypeMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		ItemStack itemStack = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openLevelToolsMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}
		if (ItemUtils.itemNameEquals(itemStack, GeneralMenuItem.CUSTOM_HEAD.getDisplayName())) {
			mPlayer.sendMessage(plugin, "command.maker.head.usage");
			return MenuClickResult.CANCEL_CLOSE;
		}
		SkullTypeItem skullTypeItem = SkullTypeItem.getSkullTypeItemByDisplayName(itemStack.getItemMeta().getDisplayName());
		if (skullTypeItem == null) {
			Bukkit.getLogger().warning(String.format("ToolsSkullTypeMenu.onClick - No skull type item for display name: [%s]", itemStack.getItemMeta().getDisplayName()));
			return MenuClickResult.CANCEL_UPDATE;
		}
		//Bukkit.getLogger().warning(String.format("ToolsSkullTypeMenu.onClick - skull type item found: [%s] - for display name: [%s]", skullTypeItem.name(), itemStack.getItemMeta().getDisplayName()));
		mPlayer.openToolsSkullMenu(skullTypeItem);
		return MenuClickResult.CANCEL_CLOSE;
	}

}
