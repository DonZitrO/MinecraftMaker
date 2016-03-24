package com.minecade.minecraftmaker.cmd;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.minecade.minecraftmaker.MinecraftMaker;

/**
 * 
 * This command is intended for developers only (which should be opped or on console)
 * for the early stages of the plugin development to manipulate levels.
 * 
 * @author DonZitrO
 * 
 */
public class LevelCommandExecutor extends AbstractCommandExecutor {

	private static final int MIN_CHUNK_COORDINATE = 0;
	private static final int MAX_CHUNK_COORDINATE = 63;

	public LevelCommandExecutor(MinecraftMaker plugin) {
		super(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// only console and op players allowed
		if (!(sender instanceof ConsoleCommandSender) && !(sender instanceof Player)) {
			sender.sendMessage(command.getPermissionMessage());
			return true;
		}
		// if sender is player it must be op
		if (sender instanceof Player && !((Player) sender).isOp()) {
			sender.sendMessage(command.getPermissionMessage());
			return true;
		}
		if (args.length != 3) {
			sender.sendMessage(command.getUsage());
			return true;
		}
		if (args[0].equalsIgnoreCase("create")) {
			if (!isValidLevelName(args[1]) || !isValidChunkCoordinate(args[2])) {
				sender.sendMessage(command.getUsage());
				return true;
			}
			plugin.getController().createEmptyLevel(args[1], Short.parseShort(args[2]));
			return true;
		}
		return false;
	}

	private boolean isValidChunkCoordinate(String chunkCoordinate) {
		try {
			Validate.inclusiveBetween(MIN_CHUNK_COORDINATE, MAX_CHUNK_COORDINATE, Short.parseShort(chunkCoordinate));
		} catch (Exception e) {
			Bukkit.getLogger().warning(String.format("LevelCommandExecutor.isValidChunkCoordinate - invalid chunk coordinate: [%s] - %s", chunkCoordinate, e.getMessage()));
			return false;
		}
		return true;
	}

	private boolean isValidLevelName(String levelName) {
		return !StringUtils.isBlank(levelName);
	}

}
