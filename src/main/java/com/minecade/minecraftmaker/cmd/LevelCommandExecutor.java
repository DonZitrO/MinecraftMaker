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
		if (args.length < 3) {
			sender.sendMessage(command.getUsage());
			return true;
		}
		if (!isValidLevelName(args[1]) || !isValidChunkCoordinate(args[2])) {
			sender.sendMessage(command.getUsage());
			return true;
		}
		if (args[0].equalsIgnoreCase("create")) {
			plugin.getController().createEmptyLevel(args[1], Short.parseShort(args[2]), optionalBlockId(args, 3));
			return true;
		}
		if (args[0].equalsIgnoreCase("load")) {
			plugin.getController().loadLevel(args[1], Short.parseShort(args[2]));
			return true;
		}
		if (args[0].equalsIgnoreCase("save")) {
			plugin.getController().saveLevel(args[1], Short.parseShort(args[2]));
			return true;
		}
		return true;
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

	private int optionalBlockId(String[] args, int index) {
		if (args.length > index) {
			try {
				int blockId = Integer.parseInt(args[index]);
				Validate.inclusiveBetween(1, 4, blockId);
				return blockId;
			} catch (Exception e) {
				Bukkit.getLogger().warning(String.format("LevelCommandExecutor.optionalBlockId - invalid block id: [%s] - %s", args[index], e.getMessage()));
				return 0;
			}
		}
		return 0;
	}

}
