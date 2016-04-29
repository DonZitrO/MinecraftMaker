package com.minecade.minecraftmaker.schematic.world;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.transform.Transform;

/**
 * Transforms blocks themselves (but not their position) according to a given
 * transform.
 */
public class BlockTransformExtent extends AbstractDelegateExtent {

	//private static final double RIGHT_ANGLE = Math.toRadians(90);

	private final Transform transform;
	private final BlockRegistry blockRegistry;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 * @param blockRegistry
	 *            the block registry used for block direction data
	 */
	public BlockTransformExtent(Extent extent, Transform transform, BlockRegistry blockRegistry) {
		super(extent);
		checkNotNull(transform);
		checkNotNull(blockRegistry);
		this.transform = transform;
		this.blockRegistry = blockRegistry;
	}

	/**
	 * Get the transform.
	 *
	 * @return the transform
	 */
	public Transform getTransform() {
		return transform;
	}

	/**
	 * Transform a block without making a copy.
	 *
	 * @param block
	 *            the block
	 * @param reverse
	 *            true to transform in the opposite direction
	 * @return the same block
	 */
	private BaseBlock transformBlock(BaseBlock block, boolean reverse) {
		transform(block, reverse ? transform.inverse() : transform, blockRegistry);
		return block;
	}

	@Override
	public BaseBlock getBlock(Vector position) {
		return transformBlock(super.getBlock(position), false);
	}

	@Override
	public BaseBlock getLazyBlock(Vector position) {
		return transformBlock(super.getLazyBlock(position), false);
	}

	@Override
	public boolean setBlock(Vector location, BaseBlock block) throws MinecraftMakerException {
		return super.setBlock(location, transformBlock(new BaseBlock(block), true));
	}

	/**
	 * Transform the given block using the given transform.
	 *
	 * <p>
	 * The provided block is modified.
	 * </p>
	 *
	 * @param block
	 *            the block
	 * @param transform
	 *            the transform
	 * @param registry
	 *            the registry
	 * @return the same block
	 */
	public static BaseBlock transform(BaseBlock block, Transform transform, BlockRegistry registry) {
		return transform(block, transform, registry, block);
	}

	/**
	 * Transform the given block using the given transform.
	 *
	 * @param block
	 *            the block
	 * @param transform
	 *            the transform
	 * @param registry
	 *            the registry
	 * @param changedBlock
	 *            the block to change
	 * @return the changed block
	 */
	private static BaseBlock transform(BaseBlock block, Transform transform, BlockRegistry registry, BaseBlock changedBlock) {
		checkNotNull(block);
		checkNotNull(transform);
		checkNotNull(registry);

		Map<String, ? extends State> states = registry.getStates(block);

		if (states == null) {
			return changedBlock;
		}

		for (State state : states.values()) {
			if (state.hasDirection()) {
				StateValue value = state.getValue(block);
				if (value != null && value.getDirection() != null) {
					StateValue newValue = getNewStateValue(state, transform, value.getDirection());
					if (newValue != null) {
						newValue.set(changedBlock);
					}
				}
			}
		}

		return changedBlock;
	}

	/**
	 * Get the new value with the transformed direction.
	 *
	 * @param state
	 *            the state
	 * @param transform
	 *            the transform
	 * @param oldDirection
	 *            the old direction to transform
	 * @return a new state or null if none could be found
	 */
	@Nullable
	private static StateValue getNewStateValue(State state, Transform transform, Vector oldDirection) {
		Vector newDirection = transform.apply(oldDirection).subtract(transform.apply(Vector.ZERO)).normalize();
		StateValue newValue = null;
		double closest = -2;
		boolean found = false;

		for (StateValue v : state.valueMap().values()) {
			if (v.getDirection() != null) {
				double dot = v.getDirection().normalize().dot(newDirection);
				if (dot >= closest) {
					closest = dot;
					newValue = v;
					found = true;
				}
			}
		}

		if (found) {
			return newValue;
		} else {
			return null;
		}
	}

}
