package com.minecade.minecraftmaker;

import org.bukkit.entity.Player;

import com.minecade.core.gamebase.MinigamePlayer;

public class MakerPlayer extends MinigamePlayer {
	
	enum MakerType {
		Lobby,
		Creator,
		Player;
	}
	
	MakerType makerType = MakerType.Lobby;
	MakerArena arena = null;

	public MakerPlayer(Player player, Type type, MakerType mtype) {
		super(player, type);
		makerType = mtype;
	}
	
	public MakerArena getArena() {
		return arena;
	}

}
