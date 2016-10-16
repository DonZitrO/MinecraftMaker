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
import com.minecade.mcore.cmd.AbstractCommandExecutor;
import com.minecade.mcore.data.Rank;
import com.minecade.minecraftmaker.controller.MakerController;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelCommandExecutor extends AbstractCommandExecutor<MinecraftMakerPlugin, MakerController, MakerPlayer> {

	private static long validateLevelSerial(String[] args, int index) {
		if (args.length > index) {
			try {
				long serial = Long.parseLong(args[index]);
				Validate.isTrue(serial > 0);
				return serial;
			} catch (Exception e) {
				Bukkit.getLogger().warning(String.format("LevelCommandExecutor.validateLevelSerial - invalid level serial: [%s] - %s", args[index], e.getMessage()));
				return 0;
			}
		}
		return 0;
	}

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
			sender.sendMessage(plugin.getMessage("command.error.permissions"));
			return true;
		}
		if (args.length < 1) {
			showUsage(mPlayer);
			return true;
		}
		if (args[0].equalsIgnoreCase("rename")) {
			executeRenameCommand(args, mPlayer);
			return true;
		}
		if (args[0].equalsIgnoreCase("search")) {
			executeSearchCommand(args, mPlayer);
			return true;
		}
		if (args[0].equalsIgnoreCase("delete")) {
			executeDeleteCommand(args, mPlayer);
			return true;
		}
		if (args[0].equalsIgnoreCase("unpublish")) {
			executeUnpublishCommand(args, mPlayer);
			return true;
		}
		// ADMIN only sub-commands below
		if (!mPlayer.getData().hasRank(Rank.ADMIN)) {
			showUsage(mPlayer);
			return true;
		}
		if (args[0].equalsIgnoreCase("copy")) {
			executeCopyCommand(args, mPlayer);
			return true;
		}
		showUsage(mPlayer);
		return true;
	}

	private void executeCopyCommand(String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			mPlayer.sendMessage("command.level.edit.usage");
			mPlayer.sendMessage("command.level.edit.permissions");
			return;
		}
		long serial = validateLevelSerial(args, 1);
		if (serial <= 0) {
			mPlayer.sendMessage("command.level.error.invalid-serial");
			return;
		}
		plugin.getController().copyAndLoadLevelForEditingBySerial(mPlayer, serial);
		return;
	}

	private void executeDeleteCommand(String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			mPlayer.sendMessage("command.level.delete.usage");
			mPlayer.sendMessage("command.level.delete.permissions");
			return;
		}
		long serial = validateLevelSerial(args, 1);
		if (serial <= 0) {
			mPlayer.sendMessage("command.level.error.invalid-serial");
			return;
		}
		plugin.getController().deleteLevel(mPlayer, serial);
	}

	private void executeRenameCommand(String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			mPlayer.sendMessage("command.level.rename.usage");
			mPlayer.sendMessage("command.level.rename.permissions");
			return;
		}
		String name = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelCommandExecutor.executeRenameCommand - new level name: [%s]", name));
		}
		if (StringUtils.isBlank(name)) {
			mPlayer.sendMessage("level.rename.error.empty-name");
			return;
		}
		if (name.length() < 3) {
			mPlayer.sendMessage("level.rename.error.too-short");
			return;
		}
		if (name.length() > 30) {
			mPlayer.sendMessage("level.rename.error.too-long");
			return;
		}
		if (!name.matches("\\w(\\w|'|\\ )*\\w")) {
			mPlayer.sendMessage("level.rename.error.invalid");
			return;
		}
		// capitalize
		name = WordUtils.capitalize(name);
		plugin.getController().renameLevel(mPlayer, name);
	}

	private void executeSearchCommand(String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			mPlayer.sendMessage("command.level.search.usage");
			mPlayer.sendMessage("command.level.search.permissions");
			return;
		}
		String searchString = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | LevelCommandExecutor.executeSearchCommand - search string: [%s]", searchString));
		}
		if (StringUtils.isBlank(searchString)) {
			mPlayer.sendMessage("level.search.error.empty-string");
			return;
		}
		if (searchString.length() < 3 || searchString.length() > 16) {
			mPlayer.sendMessage("level.search.error.wrong-size-string");
			return;
		}
		if (!searchString.matches("\\w(\\w|'|\\ )*\\w")) {
			mPlayer.sendMessage("level.search.error.invalid-string");
			return;
		}
		plugin.getController().searchLevels(mPlayer, searchString);
	}

	private void executeUnpublishCommand(String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			mPlayer.sendMessage("command.level.unpublish.usage");
			mPlayer.sendMessage("command.level.unpublish.permissions");
			return;
		}
		long serial = validateLevelSerial(args, 1);
		if (serial <= 0) {
			mPlayer.sendMessage("command.level.error.invalid-serial");
			return;
		}
		plugin.getController().unpublishLevel(mPlayer, serial);
	}

	private void showUsage(MakerPlayer mPlayer) {
		mPlayer.sendMessage("command.level.usage");
		mPlayer.sendMessage("command.level.actions");
		mPlayer.sendMessage("command.level.actions.help");
	}

}
