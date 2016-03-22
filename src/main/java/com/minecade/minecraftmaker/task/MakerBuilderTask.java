package com.minecade.minecraftmaker.task;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.function.operation.LimitedTimeRunContext;
import com.minecade.minecraftmaker.schematic.function.operation.Operation;
import com.minecade.minecraftmaker.schematic.function.operation.PausableOperationQueue;

public class MakerBuilderTask extends BukkitRunnable {

	private static final long MAX_TIME_PER_TICK_NANOSECONDS = 5000000; // 5ms

	private final PausableOperationQueue operationQueue = new PausableOperationQueue();

	public void offer(Operation operation) {
		checkNotNull(operation);
		operationQueue.offer(operation);
	}

	@Override
	public void run() {
		try {
			operationQueue.resume(new LimitedTimeRunContext(MAX_TIME_PER_TICK_NANOSECONDS));
		} catch (MinecraftMakerException e) {
			Bukkit.getLogger().severe(String.format("MakerBuilderTask.run - a severe exception occurred on the Builder Task: %s", e.getMessage()));
			e.printStackTrace();
			Bukkit.shutdown();
		}
	}

}
