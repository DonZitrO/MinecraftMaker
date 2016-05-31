package com.minecade.minecraftmaker.schematic.extent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import com.minecade.minecraftmaker.schematic.world.BlockType;
import com.minecade.minecraftmaker.schematic.world.BlockVector;
import com.minecade.minecraftmaker.schematic.world.Direction;
import com.minecade.minecraftmaker.schematic.world.ReorderingExtent;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.World;

/**
 * Re-orders blocks into several stages.
 */
public class MultiStageReorder extends AbstractDelegateExtent implements ReorderingExtent {

	private final World world;
	private boolean enabled;

	private TupleArrayList<BlockVector, BaseBlock> stage1 = new TupleArrayList<BlockVector, BaseBlock>();
	private TupleArrayList<BlockVector, BaseBlock> stage2 = new TupleArrayList<BlockVector, BaseBlock>();
	private TupleArrayList<BlockVector, BaseBlock> stage3 = new TupleArrayList<BlockVector, BaseBlock>();

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 * @param world 
	 * @param enabled
	 *            true to enable
	 */
	public MultiStageReorder(Extent extent, World world, boolean enabled) {
		super(extent);
		checkNotNull(world);
		this.world = world;
		this.enabled = enabled;

	}

	/**
	 * Create a new instance when the re-ordering is enabled.
	 *
	 * @param extent
	 *            the extent
	 */
	public MultiStageReorder(Extent extent, World world) {
		this(extent, world, true);
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

		// clear all light emitting, gravity affected and redstone related blocks
		if (BlockType.isRedstoneSource(lazyBlock.getType()) || BlockType.isRedstoneBlock(lazyBlock.getType())) {
			super.setBlock(location, new BaseBlock(BlockID.AIR));
		} else if ((BlockType.emitsLight(lazyBlock.getType()) || BlockType.isAffectedByGravity(lazyBlock.getType())) && !(lazyBlock.getType() == block.getType()) && !(lazyBlock.getData() == block.getData())) {
			super.setBlock(location, new BaseBlock(BlockID.AIR));
		}

		if (BlockType.shouldPlaceLast(block.getType())) {
			// Place torches, etc. last
			stage2.put(location.toBlockVector(), block);
			return !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
		} else if (BlockType.shouldPlaceFinal(block.getType())) {
			// Place signs, reed, etc even later
			stage3.put(location.toBlockVector(), block);
			return !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
		} else if (BlockType.shouldPlaceLast(lazyBlock.getType()) || BlockType.shouldPlaceFinal(lazyBlock.getType())) {
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
		private final Deque<BlockVector> problematicBlocks = new LinkedList<BlockVector>();

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
				attached:
				while (true) {

					// CAREFUL!
					//walked.addFirst(current);

					assert (blockTypes.containsKey(current));

					final BaseBlock baseBlock = blockTypes.get(current);

					final int type = baseBlock.getType();
					final int data = baseBlock.getData();

					// problematic blocks need to be placed again on reverse order
					if (BlockType.isRailBlock(type)) {
						problematicBlocks.addLast(current);
					} else if (BlockType.isRedstoneSource(type) || BlockType.isRedstoneBlock(type)) {
						problematicBlocks.addLast(current);
					}

					switch (type) {
					case BlockID.WOODEN_DOOR:
					case BlockID.ACACIA_DOOR:
					case BlockID.BIRCH_DOOR:
					case BlockID.JUNGLE_DOOR:
					case BlockID.DARK_OAK_DOOR:
					case BlockID.SPRUCE_DOOR:
					case BlockID.IRON_DOOR:
						if ((data & 0x8) == 0) {
							// Deal with lower door halves being attached to the
							// floor AND the upper half
							BlockVector upperBlock = current.add(0, 1, 0).toBlockVector();
							if (blocks.contains(upperBlock) && !walked.contains(upperBlock)) {
								walked.addFirst(upperBlock);
							}
						}
						walked.addFirst(current);
						break;

					case BlockID.MINECART_TRACKS:
					case BlockID.POWERED_RAIL:
					case BlockID.DETECTOR_RAIL:
					case BlockID.ACTIVATOR_RAIL:
						walked.addFirst(current);
						// Here, rails are hardcoded to be attached to the block below them.
						// They're also attached to the block they're ascending
						// towards via BlockType.getAttachment.
						BlockVector lowerBlock = current.add(0, -1, 0).toBlockVector();
						if (problematicBlocks.contains(lowerBlock)) {
							extent.setBlock(lowerBlock, new BaseBlock(BlockID.STONE));
							blocks.remove(lowerBlock);
							break attached;
						} else if (blocks.contains(lowerBlock) && !walked.contains(lowerBlock)) {
							walked.addFirst(lowerBlock);
						}
						break;
					default:
						if (!BlockType.isRedstoneSource(type) && !BlockType.isRedstoneBlock(type)) {
							walked.addFirst(current);
						} else {
							blocks.remove(current);
							break attached;
						}
						break;
					}

					final Direction attachment = BlockType.getAttachment(type, data);
					if (attachment == null) {
						if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
							Bukkit.getLogger().info(String.format("[DEBUG] | ResumableStage3Committer.resume - block is not attached to anything => we can place it - type: [%s] - data: [%s] location: [%s]", type, data, current));
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
						// Cycle detected => This will most likely go wrong, but
						// there's nothing we can do about it.
						break;
					}
				}

				for (BlockVector pt : walked) {
					BaseBlock toPlace = blockTypes.get(pt);
					extent.setBlock(pt, toPlace);
					BaseBlock placed = extent.getLazyBlock(pt);
					if (placed.getId() != toPlace.getId() || placed.getData() != toPlace.getData()) {
						Bukkit.getLogger().warning(String.format("ResumableStage3Committer.resume - block place inconsistency - location: [%s] - expected: [%s,%s] - result: [%s,%s]", pt, toPlace.getId(), toPlace.getData(), placed.getId(), placed.getData()));
					}
					blocks.remove(pt);
				}

			}

			// allow it to resume later
			if (!blocks.isEmpty()) {
				return this;
			}


			if (!problematicBlocks.isEmpty()) {
				Bukkit.getLogger().warning(String.format("ResumableStage3Committer.resume - total problematic blocks: [%s]", problematicBlocks.size()));
				long problematicStartTime = System.nanoTime();
				Deque<BlockVector> firstPass = updateAndReverse(problematicBlocks);
				Deque<BlockVector> secondPass = updateAndReverse(firstPass);
				Deque<BlockVector> thirdPass = updateAndReverse(secondPass);
				updateAndReverse(thirdPass);
//				Deque<BlockVector> secondPass = new LinkedList<BlockVector>();
//				while (!problematicBlocks.isEmpty()/* && run.shouldContinue()*/) {
//					BlockVector pt = problematicBlocks.pop();
//					BaseBlock toPlace = blockTypes.get(pt);
//					Bukkit.getLogger().warning(String.format("ResumableStage3Committer.resume - problematic block first pass: - location: [%s] - type: [%s] - data: [%s]", pt, toPlace.getId(), toPlace.getData()));
//					world.setBlock(pt, toPlace, true);
//					if (BlockType.isRailBlock(toPlace.getType())) {
//						secondPass.addLast(pt);
//					} else {
//						secondPass.addFirst(pt);
//					}
//				}
//				while (!secondPass.isEmpty()/* && run.shouldContinue()*/) {
//					BlockVector pt = secondPass.pop();
//					BaseBlock toPlace = blockTypes.get(pt);
//					Bukkit.getLogger().warning(String.format("ResumableStage3Committer.resume - problematic block second pass: - location: [%s] - type: [%s] - data: [%s]", pt, toPlace.getId(), toPlace.getData()));
//					world.setBlock(pt, toPlace, true);
//					BaseBlock placed = extent.getLazyBlock(pt);
//					if (placed.getId() != toPlace.getId() || placed.getData() != toPlace.getData()) {
//						Bukkit.getLogger().warning(String.format("ResumableStage3Committer.resume - problematic block place inconsistency - location: [%s] - expected: [%s,%s] - result: [%s,%s]", pt, toPlace.getId(), toPlace.getData(), placed.getId(), placed.getData()));
//					}
//				}
				Bukkit.getLogger().warning(String.format("ResumableStage3Committer.resume - time consumed processing the problematic blocks: [%s] ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - problematicStartTime)));
			}
//			if (!problematicBlocks.isEmpty()) {
//				return this;
//			}

			stage1.clear();
			stage2.clear();
			stage3.clear();

			return null;
		}

		private Deque<BlockVector> updateAndReverse(Deque<BlockVector> original) throws MinecraftMakerException{
			Deque<BlockVector> reversed = new LinkedList<BlockVector>();
			while (!original.isEmpty()/* && run.shouldContinue()*/) {
				BlockVector pt = original.pop();
				BaseBlock toPlace = blockTypes.get(pt);
				world.setBlock(pt, toPlace, true);
				if (BlockType.isRailBlock(toPlace.getType())) {
					reversed.addLast(pt);
				} else {
					reversed.addFirst(pt);
				}
			}
			return reversed;
		}

		@Override
		public void cancel() {
		}

		@Override
		public void addStatusMessages(List<String> messages) {
		}

	}

}
