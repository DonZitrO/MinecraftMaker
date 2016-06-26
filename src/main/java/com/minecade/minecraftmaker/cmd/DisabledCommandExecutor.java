package com.minecade.minecraftmaker.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class DisabledCommandExecutor extends AbstractCommandExecutor {

	public DisabledCommandExecutor(MinecraftMakerPlugin plugin) {
		super(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		sender.sendMessage(plugin.getMessage("command.error.disabled"));
		return true;
	}

}
