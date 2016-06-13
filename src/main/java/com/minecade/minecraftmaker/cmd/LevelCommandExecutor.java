package com.minecade.minecraftmaker.cmd;

import java.util.Arrays;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Joiner;
import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelCommandExecutor extends AbstractCommandExecutor {

	public LevelCommandExecutor(MinecraftMakerPlugin plugin) {
		super(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// only players allowed
		if (!(sender instanceof Player)) {
			sender.sendMessage(plugin.getMessage("command.error.player-only"));
			return true;
		}
		MakerPlayer mPlayer = plugin.getController().getPlayer((Player) sender);
		if (mPlayer == null) {
			sender.sendMessage(plugin.getMessage("command.level.error.permissions"));
			return true;
		}
		if (args.length < 1) {
			sender.sendMessage(plugin.getMessage("command.level.usage"));
			sender.sendMessage(plugin.getMessage("command.level.actions"));
			sender.sendMessage(plugin.getMessage("command.level.actions.help"));
			return true;
		}
		if (args[0].equalsIgnoreCase("rename")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.level.rename.usage"));
				sender.sendMessage(plugin.getMessage("command.level.rename.permissions"));
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
			if (name.length() < 3) {
				sender.sendMessage(plugin.getMessage("level.rename.error.too-short"));
				return true;
			}
			if (name.length() > 30) {
				sender.sendMessage(plugin.getMessage("level.rename.error.too-long"));
				return true;
			}
			if (!name.matches("\\w(\\w|'|\\ )*\\w")) {
				sender.sendMessage(plugin.getMessage("level.rename.error.invalid"));
				return true;
			}
			// capitalize
			name = WordUtils.capitalize(name);
			plugin.getController().renameLevel(mPlayer, name);
			return true;
		}
		if (args[0].equalsIgnoreCase("search")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.level.search.usage"));
				sender.sendMessage(plugin.getMessage("command.level.search.permissions"));
				//sender.sendMessage(plugin.getMessage("level.search.error.empty-string"));
				return true;
			}
			String searchString = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | LevelCommandExecutor.onCommand - search string: [%s]", searchString));
			}
			if (StringUtils.isBlank(searchString)) {
				sender.sendMessage(plugin.getMessage("level.search.error.empty-string"));
				return true;
			}
			if (searchString.length() < 3 || searchString.length() > 16) {
				sender.sendMessage(plugin.getMessage("level.search.error.wrong-size-string"));
				return true;
			}
			if (!searchString.matches("\\w(\\w|'|\\ )*\\w")) {
				sender.sendMessage(plugin.getMessage("level.search.error.invalid-string"));
				return true;
			}
			plugin.getController().searchLevels(mPlayer, searchString);
			return true;
		}
		// ADMIN only sub-commands below
		if (!mPlayer.getData().hasRank(Rank.ADMIN)) {
			sender.sendMessage(plugin.getMessage("command.level.error.permissions"));
			sender.sendMessage(plugin.getMessage("command.level.usage"));
			sender.sendMessage(plugin.getMessage("command.level.actions"));
			sender.sendMessage(plugin.getMessage("command.level.actions.help"));
			return true;
		}
		if (args[0].equalsIgnoreCase("edit")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.level.edit.usage"));
				sender.sendMessage(plugin.getMessage("command.level.edit.permissions"));
				return true;
			}
			long serial = validateLevelSerial(args, 1);
			if (serial <= 0) {
				sender.sendMessage(plugin.getMessage("command.level.error.invalid-serial"));
				return true;
			}
			plugin.getController().copyLevel(mPlayer, serial);
			return true;
		}
		if (args[0].equalsIgnoreCase("delete")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.level.delete.usage"));
				sender.sendMessage(plugin.getMessage("command.level.delete.permissions"));
				// sender.sendMessage(plugin.getMessage("command.level.error.empty-serial"));
				return true;
			}
			long serial = validateLevelSerial(args, 1);
			if (serial <= 0) {
				sender.sendMessage(plugin.getMessage("command.level.error.invalid-serial"));
				return true;
			}
			plugin.getController().deleteLevel(mPlayer, serial);
			return true;
		}
		sender.sendMessage(plugin.getMessage("command.level.usage"));
		sender.sendMessage(plugin.getMessage("command.level.actions"));
		sender.sendMessage(plugin.getMessage("command.level.actions.help"));
		return true;
	}

	private static long validateLevelSerial(String[] args, int index) {
		if (args.length > index) {
			try {
				long serial = Long.parseLong(args[index]);
				Validate.isTrue(serial > 0);
				return serial;
			} catch (Exception e) {
				Bukkit.getLogger().warning(String.format("LevelCommandExecutor.optionalBlockId - invalid level serial: [%s] - %s", args[index], e.getMessage()));
				return 0;
			}
		}
		return 0;
	}

}
