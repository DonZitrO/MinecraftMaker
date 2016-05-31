package com.minecade.minecraftmaker.task;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.minecraftmaker.function.operation.LimitedTimeRunContext;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.function.operation.ResumableOperationQueue;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelOperatorTask extends BukkitRunnable {

	private static final long MAX_TIME_PER_TICK_NANOSECONDS = 20000000; // 20ms to build per tick

	private final MinecraftMakerPlugin plugin;
	private final ResumableOperationQueue operationQueue = new ResumableOperationQueue();
	private final ResumableOperationQueue priorityOperationQueue = new ResumableOperationQueue();
	private long lastTickCompleteTime;

	public LevelOperatorTask(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	public void offerLowPriority(Operation operation) {
		checkNotNull(operation);
		operationQueue.offer(operation);
	}

	public void offer(Operation operation) {
		checkNotNull(operation);
		operationQueue.offerFirst(operation);
	}

	public void offerHighPriority(Operation operation) {
		checkNotNull(operation);
		priorityOperationQueue.offer(operation);
	}

	public void offerHighestPriority(Operation operation) {
		checkNotNull(operation);
		priorityOperationQueue.offerFirst(operation);
	}

	@Override
	public void run() {
		if (lastTickCompleteTime > 0) {
			long timeBetweenRuns = System.nanoTime() - lastTickCompleteTime;
			if (timeBetweenRuns < MAX_TIME_PER_TICK_NANOSECONDS) {
				Bukkit.getLogger().info(String.format("[DEBUG] | LevelOperatorTask.run - server is too busy, let's give it a break - time between runs: [%s] ms", TimeUnit.NANOSECONDS.toMillis(timeBetweenRuns)));
				lastTickCompleteTime = System.nanoTime();
				return;
			}
		}
		if (priorityOperationQueue.isEmpty() && operationQueue.isEmpty()) {
			lastTickCompleteTime = System.nanoTime();
			return;
		}
		long startNanoTime = System.nanoTime();
		long divisor = priorityOperationQueue.isEmpty() ^ operationQueue.isEmpty() ? 1 : 2;
		LimitedTimeRunContext runContext = new LimitedTimeRunContext(MAX_TIME_PER_TICK_NANOSECONDS / divisor);
		try {
			if (!priorityOperationQueue.isEmpty()) {
				priorityOperationQueue.resume(runContext);
			}
		} catch (Exception e) {
			priorityOperationQueue.cancelCurrentOperation();
			Bukkit.getLogger().severe(String.format("MakerBuilderTask.run - a severe exception occurred on the Builder Task: %s", e.getMessage()));
			e.printStackTrace();
		}
		long spentNanoTime = System.nanoTime() - startNanoTime; 
		if (plugin.isDebugMode()) {
			if (spentNanoTime > MAX_TIME_PER_TICK_NANOSECONDS / divisor) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerBuilderTask.run - high priority operation took: [%s] ms", TimeUnit.NANOSECONDS.toMillis(spentNanoTime)));
			}
		}
		try {
			if (!operationQueue.isEmpty()) {
				operationQueue.resume(runContext);
			}
		} catch (Exception e) {
			operationQueue.cancelCurrentOperation();
			Bukkit.getLogger().severe(String.format("MakerBuilderTask.run - a severe exception occurred on the Builder Task: %s", e.getMessage()));
			e.printStackTrace();
		}
		spentNanoTime = System.nanoTime() - startNanoTime; 
		if (plugin.isDebugMode()) {
			if (spentNanoTime > MAX_TIME_PER_TICK_NANOSECONDS) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerBuilderTask.run - all operations took: [%s] ms", TimeUnit.NANOSECONDS.toMillis(spentNanoTime)));
			}
		}
		lastTickCompleteTime = System.nanoTime();
	}

}
