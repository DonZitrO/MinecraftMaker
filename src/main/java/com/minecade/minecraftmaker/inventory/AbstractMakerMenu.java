package com.minecade.minecraftmaker.inventory;

import com.minecade.mcore.inventory.AbstractMenu;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractMakerMenu extends AbstractMenu<MakerPlayer> {

	protected final MinecraftMakerPlugin plugin;

	public AbstractMakerMenu(MinecraftMakerPlugin plugin, int size) {
		this(plugin, size, null);
	}

	public AbstractMakerMenu(MinecraftMakerPlugin plugin, int size, String titleModifier) {
		super(plugin, size, titleModifier);
		this.plugin = plugin;
	}

}
