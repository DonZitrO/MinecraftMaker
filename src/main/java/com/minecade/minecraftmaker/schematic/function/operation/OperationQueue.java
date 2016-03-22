package com.minecade.minecraftmaker.schematic.function.operation;

import com.google.common.collect.Lists;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Executes multiple queues in order.
 */
public class OperationQueue implements Operation {

	private final List<Operation> operations = Lists.newArrayList();
	private final Deque<Operation> queue = new ArrayDeque<Operation>();
	private Operation current;

	/**
	 * Create a new queue containing no operations.
	 */
	public OperationQueue() {
	}

	/**
	 * Create a new queue with operations from the given collection.
	 *
	 * @param operations
	 *            a collection of operations
	 */
	public OperationQueue(Collection<Operation> operations) {
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
	public OperationQueue(Operation... operation) {
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

	@Override
	public Operation resume(RunContext run) throws MinecraftMakerException {
		if (current == null && !queue.isEmpty()) {
			current = queue.poll();
		}

		if (current != null) {
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

}
