package com.minecade.minecraftmaker.cmd;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minecade.minecraftmaker.data.MakerUnlockable;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class UnlockCommandExecutor extends AbstractCommandExecutor {

	private static MakerUnlockable validateUnlockable(String[] args, int index) {
		if (args.length > index) {
			try {
				return MakerUnlockable.valueOf(args[index].toUpperCase());
			} catch (Exception e) {
				Bukkit.getLogger().warning(String.format("UnlockCommandExecutor.validateUnlockable - invalid unlockable: [%s] - %s", args[index], e.getMessage()));
			}
		}
		return null;
	}

	public UnlockCommandExecutor(MinecraftMakerPlugin plugin) {
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
		MakerUnlockable unlockable = validateUnlockable(args, 0);
		if (unlockable == null) {
			mPlayer.sendMessage("command.unlock.invalid-unlockable");
			return true;
		}
		plugin.getController().unlock(mPlayer, unlockable);
		return true;
	}

	private void showUsage(MakerPlayer mPlayer) {
		mPlayer.sendMessage("command.unlock.usage");
		// TODO: maybe list unlockables
	}

}
