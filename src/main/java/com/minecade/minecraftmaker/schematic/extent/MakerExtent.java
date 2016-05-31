package com.minecade.minecraftmaker.schematic.extent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.bukkit.BukkitUtil;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.DataException;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.BaseBiome;
import com.minecade.minecraftmaker.schematic.world.LastAccessExtentCache;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.SurvivalModeExtent;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.Vector2D;
import com.minecade.minecraftmaker.schematic.world.World;

public class MakerExtent implements Extent {

	private final World world;
	private final Extent internalExtent;

	private @Nullable MakerPlayableLevel level;

	public MakerExtent(org.bukkit.World bukkitWorld) {
		this(bukkitWorld, false, null);
	}

	public MakerExtent(org.bukkit.World bukkitWorld, MakerPlayableLevel level) {
		this(bukkitWorld, true, level);
	}

	private MakerExtent(org.bukkit.World bukkitWorld, boolean fastMode, MakerPlayableLevel level) {
		checkNotNull(bukkitWorld);
		world = BukkitUtil.toWorld(bukkitWorld);
		this.level = level;
		Extent extent;
		extent = new FastModeExtent(world, fastMode);
		extent = new SurvivalModeExtent(extent, world);
		extent = new BlockQuirkExtent(extent, world);
		extent = new ChunkLoadingExtent(extent, world);
		extent = new LastAccessExtentCache(extent);
		extent = new DataValidatorExtent(extent, world);
		// TODO: check if this extent it's useful on mcmaker
		// extent = new BlockBagExtent(extent, blockBag);
		extent = new MultiStageReorder(extent, world, true);
		this.internalExtent = extent;
	}

	@Override
	public BaseBlock getBlock(Vector position) {
		return world.getBlock(position);
	}

	@Override
	public BaseBlock getLazyBlock(Vector position) {
		return world.getLazyBlock(position);
	}

	@Override
	public BaseBiome getBiome(Vector2D position) {
		return internalExtent.getBiome(position);
	}

	@Override
	public boolean setBlock(Vector position, BaseBlock block) throws MinecraftMakerException {
		return internalExtent.setBlock(position, block);
	}

	@Override
	public boolean setBiome(Vector2D position, BaseBiome biome) {
		return internalExtent.setBiome(position, biome);
	}

	@Override
	public @Nullable Operation commit() throws MinecraftMakerException {
		if (level != null) {
			try {
				level.tryStatusTransition(LevelStatus.CLIPBOARD_PASTING, LevelStatus.CLIPBOARD_PASTE_COMMITTING);
			} catch (DataException e) {
				level.disable(e.getMessage(), e);
				throw e;
			} finally {
				level = null;
			}
		}
		return internalExtent.commit();
	}

	@Override
	public Vector getMinimumPoint() {
		return getWorld().getMinimumPoint();
	}

	@Override
	public Vector getMaximumPoint() {
		return getWorld().getMaximumPoint();
	}

	@Override
	public List<? extends Entity> getEntities(Region region) {
		return internalExtent.getEntities(region);
	}

	@Override
	public List<? extends Entity> getEntities() {
		return internalExtent.getEntities();
	}

	@Override
	@Nullable
	public Entity createEntity(Location location, BaseEntity entity) {
		return internalExtent.createEntity(location, entity);
	}

	public World getWorld() {
		return world;
	}

}
