package com.minecade.minecraftmaker.function.operation;


public class LimitedTimeRunContext {

	private long endNanoTime;

	public LimitedTimeRunContext(long nanoTimeLimit) {
		this.endNanoTime = System.nanoTime() + nanoTimeLimit;
	}

	public long getRemainingTime() {
		return Math.max(0, this.endNanoTime - System.nanoTime());
	}

	public void addExtraTime(long extraTime) {
		this.endNanoTime += extraTime;
	}

	public boolean shouldContinue() {
		long currentNanoTime = System.nanoTime();
		if (currentNanoTime < this.endNanoTime) {
			return true;
		}
		// heavy logging, uncomment for specific debugging only
		//	if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
		//		Bukkit.getLogger().warning(String.format("[DEBUG] | LimitedTimeRunContext.shouldContinue - stopping operation for further resuming..."));
		//		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
		//			Bukkit.getLogger().warning(String.format("[DEBUG] | LimitedTimeRunContext.shouldContinue - stack trace: %s", element));
		//		}
		//	}
		return false;
	}

}
