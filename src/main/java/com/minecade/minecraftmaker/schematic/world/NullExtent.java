package com.minecade.minecraftmaker.schematic.world;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.util.Location;

/**
 * An extent that returns air blocks for all blocks and does not pass on any
 * changes.
 */
public class NullExtent implements Extent {

	private final Vector nullPoint = new Vector(0, 0, 0);

	@Override
	public Vector getMinimumPoint() {
		return nullPoint;
	}

	@Override
	public Vector getMaximumPoint() {
		return nullPoint;
	}

	@Override
	public List<Entity> getEntities(Region region) {
		return Collections.emptyList();
	}

	@Override
	public List<Entity> getEntities() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public Entity createEntity(Location location, BaseEntity entity) {
		return null;
	}

	@Override
	public BaseBlock getBlock(Vector position) {
		return new BaseBlock(0);
	}

	@Override
	public BaseBlock getLazyBlock(Vector position) {
		return new BaseBlock(0);
	}

	@Nullable
	@Override
	public BaseBiome getBiome(Vector2D position) {
		return null;
	}

	@Override
	public boolean setBlock(Vector position, BaseBlock block) throws MinecraftMakerException {
		return false;
	}

	@Override
	public boolean setBiome(Vector2D position, BaseBiome biome) {
		return false;
	}

	@Nullable
	@Override
	public Operation commit() {
		return null;
	}

}
