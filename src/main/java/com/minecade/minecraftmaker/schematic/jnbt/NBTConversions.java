package com.minecade.minecraftmaker.schematic.jnbt;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.Extent;

/**
 * Utility methods for working with NBT data used in Minecraft.
 */
public final class NBTConversions {

    private NBTConversions() {
    }

    /**
     * Read a {@code Location} from two list tags, the first of which contains
     * three numbers for the X, Y, and Z components, and the second of
     * which contains two numbers, the yaw and pitch in degrees.
     *
     * <p>For values that are unavailable, their values will be 0.</p>
     *
     * @param extent the extent
     * @param positionTag the position tag
     * @param directionTag the direction tag
     * @return a location
     */
    public static Location toLocation(Extent extent, ListTag positionTag, ListTag directionTag) {
        checkNotNull(extent);
        checkNotNull(positionTag);
        checkNotNull(directionTag);
        return new Location(
                extent,
                positionTag.asDouble(0), positionTag.asDouble(1), positionTag.asDouble(2),
                (float) directionTag.asDouble(0), (float) directionTag.asDouble(1));
    }

}
