package com.minecade.minecraftmaker.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minecade.mcore.data.Rank;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerTestCommandExecutor extends AbstractMakerCommandExecutor {

	public MakerTestCommandExecutor(MinecraftMakerPlugin plugin) {
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
			sender.sendMessage(command.getUsage());
			return true;
		}
		if (!mPlayer.getData().hasRank(Rank.DEV)) {
			sender.sendMessage(command.getPermissionMessage());
			return true;
		}
		if (args[0].equalsIgnoreCase("steveclear")) {
			executeSteveClear(mPlayer);
			return true;
		}
		
		sender.sendMessage(command.getUsage());
		return true;
	}

	private void executeSteveClear(MakerPlayer mPlayer) {
		if (mPlayer.isInSteve()) {
			for (int i = 1; i < 16; i++) {
				mPlayer.getSteveData().clearLevel(Long.valueOf(i));
			}
		}
	}

}
