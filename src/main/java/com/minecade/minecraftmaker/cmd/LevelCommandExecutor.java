package com.minecade.minecraftmaker.cmd;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Joiner;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelCommandExecutor extends AbstractCommandExecutor {

	public LevelCommandExecutor(MinecraftMakerPlugin plugin) {
		super(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// only players allowed
		if (!(sender instanceof Player)) {
			sender.sendMessage(command.getPermissionMessage());
			return true;
		}
		if (args.length < 1) {
			sender.sendMessage(command.getUsage());
			return true;
		}
		if (args[0].equalsIgnoreCase("rename")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("level.rename.error.empty-name"));
				return true;
			}
			String name = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | LevelCommandExecutor.onCommand - new level name: [%s]", name));
			}
			if (StringUtils.isBlank(name)) {
				sender.sendMessage(plugin.getMessage("level.rename.error.empty-name"));
				return true;
			}
			if (name.length() > 30) {
				sender.sendMessage(plugin.getMessage("level.rename.error.too-long"));
				return true;
			}
			plugin.getController().renameLevel((Player)sender, name);
			return true;
		}
		return true;
	}

}
