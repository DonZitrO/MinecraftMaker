package com.minecade.minecraftmaker.schematic.world;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.entity.BaseEntity;

/**
 * An implementation of an entity registry that knows nothing.
 */
public class NullEntityRegistry implements EntityRegistry {

	@Nullable
	@Override
	public BaseEntity createFromId(String id) {
		return null;
	}

}
