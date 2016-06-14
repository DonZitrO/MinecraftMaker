package com.minecade.minecraftmaker.function.operation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import com.google.common.collect.Lists;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

/**
 * Executes multiple queues in order. It can be paused for later resuming
 */
public class ResumableOperationQueue implements Operation {

	private final List<Operation> operations = Lists.newArrayList();
	private final Deque<Operation> queue = new ArrayDeque<Operation>();
	private Operation current;

	/**
	 * Create a new queue containing no operations.
	 */
	public ResumableOperationQueue() {
	}

	/**
	 * Create a new queue with operations from the given collection.
	 *
	 * @param operations
	 *            a collection of operations
	 */
	public ResumableOperationQueue(Collection<Operation> operations) {
		checkNotNull(operations);
		for (Operation operation : operations) {
			offer(operation);
		}
		this.operations.addAll(operations);
	}

	/**
	 * Create a new queue with operations from the given array.
	 *
	 * @param operation
	 *            an array of operations
	 */
	public ResumableOperationQueue(Operation... operation) {
		checkNotNull(operation);
		for (Operation o : operation) {
			offer(o);
		}
	}

	/**
	 * Add a new operation to the queue.
	 *
	 * @param operation
	 *            the operation
	 */
	public void offer(Operation operation) {
		checkNotNull(operation);
		queue.offer(operation);
	}

//	/**
//	 * Add a new operation to the queue with top priority.
//	 *
//	 * @param operation
//	 *            the operation
//	 */
//	public void offerFirst(Operation operation) {
//		checkNotNull(operation);
//		queue.offerFirst(operation);
//	}

	/**
	 * Cancel the current operation of the queue.
	 * 
	 * WARNING: this should only be called for MASTER operations.
	 * if the operation is part of a bigger operation this call
	 * could lead to non deterministic behavior
	 *
	 * @param operation
	 *            the operation
	 */
	public void cancelCurrentOperation() {
		if (current != null) {
			current.cancel();
			current = null;
		}
	}

	@Override
	public Operation resume(LimitedTimeRunContext run) throws MinecraftMakerException {
		if (current == null && !queue.isEmpty()) {
			current = queue.poll();
		}
		if (!run.shouldContinue()) {
			return current != null ? this : null;
		}
		if (current != null) {
			if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
				// Bukkit.getLogger().info(String.format("[DEBUG] | ResumableOperationQueue.resume - about to resume operation: [%s]", current));
			}
			current = current.resume(run);
			if (current == null) {
				current = queue.poll();
			}
		}
		return current != null ? this : null;
	}

	@Override
	public void cancel() {
		for (Operation operation : queue) {
			operation.cancel();
		}
		queue.clear();
	}

	@Override
	public void addStatusMessages(List<String> messages) {
		for (Operation operation : operations) {
			operation.addStatusMessages(messages);
		}
	}

	public boolean isEmpty() {
		return current == null && queue.isEmpty();
	}

}
