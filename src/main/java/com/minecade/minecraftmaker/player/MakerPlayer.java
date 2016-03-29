package com.minecade.minecraftmaker.player;

import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerPlayer implements Tickable {

	private MakerLevel currentLevel;

	private long currentTick;
	private boolean enabled = true;

	public MakerPlayer() {
		// TODO Auto-generated constructor stub
	}

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
		throw new UnsupportedOperationException("A Player is enabled by default");
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
