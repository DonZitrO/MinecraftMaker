package com.minecade.minecraftmaker.util;

import org.bukkit.Bukkit;

public interface Tickable {

	default void disable(String reason, Exception exception) {
		Bukkit.getLogger().warning(String.format("Tickable.disable - disable request for object: %s", getDescription()));
		if (reason != null) {
			Bukkit.getLogger().warning(String.format("Tickable.disable - reason: %s", reason));
		}
		StackTraceElement[] stackTrace = exception != null ? exception.getStackTrace() : Thread.currentThread().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			Bukkit.getLogger().warning(String.format("Tickable.disable - stack trace: %s", element));
		}
		if (isDisabled()) {
			return;
		}
		disable();
	}

	default void disable(String reason) {
		disable(reason, null);
	}

	public void disable();

	public long getCurrentTick();

	public boolean isDisabled();

	public void tick(long currentTick);

	public String getDescription();

}
