package com.minecade.minecraftmaker.task;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class AsyncPlayerCounterUpdaterTask extends BukkitRunnable {

	private MinecraftMakerPlugin plugin;

	public AsyncPlayerCounterUpdaterTask(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		if (Bukkit.isPrimaryThread()) {
			Bukkit.getLogger().severe(String.format("AsyncPlayerCounterUpdater.run - running in primary thread!"));
			return;
		}
		long start = 0;
		if (plugin.isDebugMode()) {
			start = System.nanoTime();
		}
		plugin.getDatabaseAdapter().updatePlayersCount();
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | AsyncPlayerCounterUpdater.run - Updating player count on DB took [%s] nanoseconds", System.nanoTime() - start));
		}
		if (plugin.isDebugMode()) {
			start = System.nanoTime();
		}
//		try {
//			plugin.getMessagingAdapter().publishServerInfo(plugin.getSCBServerInfo());
//			if (plugin.isDebugMode()) {
//				Bukkit.getLogger().info(String.format("[DEBUG] | AsyncPlayerCounterUpdater.run - Updating player count on RabbitMQ took [%s] nanoseconds", System.nanoTime() - start));
//			}
//		} catch (IOException e) {
//			Bukkit.getLogger().severe(String.format("AsyncPlayerCounterUpdater.run - Unable to update player count on RabbitMQ: %s", e.getMessage()));
//			e.printStackTrace();
//		}
	}
}
