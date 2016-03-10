package com.minecade.minecraftmaker;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.minecade.core.item.active.ActiveItem;
import com.minecade.core.util.ItemBuilder;

public class LobbyMenuItem extends ActiveItem {
	
	MakerBase base;

	public LobbyMenuItem(MakerBase base) {
		super(new ItemBuilder(Material.CHEST, 1).setTitle(base.getText("lobby.item.mainmenu")), true);
		this.base = base;
	}

	@Override
	protected void runAction(Player user) {
		// TODO Auto-generated method stub

	}

}
