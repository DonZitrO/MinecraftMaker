package com.minecade.minecraftmaker.world;

import com.minecade.minecraftmaker.block.BaseBlock;
import com.minecade.minecraftmaker.exception.DataException;

/**
 * A 16 by 16 block chunk.
 */
public interface Chunk {

    /**
     * Get the block ID of a block.
     *
     * @param position the position of the block
     * @return the type ID of the block
     * @throws DataException thrown on data error
     */
    public int getBlockID(Vector position) throws DataException;
    
    /**
     * Get the block data of a block.
     *
     * @param position the position of the block
     * @return the data value of the block
     * @throws DataException thrown on data error
     */
    public int getBlockData(Vector position) throws DataException;
    
    
    /**
     * Get a block;
     *
     * @param position the position of the block
     * @return block the block
     * @throws DataException thrown on data error
     */
    public BaseBlock getBlock(Vector position) throws DataException;

}
