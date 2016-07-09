package com.minecade.minecraftmaker.inventory;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractSharedMenu extends AbstractMakerMenu {

	public AbstractSharedMenu(MinecraftMakerPlugin plugin, int size) {
		super(plugin, size);
	}

	public AbstractSharedMenu(MinecraftMakerPlugin plugin, int size, String titleModifier) {
		super(plugin, size, titleModifier);
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
