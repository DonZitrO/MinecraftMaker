package com.minecade.minecraftmaker.schematic.world;

import java.util.*;

import com.minecade.minecraftmaker.schematic.exception.RegionOperationException;

public abstract class AbstractRegion implements Region {

    protected World world;

    public AbstractRegion(World world) {
        this.world = world;
    }

    @Override
    public Vector getCenter() {
        return getMinimumPoint().add(getMaximumPoint()).divide(2);
    }

    /**
     * Get the iterator.
     *
     * @return iterator of points inside the region
     */
    @Override
    public Iterator<BlockVector> iterator() {
        return new RegionIterator(this);
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public void shift(Vector change) throws RegionOperationException {
        expand(change);
        contract(change);
    }

    @Override
    public AbstractRegion clone() {
        try {
            return (AbstractRegion) super.clone();
        } catch (CloneNotSupportedException exc) {
            return null;
        }
    }

    @Override
    public List<BlockVector2D> polygonize(int maxPoints) {
        if (maxPoints >= 0 && maxPoints < 4) {
            throw new IllegalArgumentException("Cannot polygonize an AbstractRegion with no overridden polygonize method into less than 4 points.");
        }

        final BlockVector min = getMinimumPoint().toBlockVector();
        final BlockVector max = getMaximumPoint().toBlockVector();

        final List<BlockVector2D> points = new ArrayList<BlockVector2D>(4);

        points.add(new BlockVector2D(min.getX(), min.getZ()));
        points.add(new BlockVector2D(min.getX(), max.getZ()));
        points.add(new BlockVector2D(max.getX(), max.getZ()));
        points.add(new BlockVector2D(max.getX(), min.getZ()));

        return points;
    }

    /**
     * Get the number of blocks in the region.
     *
     * @return number of blocks
     */
    @Override
    public int getArea() {
        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();

        return (int)((max.getX() - min.getX() + 1) *
                     (max.getY() - min.getY() + 1) *
                     (max.getZ() - min.getZ() + 1));
    }

    /**
     * Get X-size.
     *
     * @return width
     */
    @Override
    public int getWidth() {
        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();

        return (int) (max.getX() - min.getX() + 1);
    }

    /**
     * Get Y-size.
     *
     * @return height
     */
    @Override
    public int getHeight() {
        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();

        return (int) (max.getY() - min.getY() + 1);
    }

    /**
     * Get Z-size.
     *
     * @return length
     */
    @Override
    public int getLength() {
        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();

        return (int) (max.getZ() - min.getZ() + 1);
    }

    /**
     * Get a list of chunks.
     *
     * @return a set of chunks
     */
    @Override
    public Set<Vector2D> getChunks() {
        final Set<Vector2D> chunks = new HashSet<Vector2D>();

        final Vector min = getMinimumPoint();
        final Vector max = getMaximumPoint();

        final int minY = min.getBlockY();

        for (int x = min.getBlockX(); x <= max.getBlockX(); ++x) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); ++z) {
                if (!contains(new Vector(x, minY, z))) {
                    continue;
                }

                chunks.add(new BlockVector2D(
                    x >> ChunkStore.CHUNK_SHIFTS,
                    z >> ChunkStore.CHUNK_SHIFTS
                ));
            }
        }

        return chunks;
    }

    @Override
    public Set<Vector> getChunkCubes() {
        final Set<Vector> chunks = new HashSet<Vector>();

        final Vector min = getMinimumPoint();
        final Vector max = getMaximumPoint();

        for (int x = min.getBlockX(); x <= max.getBlockX(); ++x) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); ++y) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); ++z) {
                    if (!contains(new Vector(x, y, z))) {
                        continue;
                    }

                    chunks.add(new BlockVector(
                        x >> ChunkStore.CHUNK_SHIFTS,
                        y >> ChunkStore.CHUNK_SHIFTS,
                        z >> ChunkStore.CHUNK_SHIFTS
                    ));
                }
            }
        }

        return chunks;
    }

}
