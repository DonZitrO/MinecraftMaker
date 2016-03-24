package com.minecade.minecraftmaker.schematic.transform;

import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Makes a transformation of {@link Vector}s.
 */
public interface Transform {

	/**
	 * Return whether this transform is an identity.
	 *
	 * <p>
	 * If it is not known, then {@code false} must be returned.
	 * </p>
	 *
	 * @return true if identity
	 */
	boolean isIdentity();

	/**
	 * Returns the result of applying the function to the input.
	 *
	 * @param input
	 *            the input
	 * @return the result
	 */
	Vector apply(Vector input);

	/**
	 * Create a new inverse transform.
	 *
	 * @return a new inverse transform
	 */
	Transform inverse();

	/**
	 * Create a new {@link Transform} that combines this transform with another.
	 *
	 * @param other
	 *            the other transform to occur second
	 * @return a new transform
	 */
	Transform combine(Transform other);

}
