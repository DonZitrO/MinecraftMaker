package com.minecade.minecraftmaker.bukkit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {

	/**
	 * List of top level NBT fields that should not be copied to a world, such
	 * as UUIDLeast and UUIDMost.
	 */
	public static final List<String> NO_COPY_ENTITY_NBT_FIELDS;

	static {
		NO_COPY_ENTITY_NBT_FIELDS = Collections.unmodifiableList(Arrays.asList("UUIDLeast", "UUIDMost", // Bukkit and Vanilla
		        "WorldUUIDLeast", "WorldUUIDMost", // Bukkit and Vanilla
		        "PersistentIDMSB", "PersistentIDLSB" // Forge
		));
	}

	private Constants() {
	}

}
