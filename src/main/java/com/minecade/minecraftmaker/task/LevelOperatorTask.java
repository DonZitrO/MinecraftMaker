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
	private static final long MIN_TIME_PER_TICK_NANOSECONDS =  5000000; // 5ms to guarantee no task will stall forever

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
		long timeAvailable = MAX_TIME_PER_TICK_NANOSECONDS;
		if (lastTickCompleteTime > 0) {
			long timeBetweenRuns = System.nanoTime() - lastTickCompleteTime;
			if (timeBetweenRuns < MIN_TIME_PER_TICK_NANOSECONDS) {
				Bukkit.getLogger().info(String.format("LevelOperatorTask.run - server is too busy, let's give it a break - time between runs: [%s] ms", TimeUnit.NANOSECONDS.toMillis(timeBetweenRuns)));
				lastTickCompleteTime = System.nanoTime();
				return;
			}
			if (timeBetweenRuns < MAX_TIME_PER_TICK_NANOSECONDS) {
				timeAvailable = timeBetweenRuns;
			}
		}
		if (priorityOperationQueue.isEmpty() && operationQueue.isEmpty()) {
			lastTickCompleteTime = System.nanoTime();
			return;
		}
		long startNanoTime = System.nanoTime();
		long divisor = priorityOperationQueue.isEmpty() ^ operationQueue.isEmpty() ? 1 : 2;
		//Bukkit.getLogger().severe(String.format("priority empty: [%s] - normal empty: [%s] - divisor: [%s]",priorityOperationQueue.isEmpty(), operationQueue.isEmpty(), divisor));
		LimitedTimeRunContext runContext = null;
		try {
			if (!priorityOperationQueue.isEmpty()) {
				runContext = new LimitedTimeRunContext(timeAvailable / divisor);
				priorityOperationQueue.resume(runContext);
			}
		} catch (Exception e) {
			priorityOperationQueue.cancelCurrentOperation();
			Bukkit.getLogger().severe(String.format("MakerBuilderTask.run - a severe exception occurred on the Builder Task: %s", e.getMessage()));
			e.printStackTrace();
		}
		long spentNanoTime = System.nanoTime() - startNanoTime; 
		if (plugin.isDebugMode()) {
			if (spentNanoTime > timeAvailable / divisor) {
				Bukkit.getLogger().info(String.format("[DEBUG] | MakerBuilderTask.run - high priority operation took: [%s] ms", TimeUnit.NANOSECONDS.toMillis(spentNanoTime)));
			}
		}
		try {
			if (!operationQueue.isEmpty()) {
				long overtime = runContext != null ? runContext.getOvertime() : 0;
				long availableTime = Math.max(MIN_TIME_PER_TICK_NANOSECONDS, (timeAvailable / divisor) - overtime);
				//Bukkit.getLogger().severe(String.format("available time for low priority task: [%s]", availableTime));
				runContext = new LimitedTimeRunContext(availableTime);
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
