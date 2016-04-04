package com.minecade.minecraftmaker.schematic.bukkit;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BaseItemStack;
import com.minecade.minecraftmaker.schematic.block.LazyBlock;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.exception.WorldUnloadedException;
import com.minecade.minecraftmaker.schematic.world.AbstractWorld;
import com.minecade.minecraftmaker.schematic.world.BaseBiome;
import com.minecade.minecraftmaker.schematic.world.BlockVector2D;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.Vector2D;
import com.minecade.minecraftmaker.schematic.world.WorldData;

public class BukkitWorld extends AbstractWorld {

	private static final Map<Integer, Effect> effects = new HashMap<Integer, Effect>();
	static {
		for (Effect effect : Effect.values()) {
			effects.put(effect.getId(), effect);
		}
	}

	private final WeakReference<World> worldRef;

	/**
	 * Construct the object.
	 *
	 * @param world
	 *            the world
	 */
	@SuppressWarnings("unchecked")
	public BukkitWorld(World world) {
		this.worldRef = new WeakReference<World>(world);
	}

	@Override
	public List<com.minecade.minecraftmaker.schematic.entity.Entity> getEntities(Region region) {
		World world = getWorld();

		List<Entity> ents = world.getEntities();
		List<com.minecade.minecraftmaker.schematic.entity.Entity> entities = new ArrayList<com.minecade.minecraftmaker.schematic.entity.Entity>();
		for (Entity ent : ents) {
			if (region.contains(BukkitUtil.toVector(ent.getLocation()))) {
				entities.add(BukkitAdapter.adapt(ent));
			}
		}
		return entities;
	}

	@Override
	public List<com.minecade.minecraftmaker.schematic.entity.Entity> getEntities() {
		List<com.minecade.minecraftmaker.schematic.entity.Entity> list = new ArrayList<com.minecade.minecraftmaker.schematic.entity.Entity>();
		for (Entity entity : getWorld().getEntities()) {
			list.add(BukkitAdapter.adapt(entity));
		}
		return list;
	}

