package com.minecade.minecraftmaker.task;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecade.mcore.function.operation.LimitedTimeRunContext;
import com.minecade.mcore.function.operation.Operation;
import com.minecade.mcore.function.operation.ResumableOperationQueue;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class LevelOperatorTask extends BukkitRunnable {

	private static final long MAX_TIME_PER_TICK_NANOSECONDS = 25000000; // 25ms to build per tick
	private static final long MIN_TIME_PER_TICK_NANOSECONDS =  5000000; // 5ms to guarantee no task will stall forever
	private static final long MILLISECOND_NANOS = 1000000;

	private static final long HIGHEST_PRIORITY_DIVISOR = 4;
	private static final long HIGH_PRIORITY_DIVISOR = 3;
	private static final long NORMAL_PRIORITY_DIVISOR = 2;
	private static final long LOW_PRIORITY_DIVISOR = 1;

	private final MinecraftMakerPlugin plugin;
	private final ResumableOperationQueue lowPriorityOperationQueue = new ResumableOperationQueue();
	private final ResumableOperationQueue operationQueue = new ResumableOperationQueue();
	private final ResumableOperationQueue highPriorityoperationQueue = new ResumableOperationQueue();
	private final ResumableOperationQueue highestPriorityoperationQueue = new ResumableOperationQueue();

	private long lastTickCompleteTime;

	public LevelOperatorTask(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	public void offerLowPriority(Operation operation) {
		checkNotNull(operation);
		lowPriorityOperationQueue.offer(operation);
	}

	public void offer(Operation operation) {
		checkNotNull(operation);
		operationQueue.offer(operation);
	}

	public void offerHighPriority(Operation operation) {
		checkNotNull(operation);
		highPriorityoperationQueue.offer(operation);
	}

	public void offerHighestPriority(Operation operation) {
		checkNotNull(operation);
		highestPriorityoperationQueue.offer(operation);
	}

	@Override
	public void run() {
		long totalTimeAvailable = MAX_TIME_PER_TICK_NANOSECONDS;
		if (lastTickCompleteTime > 0) {
			long timeBetweenRuns = System.nanoTime() - lastTickCompleteTime;
			if (timeBetweenRuns < MIN_TIME_PER_TICK_NANOSECONDS) {
				Bukkit.getLogger().warning(String.format("LevelOperatorTask.run - server is too busy, let's give it a break - time between runs: [%s] ms", TimeUnit.NANOSECONDS.toMillis(timeBetweenRuns)));
				lastTickCompleteTime = System.nanoTime();
				return;
			}
			if (timeBetweenRuns < MAX_TIME_PER_TICK_NANOSECONDS) {
				totalTimeAvailable = timeBetweenRuns;
			}
		}
		long totalDivisor = calculateTotalDivisor();
		if (totalDivisor == 0) {
			// no pending tasks
			lastTickCompleteTime = System.nanoTime();
			return;
		}
		long startNanoTime = System.nanoTime();
		long availableTime = totalTimeAvailable;
		long overtime = 0;
		if (!highestPriorityoperationQueue.isEmpty()) {
			availableTime = calculateAvailableTime(HIGHEST_PRIORITY_DIVISOR, totalTimeAvailable, totalDivisor, overtime);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("[DEBUG] | LevelOperatorTask.run - available time for highest priority operation: [%s] ms", TimeUnit.NANOSECONDS.toMillis(availableTime)));
			}
			overtime = resumeQueue(highestPriorityoperationQueue, availableTime);
			if (overtime > MILLISECOND_NANOS) {
				Bukkit.getLogger().warning(String.format("LevelOperatorTask.run - highest priority queue overtime: [%s] ms", TimeUnit.NANOSECONDS.toMillis(overtime)));
			}
		}
		if (!highPriorityoperationQueue.isEmpty()) {
			availableTime = calculateAvailableTime(HIGH_PRIORITY_DIVISOR, totalTimeAvailable, totalDivisor, overtime);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("[DEBUG] | LevelOperatorTask.run - available time for high priority operation: [%s] ms", TimeUnit.NANOSECONDS.toMillis(availableTime)));
			}
			overtime = resumeQueue(highPriorityoperationQueue, availableTime);
			if (overtime > MILLISECOND_NANOS) {
				Bukkit.getLogger().warning(String.format("LevelOperatorTask.run - high priority queue overtime: [%s] ms", TimeUnit.NANOSECONDS.toMillis(overtime)));
			}
		}
		if (!operationQueue.isEmpty()) {
			availableTime = calculateAvailableTime(NORMAL_PRIORITY_DIVISOR, totalTimeAvailable, totalDivisor, overtime);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("[DEBUG] | LevelOperatorTask.run - available time for normal priority operation: [%s] ms", TimeUnit.NANOSECONDS.toMillis(availableTime)));
			}
			overtime = resumeQueue(operationQueue, availableTime);
			if (overtime > MILLISECOND_NANOS) {
				Bukkit.getLogger().warning(String.format("LevelOperatorTask.run - normal priority queue overtime: [%s] ms", TimeUnit.NANOSECONDS.toMillis(overtime)));
			}
		}
		if (!lowPriorityOperationQueue.isEmpty()) {
			availableTime = calculateAvailableTime(LOW_PRIORITY_DIVISOR, totalTimeAvailable, totalDivisor, overtime);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("[DEBUG] | LevelOperatorTask.run - available time for low priority operation: [%s] ms", TimeUnit.NANOSECONDS.toMillis(availableTime)));
			}
			overtime = resumeQueue(lowPriorityOperationQueue, availableTime);
			if (overtime > MILLISECOND_NANOS) {
				Bukkit.getLogger().warning(String.format("LevelOperatorTask.run - low priority queue overtime: [%s] ms", TimeUnit.NANOSECONDS.toMillis(overtime)));
			}
		}
		long totalTimeSpent = System.nanoTime() - startNanoTime;
		if (totalTimeSpent + MILLISECOND_NANOS > totalTimeAvailable) {
			Bukkit.getLogger().warning(String.format("LevelOperatorTask.run - all queues took: [%s] ms", TimeUnit.NANOSECONDS.toMillis(totalTimeSpent)));
		}
		lastTickCompleteTime = System.nanoTime();
	}

	private long resumeQueue(ResumableOperationQueue queue, long nanoTimeAvailable) {
		if (queue.isEmpty()) {
			return 0;
		}
		LimitedTimeRunContext runContext = new LimitedTimeRunContext(nanoTimeAvailable);
		try {
			queue.resume(runContext);
		} catch (Exception e) {
			queue.cancelCurrentOperation();
			Bukkit.getLogger().severe(String.format("MakerBuilderTask.proccessQueue - a severe exception occurred: %s", e.getMessage()));
			e.printStackTrace();
		}
		return runContext.getOvertime();
	}

	private long calculateAvailableTime(long divisor, long totalTimeAvailable, long totalDivisor, long overtime) {
		return Math.max(MIN_TIME_PER_TICK_NANOSECONDS, ((divisor * totalTimeAvailable) / totalDivisor) - overtime);
	}

	private long calculateTotalDivisor() {
		long divisor = 0;
		if (!highestPriorityoperationQueue.isEmpty()) {
			divisor += HIGHEST_PRIORITY_DIVISOR;
		}
		if (!highPriorityoperationQueue.isEmpty()) {
			divisor += HIGH_PRIORITY_DIVISOR;
		}
		if (!operationQueue.isEmpty()) {
			divisor += NORMAL_PRIORITY_DIVISOR;
		}
		if (!lowPriorityOperationQueue.isEmpty()) {
			divisor += LOW_PRIORITY_DIVISOR;
		}
		return divisor;
	}

}
