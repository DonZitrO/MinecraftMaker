package com.minecade.minecraftmaker.cmd;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minecade.core.data.Rank;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class PlayerCommandExecutor extends AbstractCommandExecutor {

	public PlayerCommandExecutor(MinecraftMakerPlugin plugin) {
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
			sender.sendMessage(plugin.getMessage("command.player.usage"));
			sender.sendMessage(plugin.getMessage("command.player.actions"));
			sender.sendMessage(plugin.getMessage("command.player.actions.help"));
			return true;
		}
		// TITAN only sub-commands below
		if (!mPlayer.getData().hasRank(Rank.TITAN)) {
			sender.sendMessage(plugin.getMessage("command.error.permissions"));
			sender.sendMessage(plugin.getMessage("command.player.usage"));
			sender.sendMessage(plugin.getMessage("command.player.actions"));
			sender.sendMessage(plugin.getMessage("command.player.actions.help"));
			return true;
		}
		if (args[0].equalsIgnoreCase("muteall")) {
			plugin.getController().muteOthers(mPlayer.getUniqueId());
			return true;
		}
		if (args[0].equalsIgnoreCase("unmuteall")) {
			plugin.getController().unmuteOthers(mPlayer.getUniqueId());
			return true;
		}
		// GM only sub-commands below
		if (!mPlayer.getData().hasRank(Rank.GM)) {
			sender.sendMessage(plugin.getMessage("command.error.permissions"));
			sender.sendMessage(plugin.getMessage("command.player.usage"));
			sender.sendMessage(plugin.getMessage("command.player.actions"));
			sender.sendMessage(plugin.getMessage("command.player.actions.help"));
			return true;
		}
		if (args[0].equalsIgnoreCase("mute")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.player.mute.usage"));
				sender.sendMessage(plugin.getMessage("command.player.mute.permissions"));
				return true;
			}
			String name = args[1];
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | PlayerCommandExecutor.onCommand - mute request for: [%s]", name));
			}
			if (StringUtils.isBlank(name)) {
				sender.sendMessage(plugin.getMessage("player.mute.error.empty-name"));
				return true;
			}
			if (name.length() > 16) {
				sender.sendMessage(plugin.getMessage("player.mute.error.too-long"));
				return true;
			}
			if (!name.matches("[a-zA-Z0-9_]")) {
				sender.sendMessage(plugin.getMessage("player.mute.error.invalid"));
				return true;
			}
			Player toMute = Bukkit.getPlayerExact(name);
			if (toMute == null) {
				sender.sendMessage(plugin.getMessage("player.mute.error.not-found"));
				return true;
			}
			plugin.getController().mutePlayer(toMute.getUniqueId());
			return true;
		}
		if (args[0].equalsIgnoreCase("unmute")) {
			if (args.length < 2) {
				sender.sendMessage(plugin.getMessage("command.player.mute.usage"));
				sender.sendMessage(plugin.getMessage("command.player.mute.permissions"));
				return true;
			}
			String name = args[1];
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | PlayerCommandExecutor.onCommand - unmute request for: [%s]", name));
			}
			if (StringUtils.isBlank(name)) {
				sender.sendMessage(plugin.getMessage("player.mute.error.empty-name"));
				return true;
			}
			if (name.length() > 16) {
				sender.sendMessage(plugin.getMessage("player.mute.error.too-long"));
				return true;
			}
			if (!name.matches("[a-zA-Z0-9_]")) {
				sender.sendMessage(plugin.getMessage("player.mute.error.invalid"));
				return true;
			}
			Player toUnmute = Bukkit.getPlayerExact(name);
			if (toUnmute == null) {
				sender.sendMessage(plugin.getMessage("player.mute.error.not-found"));
				return true;
			}
			plugin.getController().unmutePlayer(toUnmute.getUniqueId());
			return true;
		}
		sender.sendMessage(plugin.getMessage("command.player.usage"));
		sender.sendMessage(plugin.getMessage("command.player.actions"));
		sender.sendMessage(plugin.getMessage("command.player.actions.help"));
		return true;
	}

}