	@Nullable
	@Override
	public com.minecade.minecraftmaker.schematic.entity.Entity createEntity(com.minecade.minecraftmaker.schematic.util.Location location, BaseEntity entity) {
		BukkitImplAdapter adapter = MinecraftMakerPlugin.getInstance().getBukkitImplAdapter();
		if (adapter != null) {
			Entity createdEntity = adapter.createEntity(BukkitAdapter.adapt(getWorld(), location), entity);
			if (createdEntity != null) {
				return new BukkitEntity(createdEntity);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Get the world handle.
	 *
	 * @return the world
	 */
	public World getWorld() {
		return checkNotNull(worldRef.get(), "The world was unloaded and the reference is unavailable");
	}

	/**
	 * Get the world handle.
	 *
	 * @return the world
	 */
	protected World getWorldChecked() throws MinecraftMakerException {
		World world = worldRef.get();
		if (world == null) {
			throw new WorldUnloadedException();
		}
		return world;
	}

	@Override
	public String getName() {
		return getWorld().getName();
	}

	@Override
	public int getBlockLightLevel(Vector pt) {
		return getWorld().getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).getLightLevel();
	}

	@Override
	public boolean regenerate(Region region) {
		// FIXME: find out how to wire up this again.
		throw new UnsupportedOperationException();
		// BaseBlock[] history = new BaseBlock[16 * 16 * (getMaxY() + 1)];
		//
		// for (Vector2D chunk : region.getChunks()) {
		// Vector min = new Vector(chunk.getBlockX() * 16, 0, chunk.getBlockZ()
		// * 16);
		//
		// // First save all the blocks inside
		// for (int x = 0; x < 16; ++x) {
		// for (int y = 0; y < (getMaxY() + 1); ++y) {
		// for (int z = 0; z < 16; ++z) {
		// Vector pt = min.add(x, y, z);
		// int index = y * 16 * 16 + z * 16 + x;
		// history[index] = editSession.getBlock(pt);
		// }
		// }
		// }
		//
		// try {
		// getWorld().regenerateChunk(chunk.getBlockX(), chunk.getBlockZ());
		// } catch (Throwable t) {
		// logger.log(Level.WARNING,
		// "Chunk generation via Bukkit raised an error", t);
		// }
		//
		// // Then restore
		// for (int x = 0; x < 16; ++x) {
		// for (int y = 0; y < (getMaxY() + 1); ++y) {
		// for (int z = 0; z < 16; ++z) {
		// Vector pt = min.add(x, y, z);
		// int index = y * 16 * 16 + z * 16 + x;
		//
		// // We have to restore the block if it was outside
		// if (!region.contains(pt)) {
		// editSession.smartSetBlock(pt, history[index]);
		// } else { // Otherwise fool with history
		// editSession.rememberChange(pt, history[index],
		// editSession.rawGetBlock(pt));
		// }
		// }
		// }
		// }
		// }
		//
		// return true;
	}

	/**
	 * Gets the single block inventory for a potentially double chest. Handles
	 * people who have an old version of Bukkit. This should be replaced with
	 * {@link org.bukkit.block.Chest#getBlockInventory()} in a few months (now =
	 * March 2012) // note from future dev - lol
	 *
	 * @param chest
	 *            The chest to get a single block inventory for
	 * @return The chest's inventory
	 */
	private Inventory getBlockInventory(Chest chest) {
		try {
			return chest.getBlockInventory();
		} catch (Throwable t) {
			if (chest.getInventory() instanceof DoubleChestInventory) {
				DoubleChestInventory inven = (DoubleChestInventory) chest.getInventory();
				if (inven.getLeftSide().getHolder().equals(chest)) {
					return inven.getLeftSide();
				} else if (inven.getRightSide().getHolder().equals(chest)) {
					return inven.getRightSide();
				} else {
					return inven;
				}
			} else {
				return chest.getInventory();
			}
		}
	}

	@Override
	public boolean clearContainerBlockContents(Vector pt) {
		Block block = getWorld().getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
		if (block == null) {
			return false;
		}
		BlockState state = block.getState();
		if (!(state instanceof org.bukkit.inventory.InventoryHolder)) {
			return false;
		}

		org.bukkit.inventory.InventoryHolder chest = (org.bukkit.inventory.InventoryHolder) state;
		Inventory inven = chest.getInventory();
		if (chest instanceof Chest) {
			inven = getBlockInventory((Chest) chest);
		}
		inven.clear();
		return true;
	}

	@Override
	public void dropItem(Vector pt, BaseItemStack item) {
		World world = getWorld();
		ItemStack bukkitItem = new ItemStack(item.getType(), item.getAmount(), item.getData());
		world.dropItemNaturally(BukkitUtil.toLocation(world, pt), bukkitItem);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isValidBlockType(int type) {
		return Material.getMaterial(type) != null && Material.getMaterial(type).isBlock();
	}

	@Override
	public void checkLoadedChunk(Vector pt) {
		World world = getWorld();

		if (!world.isChunkLoaded(pt.getBlockX() >> 4, pt.getBlockZ() >> 4)) {
			world.loadChunk(pt.getBlockX() >> 4, pt.getBlockZ() >> 4);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		} else if ((other instanceof BukkitWorld)) {
			return ((BukkitWorld) other).getWorld().equals(getWorld());
		} else if (other instanceof com.minecade.minecraftmaker.schematic.world.World) {
			return ((com.minecade.minecraftmaker.schematic.world.World) other).getName().equals(getName());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getWorld().hashCode();
	}

	@Override
	public int getMaxY() {
		return getWorld().getMaxHeight() - 1;
	}

	@Override
	public void fixAfterFastMode(Iterable<BlockVector2D> chunks) {
		World world = getWorld();
		for (BlockVector2D chunkPos : chunks) {
			world.refreshChunk(chunkPos.getBlockX(), chunkPos.getBlockZ());
		}
	}

	@Override
	public boolean playEffect(Vector position, int type, int data) {
		World world = getWorld();

		final Effect effect = effects.get(type);
		if (effect == null) {
			return false;
		}

		world.playEffect(BukkitUtil.toLocation(world, position), effect, data);

		return true;
	}

	@Override
	public WorldData getWorldData() {
		return BukkitWorldData.getInstance();
	}

	@Override
	public void simulateBlockMine(Vector pt) {
		getWorld().getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).breakNaturally();
	}

	@Override
	public BaseBlock getBlock(Vector position) {
		BukkitImplAdapter adapter = MinecraftMakerPlugin.getInstance().getBukkitImplAdapter();
		if (adapter != null) {
			return adapter.getBlock(BukkitAdapter.adapt(getWorld(), position));
		} else {
			Block bukkitBlock = getWorld().getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());
			return new BaseBlock(bukkitBlock.getTypeId(), bukkitBlock.getData());
		}
	}

	@Override
	public boolean setBlock(Vector position, BaseBlock block, boolean notifyAndLight) throws MinecraftMakerException {
		BukkitImplAdapter adapter = MinecraftMakerPlugin.getInstance().getBukkitImplAdapter();
		if (adapter != null) {
			return adapter.setBlock(BukkitAdapter.adapt(getWorld(), position), block, notifyAndLight);
		} else {
			Block bukkitBlock = getWorld().getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());
			return bukkitBlock.setTypeIdAndData(block.getType(), (byte) block.getData(), notifyAndLight);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public BaseBlock getLazyBlock(Vector position) {
		World world = getWorld();
		Block bukkitBlock = world.getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());
		return new LazyBlock(bukkitBlock.getTypeId(), bukkitBlock.getData(), this, position);
	}

	@Override
	public BaseBiome getBiome(Vector2D position) {
		BukkitImplAdapter adapter = MinecraftMakerPlugin.getInstance().getBukkitImplAdapter();
		if (adapter != null) {
			int id = adapter.getBiomeId(getWorld().getBiome(position.getBlockX(), position.getBlockZ()));
			return new BaseBiome(id);
		} else {
			return new BaseBiome(0);
		}
	}

	@Override
	public boolean setBiome(Vector2D position, BaseBiome biome) {
		BukkitImplAdapter adapter = MinecraftMakerPlugin.getInstance().getBukkitImplAdapter();
		if (adapter != null) {
			Biome bukkitBiome = adapter.getBiome(biome.getId());
			getWorld().setBiome(position.getBlockX(), position.getBlockZ(), bukkitBiome);
			return true;
		} else {
			return false;
		}
	}

}
