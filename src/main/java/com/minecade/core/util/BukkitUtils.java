package com.minecade.core.util;

import org.bukkit.Bukkit;

public class BukkitUtils {

	public static void verifyNotPrimaryThread() {
		if (Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method should not be called from the main thread");
		}
	}

	public static void verifyPrimaryThread() {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
	}

	private BukkitUtils() {
		super();
	}

}
