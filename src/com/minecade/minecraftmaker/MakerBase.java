package com.minecade.minecraftmaker;

import org.bukkit.entity.Player;

import com.minecade.core.MinecadeCore;
import com.minecade.core.gamebase.MinigameBase;
import com.minecade.core.gamebase.MinigamePlayer;
import com.minecade.core.gamebase.MinigameSetup;

public class MakerBase extends MinigameBase {
	
	MinecraftMaker mplugin;

	public MakerBase(MinecraftMaker plugin) {
		super(plugin);
		mplugin = plugin;
	}

	@Override
	public String getMetadataArenaString() {
		return "MakerPlayer";
	}

	@Override
	public void addMinigamePlayer(Player player) {
		MakerPlayer fplayer = new MakerPlayer(player, MinigamePlayer.Type.Player, MakerPlayer.MakerType.Lobby);
		MinecadeCore.getCoreDatabase().requestDataLoad(fplayer, false);
		addMinigamePlayer(fplayer);
	}

	@Override
	public String getPluginPrefix() {
		return null;
	}

	@Override
	public String getCommandPrefix() {
		return "maker";
	}

	@Override
	public String getBaseAdminPermission() {
		return "mcmaker.admin";
	}

	@Override
	public MinigameSetup getNewMinigameArenaSetup(Player player) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveLobby() {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveArenas() {
		// TODO Auto-generated method stub

	}

}
