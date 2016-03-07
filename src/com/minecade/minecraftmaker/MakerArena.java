package com.minecade.minecraftmaker;

import org.bukkit.entity.Player;

import com.minecade.core.gamebase.MinigameArena;
import com.minecade.core.gamebase.MinigamePlayer.Type;
import com.minecade.serverweb.shared.constants.GameState;

public class MakerArena extends MinigameArena {
	
	enum ArenaType {
		CREATING,
		PLAYING;
	}
	
	ArenaType arenaType = ArenaType.PLAYING;

	public MakerArena(MakerBase base, String name, int minPlayers,
			int maxPlayers) {
		super(base, name, minPlayers, maxPlayers);
	}

	@Override
	public boolean addPlayer(Player player, Type type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removePlayer(Player player) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatus(GameState status) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean startGame() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isArenaReady() {
		return false;
	}

}
