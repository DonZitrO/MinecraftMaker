package com.minecade.minecraftmaker.schematic.extent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.function.operation.ResumableOperationQueue;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.BaseBiome;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.Vector2D;

/**
 * A base class for {@link Extent}s that merely passes extents onto another.
 */
public abstract class AbstractDelegateExtent implements Extent {

	private final Extent extent;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 */
	protected AbstractDelegateExtent(Extent extent) {
		checkNotNull(extent);
		this.extent = extent;
	}

	/**
	 * Get the extent.
	 *
	 * @return the extent
	 */
	public Extent getExtent() {
		return extent;
	}

	@Override
	public BaseBlock getBlock(Vector position) {
		return extent.getBlock(position);
	}

	@Override
	public BaseBlock getLazyBlock(Vector position) {
		return extent.getLazyBlock(position);
	}

	@Override
	public boolean setBlock(Vector location, BaseBlock block) throws MinecraftMakerException {
		return extent.setBlock(location, block);
	}

	@Override
	@Nullable
	public Entity createEntity(Location location, BaseEntity entity) {
		return extent.createEntity(location, entity);
	}

	@Override
	public List<? extends Entity> getEntities() {
		return extent.getEntities();
	}

	@Override
	public List<? extends Entity> getEntities(Region region) {
		return extent.getEntities(region);
	}

	@Override
	public BaseBiome getBiome(Vector2D position) {
		return extent.getBiome(position);
	}

	@Override
	public boolean setBiome(Vector2D position, BaseBiome biome) {
		return extent.setBiome(position, biome);
	}

	@Override
	public Vector getMinimumPoint() {
		return extent.getMinimumPoint();
	}

	@Override
	public Vector getMaximumPoint() {
		return extent.getMaximumPoint();
	}

	protected Operation commitBefore() {
		return null;
	}

	@Override
	public final @Nullable Operation commit() throws MinecraftMakerException {
		Operation ours = commitBefore();
		Operation other = extent.commit();
		if (ours != null && other != null) {
			return new ResumableOperationQueue(ours, other);
		} else if (ours != null) {
			return ours;
		} else if (other != null) {
			return other;
		} else {
			return null;
		}
	}

}
