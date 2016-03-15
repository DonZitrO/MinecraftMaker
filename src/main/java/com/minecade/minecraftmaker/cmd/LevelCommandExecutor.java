package com.minecade.minecraftmaker.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * 
 * This command is intended for developers only (which should be opped or on console)
 * for the early stages of the plugin development to manipulate levels.
 * 
 * @author DonZitrO
 * 
 */
public class LevelCommandExecutor implements CommandExecutor {

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
		if (args[1].equalsIgnoreCase("create")) {
			// TODO create empty level
		}
		return false;
	}

}
