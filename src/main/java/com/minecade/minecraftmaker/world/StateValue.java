package com.minecade.minecraftmaker.world;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.block.BaseBlock;

/**
 * Describes a possible value for a {@code State}.
 */
public interface StateValue {

    /**
     * Return whether this state is set on the given block.
     *
     * @param block the block
     * @return true if this value is set
     */
    boolean isSet(BaseBlock block);

    /**
     * Set the state to this value on the given block.
     *
     * @param block the block to change
     * @return true if the value was set successfully
     */
    boolean set(BaseBlock block);

    /**
     * Return the direction associated with this value.
     *
     * @return the direction, otherwise null
     */
    @Nullable
    Vector getDirection();

}
