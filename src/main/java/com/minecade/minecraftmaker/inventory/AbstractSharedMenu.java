package com.minecade.minecraftmaker.inventory;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class AbstractSharedMenu extends AbstractMakerMenu {

	public AbstractSharedMenu(MinecraftMakerPlugin plugin, String title, int size) {
		super(plugin, title, size);
	}

	@Override
	public final void update() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean isShared() {
		return true;
	}

}
