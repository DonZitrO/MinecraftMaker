package com.minecade.minecraftmaker.function;

import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

/**
 * Applies a function to entities.
 */
public interface EntityFunction {

	/**
	 * Apply the function to the entity.
	 *
	 * @param entity
	 *            the entity
	 * @return true if something was changed
	 * @throws WorldEditException
	 *             thrown on an error
	 */
	public boolean apply(Entity entity) throws MinecraftMakerException;

}
