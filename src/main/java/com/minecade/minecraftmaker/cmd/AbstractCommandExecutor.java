package com.minecade.minecraftmaker.cmd;

import org.bukkit.command.CommandExecutor;

import com.minecade.minecraftmaker.MinecraftMaker;

public abstract class AbstractCommandExecutor implements CommandExecutor {

	protected final MinecraftMaker plugin;

	public AbstractCommandExecutor(MinecraftMaker plugin) {
		this.plugin = plugin;
	}

}
