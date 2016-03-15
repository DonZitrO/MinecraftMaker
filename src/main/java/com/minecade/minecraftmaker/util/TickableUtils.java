package com.minecade.minecraftmaker.util;

import org.bukkit.Bukkit;

public class TickableUtils {

	/**
	 * 
	 * We need to make sure that the global tick flow will continue if any object fails
	 * while ticking it, so we catch any exception and try to disable the problematic object.
	 * If we fail to do so, we better shut the entire server down to avoid severe glitches.
	 * 
	 * @param tickable
	 * @param nextTick
	 */
	public static void tickSafely(Tickable tickable, long nextTick) {
		if (!tickable.isEnabled()) {
			return;
		}
		try {
			tickable.tick(nextTick);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("TickableUtils.tickSafely - unable to tick object: [%s] - %s", String.valueOf(tickable), e.getMessage()));
			e.printStackTrace();
			try {
				tickable.disable();
			} catch (Exception e2) {
				Bukkit.getLogger().severe(String.format("TickableUtils.tickSafely - unable to disable object: [%s] after failed tick - %s", String.valueOf(tickable), e2.getMessage()));
				e2.printStackTrace();
				// this is an extreme case, so shut the server down
				Bukkit.shutdown();
			}
		}
	}

	private TickableUtils() {
		super();
	}
}
