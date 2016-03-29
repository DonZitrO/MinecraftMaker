package com.minecade.minecraftmaker.level;

import java.util.UUID;

import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.util.Tickable;

public class MakerLevel implements Tickable {

	private UUID levelId;
	private String levelName;
	private Clipboard clipboard;

	private UUID authorId;
	private String authorName;
	private long favs;
	private long likes;
	private long dislikes;

	private UUID currentPlayerId;

	private long currentTick;
	private boolean enabled = false;

	public MakerLevel() {
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
		throw new UnsupportedOperationException("An Arena is enabled by default");
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
