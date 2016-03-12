package com.minecade.minecraftmaker.world;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.block.BaseBlock;

import java.util.Map;

/**
 * Describes a state property of a block.
 *
 * <p>Example states include "variant" (indicating material or type) and
 * "facing" (indicating orientation).</p>
 */
public interface State {

    /**
     * Return a map of available values for this state.
     *
     * <p>Keys are the value of state and map values describe that
     * particular state value.</p>
     *
     * @return the map of state values
     */
    Map<String, ? extends StateValue> valueMap();

    /**
     * Get the value that the block is set to.
     *
     * @param block the block
     * @return the state, otherwise null if the block isn't set to any of the values
     */
    @Nullable
    StateValue getValue(BaseBlock block);

    /**
     * Returns whether this state contains directional data.
     *
     * @return true if directional data is available
     */
    boolean hasDirection();

}
