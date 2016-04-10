package com.minecade.minecraftmaker.task;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class AsyncLevelSaverTask extends BukkitRunnable {

	private MinecraftMakerPlugin plugin;

	private Queue<MakerLevel> pendingSaves = new ConcurrentLinkedQueue<>();

	public AsyncLevelSaverTask(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		if (Bukkit.isPrimaryThread()) {
			Bukkit.getLogger().severe(String.format("AsyncLevelSaverTask.run - running in primary thread!"));
			return;
		}
		int savesToFlush = pendingSaves.size();
		if (savesToFlush > 0 && plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelSaverTask.run - Flushing [%s] pending level saves...", pendingSaves.size()));
		}
		MakerLevel level = pendingSaves.poll();
		while (level != null) {
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelSaverTask.run - Flushing pending level save with level id: [%s]", level.getLevelId()));
			}
			plugin.getDatabaseAdapter().saveLevel(level);
			level = pendingSaves.poll();
		}
		if (savesToFlush > 0 && plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | AsyncLevelSaverTask.run - Pending level saves flushed", pendingSaves.size()));
		}
	}

	public void saveLevelAsync(MakerLevel level) {
		pendingSaves.add(level);
	}

}
