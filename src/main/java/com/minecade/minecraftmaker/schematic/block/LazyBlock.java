package com.minecade.minecraftmaker.schematic.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.extent.Extent;
import com.minecade.minecraftmaker.schematic.jnbt.CompoundTag;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * A implementation of a lazy block for {@link Extent#getLazyBlock(Vector)}
 * that takes the block's ID and metadata, but will defer loading of NBT
 * data until time of access.
 *
 * <p>NBT data is later loaded using a call to {@link Extent#getBlock(Vector)}
 * with a stored {@link Extent} and location.</p>
 *
 * <p>All mutators on this object will throw an
 * {@link UnsupportedOperationException}.</p>
 */
public class LazyBlock extends BaseBlock {

    private final Extent extent;
    private final Vector position;
    private boolean loaded = false;

    /**
     * Create a new lazy block.
     *
     * @param type the block type
     * @param extent the extent to later load the full block data from
     * @param position the position to later load the full block data from
     */
    public LazyBlock(int type, Extent extent, Vector position) {
        super(type);
        checkNotNull(extent);
        checkNotNull(position);
        this.extent = extent;
        this.position = position;
    }

    /**
     * Create a new lazy block.
     *
     * @param type the block type
     * @param data the data value
     * @param extent the extent to later load the full block data from
     * @param position the position to later load the full block data from
     */
    public LazyBlock(int type, int data, Extent extent, Vector position) {
        super(type, data);
        checkNotNull(extent);
        checkNotNull(position);
        this.extent = extent;
        this.position = position;
    }

    @Override
    public void setId(int id) {
        throw new UnsupportedOperationException("This object is immutable");
    }

    @Override
    public void setData(int data) {
        throw new UnsupportedOperationException("This object is immutable");
    }

    @Override
    public CompoundTag getNbtData() {
        if (!loaded) {
            BaseBlock loadedBlock = extent.getBlock(position);
            super.setNbtData(loadedBlock.getNbtData());
        }
        return super.getNbtData();
    }

    @Override
    public void setNbtData(CompoundTag nbtData) {
        throw new UnsupportedOperationException("This object is immutable");
    }

}
