package com.minecade.minecraftmaker.util;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper methods for enums.
 */
public final class Enums {

	private Enums() {
	}

	/**
	 * Search the given enum for a value that is equal to the one of the given
	 * values, searching in an ascending manner.
	 *
	 * @param enumType
	 *            the enum type
	 * @param values
	 *            the list of values
	 * @param <T>
	 *            the type of enum
	 * @return the found value or null
	 */
	@Nullable
	public static <T extends Enum<T>> T findByValue(Class<T> enumType, String... values) {
		checkNotNull(enumType);
		checkNotNull(values);
		for (String val : values) {
			try {
				return Enum.valueOf(enumType, val);
			} catch (IllegalArgumentException ignored) {
			}
		}
		return null;
	}

}
