package com.minecade.minecraftmaker.cmd;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Joiner;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class ReportCommandExecutor extends AbstractCommandExecutor {

	public ReportCommandExecutor(MinecraftMakerPlugin plugin) {
		super(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// only players are allowed
		if (!(sender instanceof Player)) {
			sender.sendMessage(command.getPermissionMessage());
			return true;
		}
		if (args.length < 1) {
			sender.sendMessage(plugin.getMessage("command.report.usage"));
			return true;
		}
		String report = Joiner.on(" ").join(args);
		if (StringUtils.isBlank(report)) {
			sender.sendMessage(plugin.getMessage("command.report.error.empty-report"));
			return true;
		}
		if (report.length() > 256) {
			sender.sendMessage(plugin.getMessage("command.report.error.too-long"));
			return true;
		}
		plugin.getDatabaseAdapter().reportAsync(((Player)sender).getUniqueId(), sender.getName(), report);
		sender.sendMessage(plugin.getMessage("command.report.success"));
		return true;
	}

}
