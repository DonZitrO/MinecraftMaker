package com.minecade.minecraftmaker.schematic.world;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.util.Location;

public class MakerExtent implements Extent {

	protected final World world;
	private final Extent internalExtent;

	public MakerExtent(World world) {
		checkNotNull(world);
		this.world = world;
		Extent extent;
		extent = new FastModeExtent(world, true);
		extent = new SurvivalModeExtent(extent, world);
		extent = new BlockQuirkExtent(extent, world);
		extent = new ChunkLoadingExtent(extent, world);
		extent = new LastAccessExtentCache(extent);
		extent = new DataValidatorExtent(extent, world);
		// TODO: check if this extent it's useful on mcmaker
		// extent = new BlockBagExtent(extent, blockBag);
		extent = new MultiStageReorder(extent, true);
		this.internalExtent = extent;
	}

	@Override
	public BaseBlock getBlock(Vector position) {
		return world.getBlock(position);
	}

	@Override
	public BaseBlock getLazyBlock(Vector position) {
		return world.getLazyBlock(position);
	}

	@Override
	public BaseBiome getBiome(Vector2D position) {
		return internalExtent.getBiome(position);
	}

	@Override
	public boolean setBlock(Vector position, BaseBlock block) throws MinecraftMakerException {
		return internalExtent.setBlock(position, block);
	}

	@Override
	public boolean setBiome(Vector2D position, BaseBiome biome) {
		return internalExtent.setBiome(position, biome);
	}

	@Override
	public @Nullable Operation commit() {
		return internalExtent.commit();
	}

	@Override
	public Vector getMinimumPoint() {
		return getWorld().getMinimumPoint();
	}

	@Override
	public Vector getMaximumPoint() {
		return getWorld().getMaximumPoint();
	}

	@Override
	public List<? extends Entity> getEntities(Region region) {
		return internalExtent.getEntities(region);
	}

	@Override
	public List<? extends Entity> getEntities() {
		return internalExtent.getEntities();
	}

	@Override
	@Nullable
	public Entity createEntity(Location location, BaseEntity entity) {
		return internalExtent.createEntity(location, entity);
	}

	public World getWorld() {
		return world;
	}

}
