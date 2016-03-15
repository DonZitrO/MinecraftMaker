package com.minecade.minecraftmaker.schematic.io;

import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Specifies an object that implements something suitable as a "clipboard."
 */
public interface Clipboard extends Extent {

    /**
     * Get the bounding region of this extent.
     *
     * <p>Implementations should return a copy of the region.</p>
     *
     * @return the bounding region
     */
    Region getRegion();

    /**
     * Get the dimensions of the copy, which is at minimum (1, 1, 1).
     *
     * @return the dimensions
     */
    Vector getDimensions();

    /**
     * Get the origin point from which the copy was made from.
     *
     * @return the origin
     */
    Vector getOrigin();

    /**
     * Set the origin point from which the copy was made from.
     *
     * @param origin the origin
     */
    void setOrigin(Vector origin);

}
