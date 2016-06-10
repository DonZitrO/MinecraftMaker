package com.minecade.minecraftmaker.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerLobbyCommandExecutor extends AbstractCommandExecutor {

	public MakerLobbyCommandExecutor(MinecraftMakerPlugin plugin) {
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
		if (mPlayer == null || !mPlayer.isSpectating()) {
			sender.sendMessage(plugin.getMessage("command.error.spectator-only"));
			return true;
		}
		plugin.getController().stopSpectating(mPlayer);
		return true;
	}

}