package com.minecade.minecraftmaker.block;

/**
 * Describes the material for a block.
 */
public interface BlockMaterial {

    /**
     * Get whether this block is rendered like a normal block.
     *
     * @return the value of the test
     */
    boolean isRenderedAsNormalBlock();

    /**
     * Get whether this block is a full sized cube.
     *
     * @return the value of the test
     */
    boolean isFullCube();

    /**
     * Get whether this block is opaque.
     *
     * @return the value of the test
     */
    boolean isOpaque();

    /**
     * Get whether this block emits a Redstone signal.
     *
     * @return the value of the test
     */
    boolean isPowerSource();

    /**
     * Get whether this block is a liquid.
     *
     * @return the value of the test
     */
    boolean isLiquid();

    /**
     * Get whether this block is a solid.
     *
     * @return the value of the test
     */
    boolean isSolid();

    /**
     * Get the hardness factor for this block.
     *
     * @return the hardness factor
     */
    float getHardness();

    /**
     * Get the resistance factor for this block.
     *
     * @return the resistance factor
     */
    float getResistance();

    /**
     * Get the slipperiness factor for this block.
     *
     * @return the slipperiness factor
     */
    float getSlipperiness();

    /**
     * Get whether this block blocks grass from growing.
     *
     * @return whether this block blocks grass
     */
    boolean isGrassBlocking();

    /**
     * Get the ambient occlusion light value.
     *
     * @return the ambient occlusion light value
     */
    float getAmbientOcclusionLightValue();

    /**
     * Get the opacity of this block for light to pass through.
     *
     * @return the opacity
     */
    int getLightOpacity();

    /**
     * Get the light value for this block.
     *
     * @return the light value
     */
    int getLightValue();

    /**
     * Get whether this block breaks when it is pushed by a piston.
     *
     * @return true if the block breaks
     */
    boolean isFragileWhenPushed();

    /**
     * Get whether this block can be pushed by a piston.
     *
     * @return true if the block cannot be pushed
     */
    boolean isUnpushable();

    /**
     * Get whether this block can be used in adventure mode.
     *
     * @return true if the block can be used in adventure mode
     */
    boolean isAdventureModeExempt();

    /**
     * Get whether this block is ticked randomly.
     *
     * @return true if this block is ticked randomly
     */
    boolean isTicksRandomly();

    /**
     * Gets whether this block uses a neighbor's light value.
     *
     * @return true if this block does
     */
    boolean isUsingNeighborLight();

    /**
     * Get whether this block prevents movement.
     *
     * @return true if this block blocks movement
     */
    boolean isMovementBlocker();

    /**
     * Get whether this block will burn.
     *
     * @return true if this block will burn
     */
    boolean isBurnable();

    /**
     * Get whether this block needs to be broken by a tool for maximum
     * speed.
     *
     * @return true if a tool is required
     */
    boolean isToolRequired();

    /**
     * Get whether this block is replaced when a block is placed over it
     * (for example, tall grass).
     *
     * @return true if the block is replaced
     */
    boolean isReplacedDuringPlacement();

}
