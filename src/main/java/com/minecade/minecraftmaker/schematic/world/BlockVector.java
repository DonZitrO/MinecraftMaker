package com.minecade.minecraftmaker.schematic.world;

/**
 * Extension of {@code Vector} that that compares with other instances
 * using integer components.
 */
public class BlockVector extends Vector {

    public static final BlockVector ZERO = new BlockVector(0, 0, 0);
    public static final BlockVector UNIT_X = new BlockVector(1, 0, 0);
    public static final BlockVector UNIT_Y = new BlockVector(0, 1, 0);
    public static final BlockVector UNIT_Z = new BlockVector(0, 0, 1);
    public static final BlockVector ONE = new BlockVector(1, 1, 1);

    /**
     * Construct an instance as a copy of another instance.
     *
     * @param position the other position
     */
    public BlockVector(Vector position) {
        super(position);
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public BlockVector(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public BlockVector(float x, float y, float z) {
        super(x, y, z);
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     */
    public BlockVector(double x, double y, double z) {
        super(x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector)) {
            return false;
        }
        Vector other = (Vector) obj;
        return (int) other.getX() == (int) this.x && (int) other.getY() == (int) this.y
                && (int) other.getZ() == (int) this.z;

    }

    @Override
    public int hashCode() {
        return ((int) x << 19) ^
                ((int) y << 12) ^
                (int) z;
    }

    @Override
    public BlockVector toBlockVector() {
        return this;
    }

}
