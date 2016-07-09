package com.minecade.minecraftmaker.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.minecade.core.item.ItemUtils;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.LevelToolsItem;
import com.minecade.minecraftmaker.items.SkullItem;
import com.minecade.minecraftmaker.items.SkullTypeItem;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelToolsMenu extends AbstractSharedMenu {

	private static ItemStack glassPane;
	private static LevelToolsMenu instance;

	private SkullTypeItem skullType;

	private LevelToolsMenu(MinecraftMakerPlugin plugin) {
		super(plugin, 45);
	}

	public static LevelToolsMenu getInstance() {
		if (instance == null) {
			instance = new LevelToolsMenu(MinecraftMakerPlugin.getInstance());
			instance.init();
		}
		return instance;
	}

	private void init() {
		loadGlassPanes();
		items[20] = LevelToolsItem.WEATHER.getItem();
		items[22] = LevelToolsItem.TIME.getItem();
		items[24] = LevelToolsItem.SKULL.getItem();
		items[44] = GeneralMenuItem.EXIT_MENU.getItem();
		inventory.setContents(items);
	}

	@Override
	public String getTitleKey(String modifier) {
		return "menu.level-tools.title";
	}

	private boolean isSkullType(ItemStack itemStack) {
		for(SkullTypeItem skullType : SkullTypeItem.values()){
			if(ItemUtils.itemNameEquals(itemStack, skullType.getDisplayName())) {
				this.skullType = skullType;
				return true;
			}
		}
		
		this.skullType = null;
		return false;
	}

	private void loadGlassPanes(){
		if(glassPane == null){
			ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE);
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(" ");
			itemStack.setItemMeta(itemMeta);
			glassPane = itemStack;
		}

		for(int i=0; i<items.length; i++){
			items[i] = glassPane;
		}
	}

	@Override
	public MenuClickResult onClick(MakerPlayer mPlayer, int slot) {
		MenuClickResult result = super.onClick(mPlayer, slot);

		if (!MenuClickResult.ALLOW.equals(result)) {
			return result;
		} else if (!mPlayer.isEditingLevel()) {
			Bukkit.getLogger().warning(String.format("LevelToolsMenu.onClick - This menu should be available to level editors while editing only! - clicked by: [%s]", mPlayer.getName()));
			return MenuClickResult.CANCEL_CLOSE;
		}
		

		ItemStack itemStack = inventory.getItem(slot);

		if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.EXIT.getDisplayName())) {
			mPlayer.openLevelToolsMenu();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.TIME.getDisplayName())) {
			mPlayer.openLevelTimeMenu();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.WEATHER.getDisplayName())) {
			mPlayer.openLevelWeatherMenu();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (ItemUtils.itemNameEquals(itemStack, LevelToolsItem.SKULL.getDisplayName())) {
			mPlayer.openToolsSkullTypeMenu();
			return MenuClickResult.CANCEL_CLOSE;
		} else if (isSkullType(itemStack)){
			loadGlassPanes();
			updateSkullsTypeMenu();
			return MenuClickResult.CANCEL_UPDATE;
		}

		return MenuClickResult.CANCEL_UPDATE;
	}

	private void updateSkullsTypeMenu(){
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelToolsMenu.updateSkullTypeInventory"));
		}
		int i = 0;
		for(SkullItem skull : SkullItem.values()){
			if(skull.getSkullType().equals(this.skullType)){
				items[i++] = skull.getItem();
			}
		}
		
		items[44] = LevelToolsItem.EXIT.getItem();
		inventory.setContents(items);
	}

}
