package com.minecade.minecraftmaker.schematic.world;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BaseItemStack;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.util.Location;

/**
 * A null implementation of {@link World} that drops all changes and returns
 * dummy data.
 */
public class NullWorld extends AbstractWorld {

	private static final NullWorld INSTANCE = new NullWorld();

	protected NullWorld() {
	}

	@Override
	public String getName() {
		return "null";
	}

	@Override
	public boolean setBlock(Vector position, BaseBlock block, boolean notifyAndLight) throws MinecraftMakerException {
		return false;
	}

	@Override
	public int getBlockLightLevel(Vector position) {
		return 0;
	}

	@Override
	public boolean clearContainerBlockContents(Vector position) {
		return false;
	}

	@Override
	public BaseBiome getBiome(Vector2D position) {
		return null;
	}

	@Override
	public boolean setBiome(Vector2D position, BaseBiome biome) {
		return false;
	}

	@Override
	public void dropItem(Vector position, BaseItemStack item) {
	}

	@Override
	public boolean regenerate(Region region) {
		return false;
	}

	@Override
	public WorldData getWorldData() {
		return LegacyWorldData.getInstance();
	}

	@Override
	public BaseBlock getBlock(Vector position) {
		return new BaseBlock(BlockID.AIR);
	}

	@Override
	public BaseBlock getLazyBlock(Vector position) {
		return new BaseBlock(BlockID.AIR);
	}

	@Override
	public List<Entity> getEntities(Region region) {
		return Collections.emptyList();
	}

	@Override
	public List<Entity> getEntities() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public Entity createEntity(Location location, BaseEntity entity) {
		return null;
	}

	/**
	 * Return an instance of this null world.
	 *
	 * @return a null world
	 */
	public static NullWorld getInstance() {
		return INSTANCE;
	}

}
