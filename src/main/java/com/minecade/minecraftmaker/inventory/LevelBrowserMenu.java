package com.minecade.minecraftmaker.inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelBrowserMenu extends AbstractMakerMenu {

	private static List<ItemStack[]> pages = new ArrayList<>();

	public LevelBrowserMenu(MinecraftMakerPlugin plugin) {
		super(plugin, plugin.getMessage(getTitleKey()), 54);
		init();
	}

	private void init() {
		items[0] = GeneralMenuItem.SORT.getItem();
		items[1] = GeneralMenuItem.SEARCH.getItem();
		items[3] = GeneralMenuItem.PREVIOUS_PAGE.getItem();
		items[4] = GeneralMenuItem.CURRENT_PAGE.getItem();
		ItemMeta currentPageMeta = items[4].getItemMeta();
		currentPageMeta.setDisplayName(String.format(currentPageMeta.getDisplayName(), 1, 1));
		items[4].setItemMeta(currentPageMeta);
		items[5] = GeneralMenuItem.NEXT_PAGE.getItem();
		items[8] = GeneralMenuItem.EXIT_MENU.getItem();
		for (int i = 9; i < inventory.getSize(); i++) {
			items[i] = new ItemStack(Material.STAINED_GLASS_PANE);
			switch (i) {
			case 0:
				break;
			default:
				break;
			}
//			ServerData info = serverData.get(i + 1);
//			if (info == null) {
//				info = createOfflineServerInfo(i + 1);
//				serverData.put(info.getServerNumber(), info);
//			}
//			items[i] = updateItemStack(null, info);
		}
	}

	@Override
	public void onClick(MakerPlayer mPlayer, int slot) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isShared() {
		return false;
	}

	public static String getTitleKey() {
		return "menu.level-browser.title";
	}

}
