package com.minecade.minecraftmaker.schematic.api;

import java.util.UUID;

public interface SchematicAPI {

	public void saveSchematic(UUID levelId, String levelName, int chunkCoordinate);

	public void loadSchematic(UUID levelId, String levelName, int chunkCoordinate);

}
