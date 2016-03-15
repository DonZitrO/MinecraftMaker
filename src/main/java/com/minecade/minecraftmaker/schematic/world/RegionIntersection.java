package com.minecade.minecraftmaker.schematic.world;

import com.google.common.collect.Iterators;
import com.minecade.minecraftmaker.schematic.exception.RegionOperationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An intersection of several other regions. Any location that is contained in one
 * of the child regions is considered as contained by this region.
 *
 * <p>{@link #iterator()} returns a special iterator that will iterate through
 * the iterators of each region in an undefined sequence. Some positions may
 * be repeated if the position is contained in more than one region, but this cannot
 * be guaranteed to occur.</p>
 */
public class RegionIntersection extends AbstractRegion {

    private final List<Region> regions = new ArrayList<Region>();

    /**
     * Create a new instance with the included list of regions.
     *
     * @param regions a list of regions, which is copied
     */
    public RegionIntersection(List<Region> regions) {
        this(null, regions);
    }

    /**
     * Create a new instance with the included list of regions.
     *
     * @param regions a list of regions, which is copied
     */
    public RegionIntersection(Region... regions) {
        this(null, regions);
    }

    /**
     * Create a new instance with the included list of regions.
     *
     * @param world   the world
     * @param regions a list of regions, which is copied
     */
    public RegionIntersection(World world, List<Region> regions) {
        super(world);
        checkNotNull(regions);
        checkArgument(!regions.isEmpty(), "empty region list is not supported");
        for (Region region : regions) {
            this.regions.add(region);
        }
    }

    /**
     * Create a new instance with the included list of regions.
     *
     * @param world   the world
     * @param regions an array of regions, which is copied
     */
    public RegionIntersection(World world, Region... regions) {
        super(world);
        checkNotNull(regions);
        checkArgument(regions.length > 0, "empty region list is not supported");
        Collections.addAll(this.regions, regions);
    }

    @Override
    public Vector getMinimumPoint() {
        Vector minimum = regions.get(0).getMinimumPoint();
        for (int i = 1; i < regions.size(); i++) {
            minimum = Vector.getMinimum(regions.get(i).getMinimumPoint(), minimum);
        }
        return minimum;
    }

    @Override
    public Vector getMaximumPoint() {
        Vector maximum = regions.get(0).getMaximumPoint();
        for (int i = 1; i < regions.size(); i++) {
            maximum = Vector.getMaximum(regions.get(i).getMaximumPoint(), maximum);
        }
        return maximum;
    }

    @Override
    public void expand(Vector... changes) throws RegionOperationException {
        checkNotNull(changes);
        throw new RegionOperationException("Cannot expand a region intersection");
    }

    @Override
    public void contract(Vector... changes) throws RegionOperationException {
        checkNotNull(changes);
        throw new RegionOperationException("Cannot contract a region intersection");
    }

    @Override
    public boolean contains(Vector position) {
        checkNotNull(position);

        for (Region region : regions) {
            if (region.contains(position)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<BlockVector> iterator() {
        Iterator<BlockVector>[] iterators = (Iterator<BlockVector>[]) new Iterator[regions.size()];
        for (int i = 0; i < regions.size(); i++) {
            iterators[i] = regions.get(i).iterator();
        }
        return Iterators.concat(iterators);
    }

}
