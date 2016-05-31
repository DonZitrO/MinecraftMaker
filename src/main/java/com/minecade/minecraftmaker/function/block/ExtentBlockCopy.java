package com.minecade.minecraftmaker.function.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.util.MCDirections;
import com.minecade.minecraftmaker.function.RegionFunction;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.extent.Extent;
import com.minecade.minecraftmaker.schematic.jnbt.CompoundTag;
import com.minecade.minecraftmaker.schematic.jnbt.CompoundTagBuilder;
import com.minecade.minecraftmaker.schematic.transform.Transform;
import com.minecade.minecraftmaker.schematic.world.Direction;
import com.minecade.minecraftmaker.schematic.world.Direction.Flag;
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
		// Apply transformations to NBT data if necessary
		block = transformNbtData(block);
		return destination.setBlock(transformed.add(to), block);
	}

	/**
	 * Transform NBT data in the given block state and return a new instance if
	 * the NBT data needs to be transformed.
	 *
	 * @param state
	 *            the existing state
	 * @return a new state or the existing one
	 */
	private BaseBlock transformNbtData(BaseBlock state) {
		CompoundTag tag = state.getNbtData();
		if (tag != null) {
			// Handle blocks which store their rotation in NBT
			if (tag.containsKey("Rot")) {
				int rot = tag.asInt("Rot");
				Direction direction = MCDirections.fromRotation(rot);
				if (direction != null) {
					Vector vector = transform.apply(direction.toVector()).subtract(transform.apply(Vector.ZERO)).normalize();
					Direction newDirection = Direction.findClosest(vector, Flag.CARDINAL | Flag.ORDINAL | Flag.SECONDARY_ORDINAL);
					if (newDirection != null) {
						CompoundTagBuilder builder = tag.createBuilder();
						builder.putByte("Rot", (byte) MCDirections.toRotation(newDirection));
						return new BaseBlock(state.getId(), state.getData(), builder.build());
					}
				}
			}
		}
		return state;
	}

}
