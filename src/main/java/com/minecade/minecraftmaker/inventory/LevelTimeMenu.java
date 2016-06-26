package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.TimeItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelTimeMenu extends AbstractMakerMenu {

	private static LevelTimeMenu instance;

	private LevelTimeMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage(getTitleKey()), 45);
	}

	public static LevelTimeMenu getInstance() {
		if (instance == null) {
			instance = new LevelTimeMenu(MinecraftMakerPlugin.getInstance());
			instance.init();
		}
		return instance;
	}

	private void init() {
		loadGlassPanes(items);
		items[18] = TimeItem.THREE.getItem();
		items[19] = TimeItem.SIX.getItem();
		items[20] = TimeItem.NINE.getItem();
		items[21] = TimeItem.TWELVE.getItem();
		items[23] = TimeItem.FIFTEEN.getItem();
		items[24] = TimeItem.EIGHTEEN.getItem();
		items[25] = TimeItem.TWENTYONE.getItem();
		items[26] = TimeItem.MIDNIGHT.getItem();
		items[44] = LevelToolsItem.EXIT.getItem();
		inventory.setContents(items);
	}

	@Override
	public boolean isShared() {
		return true;
	}

	public static String getTitleKey() {
		return "menu.level-time.title";
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);
		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("LevelTimeMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		
		ItemStack itemStack = inventory.getItem(slot);
		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openLevelToolsMenu();
			return MenuClickResult.CANCEL_CLOSE;
		}
		TimeItem timeItem = TimeItem.getTimeItemByDisplayName(itemStack.getItemMeta().getDisplayName());
		if (timeItem == null) {
			Bukkit.getLogger().warning(String.format("LevelTimeMenu.onClick - No time item for display name: [%s]", itemStack.getItemMeta().getDisplayName()));
			return MenuClickResult.CANCEL_UPDATE;
		}
		mPlayer.getCurrentLevel().setLevelTime(timeItem.getTime());
		mPlayer.getPlayer().setPlayerTime(timeItem.getTime(), false);
		return MenuClickResult.CANCEL_CLOSE;
	}

	@Override
	public void update() {
	}

}