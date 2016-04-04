package com.minecade.minecraftmaker.function.mask;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.Vector2D;

/**
 * Various utility functions related to {@link Mask} and {@link Mask2D}.
 */
public final class Masks {

	private static final AlwaysTrue ALWAYS_TRUE = new AlwaysTrue();
	private static final AlwaysFalse ALWAYS_FALSE = new AlwaysFalse();

	/**
	 * Return a 3D mask that always returns true;
	 *
	 * @return a mask
	 */
	public static Mask alwaysTrue() {
		return ALWAYS_TRUE;
	}

	/**
	 * Return a 2D mask that always returns true;
	 *
	 * @return a mask
	 */
	public static Mask2D alwaysTrue2D() {
		return ALWAYS_TRUE;
	}

	/**
	 * Negate the given mask.
	 *
	 * @param mask
	 *            the mask
	 * @return a new mask
	 */
	public static Mask negate(final Mask mask) {
		if (mask instanceof AlwaysTrue) {
			return ALWAYS_FALSE;
		} else if (mask instanceof AlwaysFalse) {
			return ALWAYS_TRUE;
		}

		checkNotNull(mask);
		return new AbstractMask() {
			@Override
			public boolean test(Vector vector) {
				return !mask.test(vector);
			}

			@Nullable
			@Override
			public Mask2D toMask2D() {
				Mask2D mask2d = mask.toMask2D();
				if (mask2d != null) {
					return negate(mask2d);
				} else {
					return null;
				}
			}
		};
	}

	/**
	 * Negate the given mask.
	 *
	 * @param mask
	 *            the mask
	 * @return a new mask
	 */
	public static Mask2D negate(final Mask2D mask) {
		if (mask instanceof AlwaysTrue) {
			return ALWAYS_FALSE;
		} else if (mask instanceof AlwaysFalse) {
			return ALWAYS_TRUE;
		}

		checkNotNull(mask);
		return new AbstractMask2D() {
			@Override
			public boolean test(Vector2D vector) {
				return !mask.test(vector);
			}
		};
	}

	/**
	 * Return a 3-dimensional version of a 2D mask.
	 *
	 * @param mask
	 *            the mask to make 3D
	 * @return a 3D mask
	 */
	public static Mask asMask(final Mask2D mask) {
		return new AbstractMask() {
			@Override
			public boolean test(Vector vector) {
				return mask.test(vector.toVector2D());
			}

			@Nullable
			@Override
			public Mask2D toMask2D() {
				return mask;
			}
		};
	}

	private static class AlwaysTrue implements Mask, Mask2D {
		@Override
		public boolean test(Vector vector) {
			return true;
		}

		@Override
		public boolean test(Vector2D vector) {
			return true;
		}

		@Nullable
		@Override
		public Mask2D toMask2D() {
			return this;
		}
	}

	private static class AlwaysFalse implements Mask, Mask2D {
		@Override
		public boolean test(Vector vector) {
			return false;
		}

		@Override
		public boolean test(Vector2D vector) {
			return false;
		}

		@Nullable
		@Override
		public Mask2D toMask2D() {
			return this;
		}
	}

	private Masks() {
		super();
	}

}
