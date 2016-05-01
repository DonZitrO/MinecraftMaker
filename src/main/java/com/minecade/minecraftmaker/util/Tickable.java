package com.minecade.minecraftmaker.util;

public interface Tickable {

	public void disable(String reason, Exception exception);

	public long getCurrentTick();

	public boolean isDisabled();

	public void tick(long currentTick);

}
