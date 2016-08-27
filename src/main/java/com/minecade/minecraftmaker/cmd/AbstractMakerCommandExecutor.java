package com.minecade.minecraftmaker.cmd;

import com.minecade.mcore.cmd.AbstractCommandExecutor;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractMakerCommandExecutor extends AbstractCommandExecutor {

	protected final MinecraftMakerPlugin plugin;

	public AbstractMakerCommandExecutor(MinecraftMakerPlugin plugin) {
		super(plugin);
		this.plugin = plugin;
	}

}
