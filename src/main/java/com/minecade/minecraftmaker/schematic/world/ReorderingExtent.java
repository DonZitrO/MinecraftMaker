package com.minecade.minecraftmaker.schematic.world;

/**
 * An interface for {@link Extent}s that are meant to reorder changes so that
 * they are more successful.
 *
 * <p>
 * For example, torches in Minecraft need to be placed on a block. A smart
 * reordering implementation might place the torch after the block has been
 * placed.
 * </p>
 */
public interface ReorderingExtent extends Extent {

}
