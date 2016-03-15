package com.minecade.minecraftmaker.schematic.world;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.block.BaseItem;

public interface ItemRegistry {

    /**
     * Create a new item using its ID.
     *
     * @param id the id
     * @return the item, which may be null if no item exists
     */
    @Nullable
    BaseItem createFromId(String id);

    /**
     * Create a new item using its legacy numeric ID.
     *
     * @param id the id
     * @return the item, which may be null if no item exists
     */
    @Nullable
    BaseItem createFromId(int id);

}
