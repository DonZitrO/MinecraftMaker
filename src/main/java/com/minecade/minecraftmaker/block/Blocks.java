package com.minecade.minecraftmaker.block;

import java.util.Collection;

/**
 * Block-related utility methods.
 */
public final class Blocks {

    private Blocks() {
    }

    /**
     * Checks whether a given block is in a list of base blocks.
     *
     * @param collection the collection
     * @param o the block
     * @return true if the collection contains the given block
     */
    public static boolean containsFuzzy(Collection<? extends BaseBlock> collection, BaseBlock o) {
        // Allow masked data in the searchBlocks to match various types
        for (BaseBlock b : collection) {
            if (b.equalsFuzzy(o)) {
                return true;
            }
        }
        return false;
    }

}
