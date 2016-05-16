package com.minecade.minecraftmaker.schematic.world;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;

import com.google.common.collect.Iterators;
import com.minecade.core.util.TupleArrayList;
import com.minecade.minecraftmaker.function.operation.LimitedTimeRunContext;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.function.operation.ResumableBlockMapEntryPlacer;
import com.minecade.minecraftmaker.function.operation.ResumableOperationQueue;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

/**
 * Re-orders blocks into several stages.
 */
public class MultiStageReorder extends AbstractDelegateExtent implements ReorderingExtent {

	private TupleArrayList<BlockVector, BaseBlock> stage1 = new TupleArrayList<BlockVector, BaseBlock>();
	private TupleArrayList<BlockVector, BaseBlock> stage2 = new TupleArrayList<BlockVector, BaseBlock>();
	private TupleArrayList<BlockVector, BaseBlock> stage3 = new TupleArrayList<BlockVector, BaseBlock>();
	private boolean enabled;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 * @param enabled
	 *            true to enable
	 */
	public MultiStageReorder(Extent extent, boolean enabled) {
		super(extent);
		this.enabled = enabled;
	}

	/**
	 * Create a new instance when the re-ordering is enabled.
	 *
	 * @param extent
	 *            the extent
	 */
	public MultiStageReorder(Extent extent) {
		this(extent, true);
	}

	/**
	 * Return whether re-ordering is enabled.
	 *
	 * @return true if re-ordering is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set whether re-ordering is enabled.
	 *
	 * @param enabled
	 *            true if re-ordering is enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean setBlock(Vector location, BaseBlock block) throws MinecraftMakerException {
		BaseBlock lazyBlock = getLazyBlock(location);

		if (!enabled) {
			return super.setBlock(location, block);
		}

		if (BlockType.shouldPlaceLast(block.getType())) {
			// Place torches, etc. last
			stage2.put(location.toBlockVector(), block);
			return !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
		} else if (BlockType.shouldPlaceFinal(block.getType())) {
			// Place signs, reed, etc even later
			stage3.put(location.toBlockVector(), block);
			return !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
		} else if (BlockType.shouldPlaceLast(lazyBlock.getType())) {
			// Destroy torches, etc. first
			super.setBlock(location, new BaseBlock(BlockID.AIR));
			return super.setBlock(location, block);
		} else {
			stage1.put(location.toBlockVector(), block);
			return !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
		}
	}

	@Override
	public Operation commitBefore() {
		return new ResumableOperationQueue(new ResumableBlockMapEntryPlacer(getExtent(), Iterators.concat(stage1.iterator(), stage2.iterator())), new ResumableStage3Committer());
	}

	private class ResumableStage3Committer implements Operation {

		private final Set<BlockVector> blocks = new HashSet<BlockVector>();
		private final Map<BlockVector, BaseBlock> blockTypes = new HashMap<BlockVector, BaseBlock>();

		public ResumableStage3Committer() {
			for (Map.Entry<BlockVector, BaseBlock> entry : stage3) {
				final BlockVector pt = entry.getKey();
				blocks.add(pt);
				blockTypes.put(pt, entry.getValue());
			}
		}

		@Override
		public Operation resume(LimitedTimeRunContext run) throws MinecraftMakerException {
			Extent extent = getExtent();

			while (!blocks.isEmpty() && run.shouldContinue()) {
				BlockVector current = blocks.iterator().next();
				if (!blocks.contains(current)) {
					continue;
				}

				final Deque<BlockVector> walked = new LinkedList<BlockVector>();

				while (true) {
					walked.addFirst(current);

					assert (blockTypes.containsKey(current));

					final BaseBlock baseBlock = blockTypes.get(current);

					final int type = baseBlock.getType();
					final int data = baseBlock.getData();

					switch (type) {
					case BlockID.WOODEN_DOOR:
					case BlockID.IRON_DOOR:
						if ((data & 0x8) == 0) {
							// Deal with lower door halves being attached to the
							// floor AND the upper half
							BlockVector upperBlock = current.add(0, 1, 0).toBlockVector();
							if (blocks.contains(upperBlock) && !walked.contains(upperBlock)) {
								walked.addFirst(upperBlock);
							}
						}
						break;

					case BlockID.MINECART_TRACKS:
					case BlockID.POWERED_RAIL:
					case BlockID.DETECTOR_RAIL:
					case BlockID.ACTIVATOR_RAIL:
						// FIXME: experimental - set rail blocks twice (second time force update of direction and attachment)
						extent.setBlock(current, blockTypes.get(current));
						// Here, rails are hardcoded to be attached to the block
						// below them.
						// They're also attached to the block they're ascending
						// towards via BlockType.getAttachment.
						if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
							Bukkit.getLogger().info(String.format("[DEBUG] | ResumableStage3Committer.resume - processing rail block - type: [%s] - location: [%s]", type, current));
						}
						BlockVector lowerBlock = current.add(0, -1, 0).toBlockVector();
						Bukkit.getLogger().info(String.format("[DEBUG] | ResumableStage3Committer.resume - processing block below rail block - location: [%s] - blocks.contains: [%s] - walked.contains: [%s]", lowerBlock, blocks.contains(lowerBlock), walked.contains(lowerBlock)));
						if (blocks.contains(lowerBlock) && !walked.contains(lowerBlock)) {
							walked.addFirst(lowerBlock);
						}
						break;
					}

					final Direction attachment = BlockType.getAttachment(type, data);
					if (attachment == null) {
						if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
							Bukkit.getLogger().info(String.format("[DEBUG] | ResumableStage3Committer.resume - block is not attached to anything => we can place it - type: [%s] - location: [%s]", type, current));
						}
						// Block is not attached to anything => we can place it
						break;
					}

					current = current.add(attachment.toVector()).toBlockVector();
					if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
						Bukkit.getLogger().info(String.format("[DEBUG] | ResumableStage3Committer.resume - attachment detected - location: [%s] - blocks.contains: [%s] - walked.contains: [%s]", current, blocks.contains(current), walked.contains(current)));
					}

					if (!blocks.contains(current)) {
						// We ran outside the remaining set => assume we can
						// place blocks on this
						break;
					}

					if (walked.contains(current)) {
						if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
							Bukkit.getLogger().info(String.format(""));
						}
						// Cycle detected => This will most likely go wrong, but
						// there's nothing we can do about it.
						break;
					}
				}

				for (BlockVector pt : walked) {
					extent.setBlock(pt, blockTypes.get(pt));
					blocks.remove(pt);
				}
			}

			// allow it to resume later
			if (!blocks.isEmpty()) {
				return this;
			}

			stage1.clear();
			stage2.clear();
			stage3.clear();

			return null;
		}

		@Override
		public void cancel() {
		}

		@Override
		public void addStatusMessages(List<String> messages) {
		}

	}

}
