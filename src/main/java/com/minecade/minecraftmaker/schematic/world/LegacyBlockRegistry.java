package com.minecade.minecraftmaker.schematic.world;

import java.util.Map;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockMaterial;

/**
 * A block registry that uses {@link BundledBlockData} to serve information
 * about blocks.
 */
public class LegacyBlockRegistry implements BlockRegistry {

	@Nullable
	@Override
	public BaseBlock createFromId(String id) {
		Integer legacyId = BundledBlockData.getInstance().toLegacyId(id);
		if (legacyId != null) {
			return createFromId(legacyId);
		} else {
			return null;
		}
	}

	@Nullable
	@Override
	public BaseBlock createFromId(int id) {
		return new BaseBlock(id);
	}

	@Nullable
	@Override
	public BlockMaterial getMaterial(BaseBlock block) {
		return BundledBlockData.getInstance().getMaterialById(block.getId());
	}

	@Nullable
	@Override
	public Map<String, ? extends State> getStates(BaseBlock block) {
		return BundledBlockData.getInstance().getStatesById(block.getId());
	}

}
