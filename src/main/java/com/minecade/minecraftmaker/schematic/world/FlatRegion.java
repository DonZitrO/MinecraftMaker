package com.minecade.minecraftmaker.schematic.world;

public interface FlatRegion extends Region {

    /**
     * Gets the minimum Y value
     *
     * @return the Y value
     */
    public int getMinimumY();

    /**
     * Gets the maximum Y value
     *
     * @return the Y value
     */
    public int getMaximumY();

    /**
     * Get this region as an iterable flat region.
     *
     * @return a flat region iterable
     */
    public Iterable<Vector2D> asFlatRegion();
}
