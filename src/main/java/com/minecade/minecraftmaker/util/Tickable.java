package com.minecade.minecraftmaker.util;

public interface Tickable {

	public void disable();

	public void enable();

	public long getCurrentTick();

	public boolean isEnabled();

	public void tick(long currentTick);

}
