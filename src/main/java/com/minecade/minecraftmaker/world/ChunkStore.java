package com.minecade.minecraftmaker.world;

import java.io.IOException;
import java.util.Map;

import com.minecade.minecraftmaker.exception.DataException;
import com.minecade.minecraftmaker.jnbt.CompoundTag;
import com.minecade.minecraftmaker.jnbt.Tag;

/**
 * Represents chunk storage mechanisms.
 */
public abstract class ChunkStore {

    /**
     * >> to chunk
     * << from chunk
     */
    public static final int CHUNK_SHIFTS = 4;

    /**
     * Convert a position to a chunk.
     *
     * @param position the position
     * @return chunk coordinates
     */
    public static BlockVector2D toChunk(Vector position) {
        int chunkX = (int) Math.floor(position.getBlockX() / 16.0);
        int chunkZ = (int) Math.floor(position.getBlockZ() / 16.0);

        return new BlockVector2D(chunkX, chunkZ);
    }

    /**
     * Get the tag for a chunk.
     *
     * @param position the position of the chunk
     * @return tag
     * @throws DataException thrown on data error
     * @throws IOException thrown on I/O error
     */
    public abstract CompoundTag getChunkTag(Vector2D position, World world) throws DataException, IOException;

    /**
     * Get a chunk at a location.
     *
     * @param position the position of the chunk
     * @return a chunk
     * @throws ChunkStoreException thrown if there is an error from the chunk store
     * @throws DataException thrown on data error
     * @throws IOException thrown on I/O error
     */
    public Chunk getChunk(Vector2D position, World world) throws DataException, IOException {
        CompoundTag tag = getChunkTag(position, world);
        Map<String, Tag> tags = tag.getValue();
        if (tags.containsKey("Sections")) {
            return new AnvilChunk(world, tag);
        }

        return new OldChunk(world, tag);
    }

    /**
     * Close resources.
     *
     * @throws IOException on I/O error
     */
    public void close() throws IOException {
    }

    /**
     * Returns whether the chunk store is of this type.
     *
     * @return true if valid
     */
    public abstract boolean isValid();

}
