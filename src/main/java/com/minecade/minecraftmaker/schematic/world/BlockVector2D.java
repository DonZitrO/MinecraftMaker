package com.minecade.minecraftmaker.schematic.world;

/**
 * Extension of {@code Vector2D} that that compares with other instances
 * using integer components.
 */
public class BlockVector2D extends Vector2D {

    public static final BlockVector2D ZERO = new BlockVector2D(0, 0);
    public static final BlockVector2D UNIT_X = new BlockVector2D(1, 0);
    public static final BlockVector2D UNIT_Z = new BlockVector2D(0, 1);
    public static final BlockVector2D ONE = new BlockVector2D(1, 1);

    /**
     * Construct an instance from another instance.
     *
     * @param position the position to copy
     */
    public BlockVector2D(Vector2D position) {
        super(position);
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     */
    public BlockVector2D(int x, int z) {
        super(x, z);
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     */
    public BlockVector2D(float x, float z) {
        super(x, z);
    }

    /**
     * Construct a new instance.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     */
    public BlockVector2D(double x, double z) {
        super(x, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector2D)) {
            return false;
        }

        Vector2D other = (Vector2D) obj;
        return (int) other.x == (int) this.x && (int) other.z == (int) this.z;

    }

    @Override
    public int hashCode() {
        return (Integer.valueOf((int) x).hashCode() >> 13) ^
                Integer.valueOf((int) z).hashCode();
    }

    @Override
    public BlockVector2D toBlockVector2D() {
        return this;
    }

}
