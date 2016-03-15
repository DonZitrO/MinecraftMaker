package com.minecade.minecraftmaker;

import org.bukkit.entity.Player;

import com.minecade.core.gamebase.MinigamePlayer;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerPlayer extends MinigamePlayer implements Tickable {
	
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

	private long currentTick;
	private boolean enabled = true;

	@Override
	public void disable() {
		if (!enabled) {
			return;
		}
		// TODO: disable logic
		enabled = false;
	}

	@Override
	public void enable() {
		throw new UnsupportedOperationException("An Player is enabled by default");
	}

	@Override
	public long getCurrentTick() {
		return currentTick;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
	}

}
