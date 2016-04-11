package com.minecade.minecraftmaker.task;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.minecraftmaker.function.operation.LimitedTimeRunContext;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.function.operation.ResumableOperationQueue;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

public class LevelOperatorTask extends BukkitRunnable {

	private static final long MAX_TIME_PER_TICK_NANOSECONDS = 5000000; // 5ms to build per tick

	private final MinecraftMakerPlugin plugin;
	private final ResumableOperationQueue operationQueue = new ResumableOperationQueue();

	public LevelOperatorTask(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	public void offer(Operation operation) {
		checkNotNull(operation);
		operationQueue.offer(operation);
	}

	@Override
	public void run() {
		long startNanoTime = 0;
		if (plugin.isDebugMode()) {
			startNanoTime = System.nanoTime();
		}
		try {
			operationQueue.resume(new LimitedTimeRunContext(MAX_TIME_PER_TICK_NANOSECONDS));
			if (plugin.isDebugMode()) {
				// only enable this for specific debugging
				// Bukkit.getLogger().info(String.format("MakerBuilderTask.run - operation took: [%s] nanoseconds", System.nanoTime() - startNanoTime));
			}
		} catch (MinecraftMakerException e) {
			Bukkit.getLogger().severe(String.format("MakerBuilderTask.run - a severe exception occurred on the Builder Task: %s", e.getMessage()));
			e.printStackTrace();
			Bukkit.shutdown();
		}
	}

}
