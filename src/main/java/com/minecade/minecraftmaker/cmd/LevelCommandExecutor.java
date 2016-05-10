package com.minecade.minecraftmaker.cmd;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Joiner;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

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
				Bukkit.getLogger().info(String.format("LevelCommandExecutor.onCommand - new level name: [%s]", name));
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
		// op-only sub-commands below
		if (!((Player) sender).isOp()) {
			sender.sendMessage(command.getPermissionMessage());
			return true;
		}
		if (!isValidLevelName(args[1])) {
			sender.sendMessage(command.getUsage());
			return true;
		}
		//  || !isValidChunkCoordinate(args[2])
		if (args[0].equalsIgnoreCase("create")) {
			plugin.getController().createEmptyLevel(((Player)sender).getUniqueId(), optionalBlockId(args, 1));
			return true;
		}
		if (args[0].equalsIgnoreCase("load")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("level.error.empty-name"));
				return true;
			}
			if (!isValidLevelName(args[1])) {
				sender.sendMessage(command.getUsage());
				return true;
			}
			// FIXME: remove chunk coordinate
			plugin.getController().loadLevel(((Player)sender).getUniqueId(), args[1], Short.parseShort(args[2]));
			return true;
		}
		if (args[0].equalsIgnoreCase("save")) {
			// FIXME: remove chunk coordinate and maybe name
			plugin.getController().saveLevel(((Player)sender).getUniqueId(), args[1], Short.parseShort(args[2]));
			return true;
		}
		return true;
	}

	@SuppressWarnings("unused") // TODO: use or remove
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
