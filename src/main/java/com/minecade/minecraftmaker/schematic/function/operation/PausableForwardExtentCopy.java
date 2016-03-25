package com.minecade.minecraftmaker.schematic.function.operation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.MinecraftMaker;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.function.CombinedRegionFunction;
import com.minecade.minecraftmaker.schematic.function.RegionFunction;
import com.minecade.minecraftmaker.schematic.function.RegionMaskingFilter;
import com.minecade.minecraftmaker.schematic.function.block.ExtentBlockCopy;
import com.minecade.minecraftmaker.schematic.function.entity.ExtentEntityCopy;
import com.minecade.minecraftmaker.schematic.function.mask.Mask;
import com.minecade.minecraftmaker.schematic.function.mask.Masks;
import com.minecade.minecraftmaker.schematic.function.visitor.EntityVisitor;
import com.minecade.minecraftmaker.schematic.function.visitor.PausableRegionVisitor;
import com.minecade.minecraftmaker.schematic.transform.Identity;
import com.minecade.minecraftmaker.schematic.transform.Transform;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Makes a copy of a portion of one extent to another extent or another point.
 *
 * <p>
 * This is a pausable forward extent copy, meaning that it iterates over the blocks in
 * the source extent, and will copy as many blocks as there are in the source.
 * Therefore, interpolation will not occur to fill in the gaps.
 * </p>
 */
public class PausableForwardExtentCopy implements Operation {

	private final Extent source;
	private final Extent destination;
	private final Region region;
	private final Vector from;
	private final Vector to;
	private int repetitions = 1;
	private Mask sourceMask = Masks.alwaysTrue();
	private boolean removingEntities;
	private RegionFunction sourceFunction = null;
	private Transform transform = new Identity();
	private Transform currentTransform = null;
	private PausableRegionVisitor lastVisitor;
	private int affected;

	/**
	 * Create a new copy using the region's lowest minimum point as the "from"
	 * position.
	 *
	 * @param source
	 *            the source extent
	 * @param region
	 *            the region to copy
	 * @param destination
	 *            the destination extent
	 * @param to
	 *            the destination position
	 * @see #ForwardExtentCopy(Extent, Region, Vector, Extent, Vector) the main
	 *      constructor
	 */
	public PausableForwardExtentCopy(Extent source, Region region, Extent destination, Vector to) {
		this(source, region, region.getMinimumPoint(), destination, to);
	}

	/**
	 * Create a new copy.
	 *
	 * @param source
	 *            the source extent
	 * @param region
	 *            the region to copy
	 * @param from
	 *            the source position
	 * @param destination
	 *            the destination extent
	 * @param to
	 *            the destination position
	 */
	public PausableForwardExtentCopy(Extent source, Region region, Vector from, Extent destination, Vector to) {
		checkNotNull(source);
		checkNotNull(region);
		checkNotNull(from);
		checkNotNull(destination);
		checkNotNull(to);
		this.source = source;
		this.destination = destination;
		this.region = region;
		this.from = from;
		this.to = to;
	}

	/**
	 * Get the transformation that will occur on every point.
	 *
	 * <p>
	 * The transformation will stack with each repetition.
	 * </p>
	 *
	 * @return a transformation
	 */
	public Transform getTransform() {
		return transform;
	}

	/**
	 * Set the transformation that will occur on every point.
	 *
	 * @param transform
	 *            a transformation
	 * @see #getTransform()
	 */
	public void setTransform(Transform transform) {
		checkNotNull(transform);
		this.transform = transform;
	}

	/**
	 * Get the mask that gets applied to the source extent.
	 *
	 * <p>
	 * This mask can be used to filter what will be copied from the source.
	 * </p>
	 *
	 * @return a source mask
	 */
	public Mask getSourceMask() {
		return sourceMask;
	}

	/**
	 * Set a mask that gets applied to the source extent.
	 *
	 * @param sourceMask
	 *            a source mask
	 * @see #getSourceMask()
	 */
	public void setSourceMask(Mask sourceMask) {
		checkNotNull(sourceMask);
		this.sourceMask = sourceMask;
	}

	/**
	 * Get the function that gets applied to all source blocks <em>after</em>
	 * the copy has been made.
	 *
	 * @return a source function, or null if none is to be applied
	 */
	public RegionFunction getSourceFunction() {
		return sourceFunction;
	}

	/**
	 * Set the function that gets applied to all source blocks <em>after</em>
	 * the copy has been made.
	 *
	 * @param function
	 *            a source function, or null if none is to be applied
	 */
	public void setSourceFunction(RegionFunction function) {
		this.sourceFunction = function;
	}

	/**
	 * Get the number of repetitions left.
	 *
	 * @return the number of repetitions
	 */
	public int getRepetitions() {
		return repetitions;
	}

	/**
	 * Set the number of repetitions left.
	 *
	 * @param repetitions
	 *            the number of repetitions
	 */
	public void setRepetitions(int repetitions) {
		checkArgument(repetitions >= 0, "number of repetitions must be non-negative");
		this.repetitions = repetitions;
	}

	/**
	 * Return whether entities that are copied should be removed.
	 *
	 * @return true if removing
	 */
	public boolean isRemovingEntities() {
		return removingEntities;
	}

	/**
	 * Set whether entities that are copied should be removed.
	 *
	 * @param removingEntities
	 *            true if removing
	 */
	public void setRemovingEntities(boolean removingEntities) {
		this.removingEntities = removingEntities;
	}

	/**
	 * Get the number of affected objects.
	 *
	 * @return the number of affected
	 */
	public int getAffected() {
		return affected;
	}

	@Override
	public Operation resume(RunContext run) throws MinecraftMakerException {
		Operation toResume = null;
		long startNanoTime = 0;
		if (MinecraftMaker.getInstance().isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | PausableForwardExtentCopy.resume - start..."));
			startNanoTime = System.nanoTime();
		}
		if (lastVisitor != null) {
			affected += lastVisitor.getAffected();
			lastVisitor = null;
		}

		if (repetitions > 0) {
			repetitions--;

			if (currentTransform == null) {
				currentTransform = transform;
			}

			ExtentBlockCopy blockCopy = new ExtentBlockCopy(source, from, destination, to, currentTransform);
			RegionMaskingFilter filter = new RegionMaskingFilter(sourceMask, blockCopy);
			RegionFunction function = sourceFunction != null ? new CombinedRegionFunction(filter, sourceFunction) : filter;
			PausableRegionVisitor blockVisitor = new PausableRegionVisitor(region, function);

			ExtentEntityCopy entityCopy = new ExtentEntityCopy(from, destination, to, currentTransform);
			entityCopy.setRemoving(removingEntities);
			List<? extends Entity> entities = source.getEntities(region);
			EntityVisitor entityVisitor = new EntityVisitor(entities.iterator(), entityCopy);

			lastVisitor = blockVisitor;
			currentTransform = currentTransform.combine(transform);
			toResume = new DelegateOperation(this, new ResumableOperationQueue(blockVisitor, entityVisitor));
		}
		if (MinecraftMaker.getInstance().isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | PausableForwardExtentCopy.resume - took: [%s] nanoseconds", System.nanoTime() - startNanoTime));
			startNanoTime = System.nanoTime();
		}
		return toResume;
	}

	@Override
	public void cancel() {
	}

	@Override
	public void addStatusMessages(List<String> messages) {
	}

}
