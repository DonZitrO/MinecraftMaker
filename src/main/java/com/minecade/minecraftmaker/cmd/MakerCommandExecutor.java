package com.minecade.minecraftmaker.cmd;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minecade.core.data.Rank;
import com.minecade.core.item.SkullItemBuilder;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerCommandExecutor extends AbstractCommandExecutor {

	public MakerCommandExecutor(MinecraftMakerPlugin plugin) {
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
			sender.sendMessage(plugin.getMessage("command.maker.usage"));
			sender.sendMessage(plugin.getMessage("command.maker.actions"));
			sender.sendMessage(plugin.getMessage("command.maker.actions.help"));
			return true;
		}
		if (args[0].equalsIgnoreCase("head")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.maker.head.usage"));
				sender.sendMessage(plugin.getMessage("command.maker.head.permissions"));
				return true;
			}
			if (!mPlayer.getData().hasRank(Rank.PRO)) {
				mPlayer.sendMessage(plugin, "command.error.rank.pro");
				mPlayer.sendMessage(plugin, "command.error.rank.upgrade");
				return true;
			}
			if (!mPlayer.isEditingLevel()) {
				mPlayer.sendMessage(plugin, "command.error.edit-only");
				return true;
			}
			String nameError = validatePlayerName(args[1]);
			if (nameError != null) {
				mPlayer.sendMessage(plugin, nameError);
				return true;
			}
			mPlayer.getPlayer().getInventory().addItem(new SkullItemBuilder(args[1]).build());
			return true;
		}
		// TITAN only sub-commands below
		if (!mPlayer.getData().hasRank(Rank.TITAN)) {
			sender.sendMessage(plugin.getMessage("command.error.permissions"));
			sender.sendMessage(plugin.getMessage("command.maker.usage"));
			sender.sendMessage(plugin.getMessage("command.maker.actions"));
			sender.sendMessage(plugin.getMessage("command.maker.actions.help"));
			return true;
		}
		if (args[0].equalsIgnoreCase("muteall")) {
			plugin.getController().muteOthers(mPlayer.getUniqueId());
			sender.sendMessage(plugin.getMessage("command.maker.muteall.success"));
			return true;
		}
		if (args[0].equalsIgnoreCase("unmuteall")) {
			plugin.getController().unmuteOthers(mPlayer.getUniqueId());
			sender.sendMessage(plugin.getMessage("command.maker.unmuteall.success"));
			return true;
		}
		// GM only sub-commands below
		if (!mPlayer.getData().hasRank(Rank.GM)) {
			sender.sendMessage(plugin.getMessage("command.error.permissions"));
			sender.sendMessage(plugin.getMessage("command.maker.usage"));
			sender.sendMessage(plugin.getMessage("command.maker.actions"));
			sender.sendMessage(plugin.getMessage("command.maker.actions.help"));
			return true;
		}
		if (args[0].equalsIgnoreCase("mute")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.maker.mute.usage"));
				sender.sendMessage(plugin.getMessage("command.maker.mute.permissions"));
				return true;
			}
			String nameError = validatePlayerName(args[1]);
			if (nameError != null) {
				mPlayer.sendMessage(plugin, nameError);
				return true;
			}
			Player toMute = Bukkit.getPlayerExact(args[1]);
			if (toMute == null) {
				sender.sendMessage(plugin.getMessage("player.error.not-found"));
				return true;
			}
			plugin.getController().mutePlayer(toMute.getUniqueId());
			sender.sendMessage(plugin.getMessage("command.maker.mute.success", toMute.getDisplayName()));
			return true;
		}
		if (args[0].equalsIgnoreCase("unmute")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.maker.mute.usage"));
				sender.sendMessage(plugin.getMessage("command.maker.mute.permissions"));
				return true;
			}
			String nameError = validatePlayerName(args[1]);
			if (nameError != null) {
				mPlayer.sendMessage(plugin, nameError);
				return true;
			}
			Player toUnmute = Bukkit.getPlayerExact(args[1]);
			if (toUnmute == null) {
				sender.sendMessage(plugin.getMessage("player.error.not-found"));
				return true;
			}
			plugin.getController().unmutePlayer(toUnmute.getUniqueId());
			sender.sendMessage(plugin.getMessage("command.maker.unmute.success", toUnmute.getDisplayName()));
			return true;
		}
		sender.sendMessage(plugin.getMessage("command.maker.usage"));
		sender.sendMessage(plugin.getMessage("command.maker.actions"));
		sender.sendMessage(plugin.getMessage("command.maker.actions.help"));
		return true;
	}

	// TODO: move this to super class
	private String validatePlayerName(String playerName) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | PlayerCommandExecutor.validatePlayerName - validating player name: [%s]", playerName));
		}
		if (StringUtils.isBlank(playerName)) {
			return "player.error.empty-name";
		}
		if (playerName.length() > 16) {
			return "player.error.name-too-long";
		}
		if (!playerName.matches("^[a-zA-Z0-9_]{2,16}$")) {
			return "player.error.invalid-name";
		}
		return null;
	}

}
