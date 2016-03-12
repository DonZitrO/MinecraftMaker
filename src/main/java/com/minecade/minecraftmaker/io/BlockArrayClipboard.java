package com.minecade.minecraftmaker.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.block.BaseBlock;
import com.minecade.minecraftmaker.block.BlockID;
import com.minecade.minecraftmaker.entity.BaseEntity;
import com.minecade.minecraftmaker.entity.Entity;
import com.minecade.minecraftmaker.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.util.Location;
import com.minecade.minecraftmaker.world.BaseBiome;
import com.minecade.minecraftmaker.world.Region;
import com.minecade.minecraftmaker.world.Vector;
import com.minecade.minecraftmaker.world.Vector2D;

/**
 * Stores block data as a multi-dimensional array of {@link BaseBlock}s and
 * other data as lists or maps.
 */
public class BlockArrayClipboard implements Clipboard {

    private final Region region;
    private Vector origin = new Vector();
    private final BaseBlock[][][] blocks;
    private final List<ClipboardEntity> entities = new ArrayList<ClipboardEntity>();

    /**
     * Create a new instance.
     *
     * <p>The origin will be placed at the region's lowest minimum point.</p>
     *
     * @param region the bounding region
     */
    public BlockArrayClipboard(Region region) {
        checkNotNull(region);
        this.region = region.clone();
        this.origin = region.getMinimumPoint();

        Vector dimensions = getDimensions();
        blocks = new BaseBlock[dimensions.getBlockX()][dimensions.getBlockY()][dimensions.getBlockZ()];
    }

    @Override
    public Region getRegion() {
        return region.clone();
    }

    @Override
    public Vector getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(Vector origin) {
        this.origin = origin;
    }

    @Override
    public Vector getDimensions() {
        return region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
    }

    @Override
    public Vector getMinimumPoint() {
        return region.getMinimumPoint();
    }

    @Override
    public Vector getMaximumPoint() {
        return region.getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        List<Entity> filtered = new ArrayList<Entity>();
        for (Entity entity : entities) {
            if (region.contains(entity.getLocation().toVector())) {
                filtered.add(entity);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        ClipboardEntity ret = new ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        if (region.contains(position)) {
            Vector v = position.subtract(region.getMinimumPoint());
            BaseBlock block = blocks[v.getBlockX()][v.getBlockY()][v.getBlockZ()];
            if (block != null) {
                return new BaseBlock(block);
            }
        }

        return new BaseBlock(BlockID.AIR);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return getBlock(position);
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block) throws MinecraftMakerException {
        if (region.contains(position)) {
            Vector v = position.subtract(region.getMinimumPoint());
            blocks[v.getBlockX()][v.getBlockY()][v.getBlockZ()] = new BaseBlock(block);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return new BaseBiome(0);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    /**
     * Stores entity data.
     */
    private class ClipboardEntity extends StoredEntity {
        ClipboardEntity(Location location, BaseEntity entity) {
            super(location, entity);
        }

        @Override
        public boolean remove() {
            return entities.remove(this);
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return null;
        }
    }

}
