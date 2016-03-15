package com.minecade.minecraftmaker;

import org.bukkit.Bukkit;

import com.minecade.core.gamebase.MinigameBase;
import com.minecade.core.serverbrowser.ServerDataHolder;
import com.minecade.core.serverbrowser.packets.LocalMenuGameState;

public class MakerServerDataHolder implements ServerDataHolder {
	
	MinigameBase plugin;
	
	public MakerServerDataHolder(MinigameBase plugin) {
		this.plugin = plugin;
	}

	@Override
	public int getCurrentPlayers() {
		return Bukkit.getOnlinePlayers().size();
	}

	@Override
	public int getMaxPlayers() {
		return Bukkit.getServer().getMaxPlayers();
	}

	@Override
	public LocalMenuGameState getGameState() {
		//Since it's just a lobby with instances it is never actually "In game"
		//So this is just a dummy method
		return LocalMenuGameState.IN_LOBBY;
	}

}
