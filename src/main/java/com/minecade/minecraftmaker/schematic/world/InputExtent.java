package com.minecade.minecraftmaker.schematic.world;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;

/**
 * Provides the current state of blocks, entities, and so on.
 */
public interface InputExtent {

    /**
     * Get a snapshot of the block at the given location.
     *
     * <p>If the given position is out of the bounds of the extent, then the behavior
     * is undefined (an air block could be returned). However, {@code null}
     * should <strong>not</strong> be returned.</p>
     *
     * <p>The returned block is mutable and is a snapshot of the block at the time
     * of call. It has no position attached to it, so it could be reused in
     * {@link Pattern}s and so on.</p>
     *
     * <p>Calls to this method can actually be quite expensive, so cache results
     * whenever it is possible, while being aware of the mutability aspect.
     * The cost, however, depends on the implementation and particular extent.
     * If only basic information about the block is required, then use of
     * {@link #getLazyBlock(Vector)} is recommended.</p>
     *
     * @param position position of the block
     * @return the block
     */
    BaseBlock getBlock(Vector position);

    /**
     * Get a lazy, immutable snapshot of the block at the given location that only
     * immediately contains information about the block's type (and metadata).
     *
     * <p>Further information (such as NBT data) will be available <strong>by the
     * time of access</strong>. Therefore, it is not recommended that
     * this method is used if the world is being simulated at the time of
     * call. If the block needs to be stored for future use, then this method should
     * definitely not be used. Moreover, the block that is returned is immutable (or
     * should be), and therefore modifications should not be attempted on it. If a
     * modifiable copy is required, then the block should be cloned.</p>
     *
     * <p>This method exists because it is sometimes important to inspect the block
     * at a given location, but {@link #getBlock(Vector)} may be too expensive in
     * the underlying implementation. It is also not possible to implement
     * caching if the returned object is mutable, so this methods allows caching
     * implementations to be used.</p>
     *
     * @param position position of the block
     * @return the block
     */
    BaseBlock getLazyBlock(Vector position);

    /**
     * Get the biome at the given location.
     *
     * <p>If there is no biome available, then the ocean biome should be
     * returned.</p>
     *
     * @param position the (x, z) location to check the biome at
     * @return the biome at the location
     */
    BaseBiome getBiome(Vector2D position);

}
