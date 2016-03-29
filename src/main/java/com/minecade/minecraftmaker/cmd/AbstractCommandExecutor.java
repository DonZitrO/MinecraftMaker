package com.minecade.minecraftmaker.cmd;

import org.bukkit.command.CommandExecutor;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public abstract class AbstractCommandExecutor implements CommandExecutor {

	protected final MinecraftMakerPlugin plugin;

	public AbstractCommandExecutor(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

}
