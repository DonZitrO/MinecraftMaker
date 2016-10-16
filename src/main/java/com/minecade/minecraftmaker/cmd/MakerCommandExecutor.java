package com.minecade.minecraftmaker.cmd;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.minecade.mcore.cmd.AbstractCommandExecutor;
import com.minecade.mcore.data.Rank;
import com.minecade.mcore.item.SkullItemBuilder;
import com.minecade.minecraftmaker.controller.MakerController;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.PlayableLevelLimits;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerCommandExecutor extends AbstractCommandExecutor<MinecraftMakerPlugin, MakerController, MakerPlayer> {

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
			executeHeadCommand(sender, args, mPlayer);
			return true;
		}
		if (args[0].equalsIgnoreCase("invite")) {
			executeInviteCommand(args, mPlayer);
			return true;
		}
		if (args[0].equalsIgnoreCase("join")) {
			executeJoinCommand(args, mPlayer);
			return true;
		}
		if (args[0].equalsIgnoreCase("remove")) {
			executeRemoveCommand(args, mPlayer);
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
		if (!mPlayer.getData().hasRank(Rank.GM) && !mPlayer.getData().hasRank(Rank.YT)) {
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
				mPlayer.sendMessage(nameError);
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
				mPlayer.sendMessage(nameError);
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

	private void executeHeadCommand(CommandSender sender, String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			sender.sendMessage(plugin.getMessage("command.maker.head.usage"));
			sender.sendMessage(plugin.getMessage("command.maker.head.permissions"));
			return;
		}
		if (!mPlayer.getData().hasRank(Rank.PRO)) {
			mPlayer.sendMessage("command.error.rank.pro");
			mPlayer.sendMessage("command.error.rank.upgrade");
			return;
		}
		if (!mPlayer.isEditing()) {
			mPlayer.sendMessage("command.error.edit-only");
			return;
		}
		String nameError = validatePlayerName(args[1]);
		if (nameError != null) {
			mPlayer.sendMessage(nameError);
			return;
		}
		mPlayer.getPlayer().getInventory().addItem(new SkullItemBuilder(args[1]).build());
	}

	private void executeInviteCommand(String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			mPlayer.sendMessage("command.maker.invite.usage");
			mPlayer.sendMessage("command.maker.invite.permissions");
			return;
		}
		if (!mPlayer.getData().hasRank(Rank.VIP)) {
			mPlayer.sendMessage("command.error.rank.vip");
			mPlayer.sendMessage("command.error.rank.upgrade");
			return;
		}
		if (!mPlayer.isAuthorEditingLevel()) {
			mPlayer.sendMessage("command.error.edit-only");
			return;
		}
		if (!mPlayer.hasRank(Rank.TITAN)) {
			int guestLimit = PlayableLevelLimits.getRankGuestEditorsLimit(mPlayer.getHighestRank());
			int currentCount = mPlayer.getCurrentLevel().getGuestEditorsCount();
			if (currentCount >= guestLimit) {
				mPlayer.sendMessage("command.maker.invite.error.limit", currentCount);
				mPlayer.sendMessage("player.guest-edit.list", mPlayer.getCurrentLevel().getFormattedGuestEditorsList());
				mPlayer.sendMessage("command.maker.invite.remove");
				mPlayer.sendMessage("upgrade.rank.increase.limits.or");
				mPlayer.sendMessage("upgrade.rank.guest-editors.limits");
				return;
			}
		}
		String nameError = validatePlayerName(args[1]);
		if (nameError != null) {
			mPlayer.sendMessage(nameError);
			return;
		}
		Player guestEditorBukkit = Bukkit.getPlayerExact(args[1]);
		if (guestEditorBukkit == null) {
			mPlayer.sendMessage("player.error.not-found");
			return;
		}
		MakerPlayer guestEditor = plugin.getController().getPlayer(guestEditorBukkit);
		if (guestEditor == null || !guestEditor.isInLobby()) {
			mPlayer.sendMessage("command.error.player-busy");
			return;
		}
		if (!guestEditor.hasRank(Rank.VIP) && !mPlayer.hasRank(Rank.PRO)) {
			mPlayer.sendMessage("command.maker.invite.error.rank");
			return;
		}
		mPlayer.getCurrentLevel().inviteGuestEditor(guestEditor.getName());
		guestEditor.sendMessage("command.maker.invite.guest", mPlayer.getDisplayName(), mPlayer.getName());
		mPlayer.sendMessage("command.maker.invite.author", guestEditor.getDisplayName(), guestEditor.getName());
	}

	private void executeJoinCommand(String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			mPlayer.sendMessage("command.maker.join.usage");
			mPlayer.sendMessage("command.maker.join.permissions");
			return;
		}
		if (!mPlayer.getData().hasRank(Rank.VIP)) {
			mPlayer.sendMessage("command.error.rank.vip");
			mPlayer.sendMessage("command.error.rank.upgrade");
			return;
		}
		if (!mPlayer.isInLobby() && !mPlayer.isSpectating()) {
			mPlayer.sendMessage("command.error.lobby-only");
			return;
		}
		String nameError = validatePlayerName(args[1]);
		if (nameError != null) {
			mPlayer.sendMessage(nameError);
			return;
		}
		if (mPlayer.getName().equalsIgnoreCase(args[1])) {
			mPlayer.sendMessage("command.maker.join.error.self");
			return;
		}
		Player hostEditorBukkit = Bukkit.getPlayerExact(args[1]);
		if (hostEditorBukkit == null) {
			mPlayer.sendMessage("player.error.not-found");
			return;
		}
		MakerPlayer hostEditor = plugin.getController().getPlayer(hostEditorBukkit);
		if (hostEditor == null || !hostEditor.isAuthorEditingLevel()) {
			mPlayer.sendMessage("command.error.player-busy");
			return;
		}
		if (!LevelStatus.EDITING.equals(hostEditor.getCurrentLevel().getStatus())) {
			mPlayer.sendMessage("command.error.level-busy");
			return;
		}
		if (!hostEditor.getCurrentLevel().isGuestEditor(mPlayer.getName())) {
			mPlayer.sendMessage("command.maker.join.error.uninvited");
			return;
		}
		hostEditor.getCurrentLevel().addGuestEditor(mPlayer);
		hostEditor.sendMessage("command.maker.join.author", mPlayer.getDisplayName(), mPlayer.getName());
		mPlayer.sendMessage("command.maker.join.guest", hostEditor.getName());
	}

	private void executeRemoveCommand(String[] args, MakerPlayer mPlayer) {
		if (args.length < 2) {
			mPlayer.sendMessage("command.maker.remove.usage");
			mPlayer.sendMessage("command.maker.remove.permissions");
			return;
		}
		if (!mPlayer.getData().hasRank(Rank.VIP)) {
			mPlayer.sendMessage("command.error.rank.vip");
			mPlayer.sendMessage("command.error.rank.upgrade");
			return;
		}
		if (!mPlayer.isAuthorEditingLevel()) {
			mPlayer.sendMessage("level.edit.error.author-only");
			return;
		}
		String nameError = validatePlayerName(args[1]);
		if (nameError != null) {
			mPlayer.sendMessage(nameError);
			return;
		}
		if (mPlayer.getName().equalsIgnoreCase(args[1])) {
			mPlayer.sendMessage("command.maker.remove.error.self");
			return;
		}
		boolean guestRemoved = mPlayer.getCurrentLevel().removeGuestEditor(args[1]);
		boolean invitationRemoved = mPlayer.getCurrentLevel().removeGuestEditorInvitation(args[1]);
		if (guestRemoved) {
			mPlayer.sendMessage("command.maker.remove.guest-removed", args[1]);
			return;
		}
		if (invitationRemoved){
			mPlayer.sendMessage("command.maker.remove.invitation-removed", args[1]);
			return;
		}
		mPlayer.sendMessage("command.maker.remove.error.uninvited", args[1]);
	}

}
