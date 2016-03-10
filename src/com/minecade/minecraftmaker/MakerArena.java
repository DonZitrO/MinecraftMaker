package com.minecade.minecraftmaker;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.minecade.core.gamebase.MinigameArena;
import com.minecade.core.gamebase.MinigamePlayer.Type;
import com.minecade.serverweb.shared.constants.GameState;

public class MakerArena extends MinigameArena {
	
	enum ArenaType {
		CREATING,
		PLAYING;
	}
	
	ArenaType arenaType = ArenaType.PLAYING;
	ArenaDefinition arenaDef = null;

	/**
	 * Use this method for a player who is just going to be creating a blank arena.
	 * @param base
	 * @param name the name can be blank, as the user will fill it in during arena creation.
	 * @param minPlayers this should be set to 1
	 * @param maxPlayers this should also be set to 1, unless we want more than 1 editor.
	 */
	public MakerArena(MakerBase base, String name, int minPlayers, int maxPlayers) {
		super(base, name, minPlayers, maxPlayers);
		arenaType = ArenaType.CREATING;
	}
	
	public MakerArena(MakerBase base, ArenaDefinition arenaDef, int minPlayers, int maxPlayers) {
		super(base, arenaDef.getName(), minPlayers, maxPlayers);
		this.arenaDef = arenaDef;
	}
	
	public MakerArena(MakerBase base, ArenaDefinition arenaDef, int minPlayers, int maxPlayers, ArenaType arenaType) {
		super(base, arenaDef.getName(), minPlayers, maxPlayers);
		this.arenaDef = arenaDef;
		this.arenaType = arenaType;
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
		if(arenaType == ArenaType.PLAYING) {
			
		}else if(arenaType == ArenaType.CREATING) {
			
		}
		return true;
	}

	@Override
	public boolean isArenaReady() {
		return false;
	}

	public void onEntityDamageEvent(EntityDamageEvent event) {
		// TODO Auto-generated method stub
		
	}

}
