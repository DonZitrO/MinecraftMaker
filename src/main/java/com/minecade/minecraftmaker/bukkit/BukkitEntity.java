package com.minecade.minecraftmaker.bukkit;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.ref.WeakReference;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.NullWorld;

/**
 * An adapter to adapt a Bukkit entity into a WorldEdit one.
 */
class BukkitEntity implements Entity {

	private final WeakReference<org.bukkit.entity.Entity> entityRef;

	/**
	 * Create a new instance.
	 *
	 * @param entity
	 *            the entity
	 */
	BukkitEntity(org.bukkit.entity.Entity entity) {
		checkNotNull(entity);
		this.entityRef = new WeakReference<org.bukkit.entity.Entity>(entity);
	}

	@Override
	public Extent getExtent() {
		org.bukkit.entity.Entity entity = entityRef.get();
		if (entity != null) {
			return BukkitAdapter.adapt(entity.getWorld());
		} else {
			return NullWorld.getInstance();
		}
	}

	@Override
	public Location getLocation() {
		org.bukkit.entity.Entity entity = entityRef.get();
		if (entity != null) {
			return BukkitAdapter.adapt(entity.getLocation());
		} else {
			return new Location(NullWorld.getInstance());
		}
	}

	@Override
	public BaseEntity getState() {
		org.bukkit.entity.Entity entity = entityRef.get();
		if (entity != null) {
			if (entity instanceof Player) {
				return null;
			}

			BukkitImplAdapter adapter = MinecraftMakerPlugin.getInstance().getBukkitImplAdapter();
			if (adapter != null) {
				return adapter.getEntity(entity);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public boolean remove() {
		org.bukkit.entity.Entity entity = entityRef.get();
		if (entity != null) {
			entity.remove();
			return entity.isDead();
		} else {
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T getFacet(Class<? extends T> cls) {
		org.bukkit.entity.Entity entity = entityRef.get();
		if (entity != null && EntityType.class.isAssignableFrom(cls)) {
			return (T) new BukkitEntityType(entity);
		} else {
			return null;
		}
	}

}
