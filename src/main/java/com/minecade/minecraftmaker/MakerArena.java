package com.minecade.minecraftmaker;

import java.util.UUID;

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
	MakerSchematic schematic = null;
	UUID id = UUID.randomUUID();
	SlotBoundaries slot = null;

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
		this.status = status;
	}

	@Override
	public boolean startGame() {
		setStatus(GameState.IN_GAME);
		if(arenaType == ArenaType.PLAYING) {
			((MakerBase)base).loadArenaSchematic(this, arenaDef);
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
	
	public void setArenaSchematic(MakerSchematic schematic) {
		this.schematic = schematic;
		if(getStatus() == GameState.IN_GAME) {
			pasteSchematic();
			//TODO: Let's get the player set up to play the game!
		}
	}
	
	public void pasteSchematic() {
		schematic.pasteSchematic(slot);
	}
	
	public UUID getUniqueID() {
		return id;
	}
	
	public void setSlot(SlotBoundaries slot) {
		this.slot = slot;
	}
	
	public SlotBoundaries getSlot() {
		return slot;
	}

}
