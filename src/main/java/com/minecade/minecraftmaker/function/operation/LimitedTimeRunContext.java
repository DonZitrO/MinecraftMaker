package com.minecade.minecraftmaker.function.operation;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LimitedTimeRunContext {

	private long endNanoTime;

	public LimitedTimeRunContext(long nanoTimeLimit) {
		this.endNanoTime = System.nanoTime() + nanoTimeLimit;
	}

	public boolean shouldContinue() {
		long currentNanoTime = System.nanoTime();
		if (currentNanoTime < this.endNanoTime) {
			return true;
		}
		if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
			Bukkit.getLogger().warning(String.format("[DEBUG] | LimitedTimeRunContext.shouldContinue - stopping operation for further resuming..."));
			for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
				Bukkit.getLogger().warning(String.format("[DEBUG] | LimitedTimeRunContext.shouldContinue - stack trace: %s", element));
			}
		}
		return false;
	}

}
