package com.minecade.minecraftmaker.function;

import com.minecade.minecraftmaker.world.Vector2D;

/**
 * Tests whether a given vector meets a criteria.
 */
public interface Mask2D {

    /**
     * Returns true if the criteria is met.
     *
     * @param vector the vector to test
     * @return true if the criteria is met
     */
    boolean test(Vector2D vector);

}
