package com.minecade.minecraftmaker.task;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.minecraftmaker.function.operation.LimitedTimeRunContext;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.function.operation.ResumableOperationQueue;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelOperatorTask extends BukkitRunnable {

	private static final long MAX_TIME_PER_TICK_NANOSECONDS = 5000000; // 5ms to build per tick

	private final MinecraftMakerPlugin plugin;
	private final ResumableOperationQueue operationQueue = new ResumableOperationQueue();
	private final ResumableOperationQueue priorityOperationQueue = new ResumableOperationQueue();

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
		if (priorityOperationQueue.isEmpty() && operationQueue.isEmpty()) {
			return;
		}
		long startNanoTime = 0;
		if (plugin.isDebugMode()) {
			startNanoTime = System.nanoTime();
		}
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
		try {
			runContext.addExtraTime(MAX_TIME_PER_TICK_NANOSECONDS/2);
			if (!operationQueue.isEmpty()) {
				operationQueue.resume(runContext);
			}
		} catch (Exception e) {
			operationQueue.cancelCurrentOperation();
			Bukkit.getLogger().severe(String.format("MakerBuilderTask.run - a severe exception occurred on the Builder Task: %s", e.getMessage()));
			e.printStackTrace();
		}
		if (plugin.isDebugMode()) {
			long totalNanoTime = System.nanoTime() - startNanoTime;
			if (totalNanoTime > MAX_TIME_PER_TICK_NANOSECONDS) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerBuilderTask.run - operation took: [%s] nanoseconds", totalNanoTime));
			}
		}
	}

}
