package com.minecade.minecraftmaker.schematic.function.visitor;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.function.EntityFunction;
import com.minecade.minecraftmaker.schematic.function.operation.Operation;
import com.minecade.minecraftmaker.schematic.function.operation.RunContext;

/**
 * Visits entities as provided by an {@code Iterator}.
 */
public class EntityVisitor implements Operation {

	private final Iterator<? extends Entity> iterator;
	private final EntityFunction function;
	private int affected = 0;

	/**
	 * Create a new instance.
	 *
	 * @param iterator
	 *            the iterator
	 * @param function
	 *            the function
	 */
	public EntityVisitor(Iterator<? extends Entity> iterator, EntityFunction function) {
		checkNotNull(iterator);
		checkNotNull(function);
		this.iterator = iterator;
		this.function = function;
	}

	/**
	 * Get the number of affected objects.
	 *
	 * @return the number of affected
	 */
	public int getAffected() {
		return affected;
	}

	@Override
	public Operation resume(RunContext run) throws MinecraftMakerException {
		while (iterator.hasNext()) {
			if (function.apply(iterator.next())) {
				affected++;
			}
		}

		return null;
	}

	@Override
	public void cancel() {
	}

	@Override
	public void addStatusMessages(List<String> messages) {
		messages.add(getAffected() + " entities affected");
	}

}
