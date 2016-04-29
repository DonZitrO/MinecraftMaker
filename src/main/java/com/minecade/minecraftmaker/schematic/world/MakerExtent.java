package com.minecade.minecraftmaker.schematic.world;

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

	private @Nullable FastModeExtent fastModeExtent;
	@SuppressWarnings("unused")
	private final SurvivalModeExtent survivalExtent;

	private final Extent bypassNone;

	public MakerExtent(World world) {
		super();
		this.world = world;
		if (world != null) {
			Extent extent;
			extent = fastModeExtent = new FastModeExtent(world, false);
			extent = survivalExtent = new SurvivalModeExtent(extent, world);
			this.bypassNone = extent;
		} else {
			Extent extent = new NullExtent();
			extent = survivalExtent = new SurvivalModeExtent(extent, NullWorld.getInstance());
			this.bypassNone = extent;
		}
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
		return bypassNone.getBiome(position);
	}

	@Override
	public boolean setBlock(Vector position, BaseBlock block) throws MinecraftMakerException {
		return bypassNone.setBlock(position, block);
	}

	@Override
	public boolean setBiome(Vector2D position, BaseBiome biome) {
		return bypassNone.setBiome(position, biome);
	}

	@Override
	public @Nullable Operation commit() {
		return bypassNone.commit();
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
		return bypassNone.getEntities(region);
	}

	@Override
	public List<? extends Entity> getEntities() {
		return bypassNone.getEntities();
	}

	@Override
	@Nullable
	public Entity createEntity(Location location, BaseEntity entity) {
		return bypassNone.createEntity(location, entity);
	}

	public World getWorld() {
		return world;
	}

}
