package com.minecade.minecraftmaker.world;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.block.BaseBlock;
import com.minecade.minecraftmaker.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.function.operation.Operation;

/**
 * Accepts block and entity changes.
 */
public interface OutputExtent {

    /**
     * Change the block at the given location to the given block. The operation may
     * not tie the given {@link BaseBlock} to the world, so future changes to the
     * {@link BaseBlock} do not affect the world until this method is called again.
     *
     * <p>The return value of this method indicates whether the change was probably
     * successful. It may not be successful if, for example, the location is out
     * of the bounds of the extent. It may be unsuccessful if the block passed
     * is the same as the one in the world. However, the return value is only an
     * estimation and it may be incorrect, but it could be used to count, for
     * example, the approximate number of changes.</p>
     *
     * @param position position of the block
     * @param block block to set
     * @return true if the block was successfully set (return value may not be accurate)
     * @throws MinecraftMakerException thrown on an error
     */
    boolean setBlock(Vector position, BaseBlock block) throws MinecraftMakerException;

    /**
     * Set the biome.
     *
     * @param position the (x, z) location to set the biome at
     * @param biome the biome to set to
     * @return true if the biome was successfully set (return value may not be accurate)
     */
    boolean setBiome(Vector2D position, BaseBiome biome);

    /**
     * Return an {@link Operation} that should be called to tie up loose ends
     * (such as to commit changes in a buffer).
     *
     * @return an operation or null if there is none to execute
     */
    @Nullable Operation commit();

}
