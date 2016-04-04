package com.minecade.minecraftmaker.function.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.function.RegionFunction;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.transform.Transform;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Copies blocks from one extent to another.
 */
public class ExtentBlockCopy implements RegionFunction {

	private final Extent source;
	private final Extent destination;
	private final Vector from;
	private final Vector to;
	private final Transform transform;

	/**
	 * Make a new copy.
	 *
	 * @param source
	 *            the source extent
	 * @param from
	 *            the source offset
	 * @param destination
	 *            the destination extent
	 * @param to
	 *            the destination offset
	 * @param transform
	 *            a transform to apply to positions (after source offset, before
	 *            destination offset)
	 */
	public ExtentBlockCopy(Extent source, Vector from, Extent destination, Vector to, Transform transform) {
		checkNotNull(source);
		checkNotNull(from);
		checkNotNull(destination);
		checkNotNull(to);
		checkNotNull(transform);
		this.source = source;
		this.from = from;
		this.destination = destination;
		this.to = to;
		this.transform = transform;
	}

	@Override
	public boolean apply(Vector position) throws MinecraftMakerException {
		BaseBlock block = source.getBlock(position);
		Vector orig = position.subtract(from);
		Vector transformed = transform.apply(orig);
		return destination.setBlock(transformed.add(to), block);
	}

}
