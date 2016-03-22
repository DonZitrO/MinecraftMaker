package com.minecade.minecraftmaker.schematic.world;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.block.BaseItem;

public class NullItemRegistry implements ItemRegistry {

	@Nullable
	@Override
	public BaseItem createFromId(String id) {
		return null;
	}

	@Nullable
	@Override
	public BaseItem createFromId(int id) {
		return null;
	}

}
