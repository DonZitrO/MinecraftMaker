package com.minecade.minecraftmaker.function.entity;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.function.EntityFunction;
import com.minecade.minecraftmaker.schematic.bukkit.EntityType;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.jnbt.CompoundTag;
import com.minecade.minecraftmaker.schematic.jnbt.CompoundTagBuilder;
import com.minecade.minecraftmaker.schematic.transform.Transform;
import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.Direction;
import com.minecade.minecraftmaker.schematic.world.Direction.Flag;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.util.MCDirections;


/**
 * Copies entities provided to the function to the provided destination
 * {@code Extent}.
 */
public class ExtentEntityCopy implements EntityFunction {

	private final Extent destination;
	private final Vector from;
	private final Vector to;
	private final Transform transform;
	private boolean removing;

	/**
	 * Create a new instance.
	 *
	 * @param from
	 *            the from position
	 * @param destination
	 *            the destination {@code Extent}
	 * @param to
	 *            the destination position
	 * @param transform
	 *            the transformation to apply to both position and orientation
	 */
	public ExtentEntityCopy(Vector from, Extent destination, Vector to, Transform transform) {
		checkNotNull(from);
		checkNotNull(destination);
		checkNotNull(to);
		checkNotNull(transform);
		this.destination = destination;
		this.from = from;
		this.to = to;
		this.transform = transform;
	}

	/**
	 * Return whether entities that are copied should be removed.
	 *
	 * @return true if removing
	 */
	public boolean isRemoving() {
		return removing;
	}

	/**
	 * Set whether entities that are copied should be removed.
	 *
	 * @param removing
	 *            true if removing
	 */
	public void setRemoving(boolean removing) {
		this.removing = removing;
	}

	@Override
	public boolean apply(Entity entity) throws MinecraftMakerException {
		EntityType registryType = entity.getFacet(EntityType.class);
		Bukkit.getLogger().severe(String.format("Item: [%s]", registryType != null ? registryType.isItem() : false));
		BaseEntity state = entity.getState();
		if (state != null) {
			Location newLocation;
			Location location = entity.getLocation();

			Vector pivot = from.round().add(0.5, 0.5, 0.5);
			Vector newPosition = transform.apply(location.toVector().subtract(pivot));
			Vector newDirection;

			newDirection = transform.isIdentity() ? entity.getLocation().getDirection() : transform.apply(location.getDirection()).subtract(transform.apply(Vector.ZERO))
			        .normalize();
			newLocation = new Location(destination, newPosition.add(to.round().add(0.5, 0.5, 0.5)), newDirection);

			// Some entities store their position data in NBT
			state = transformNbtData(state);

			boolean success = destination.createEntity(newLocation, state) != null;

			// Remove
			if (isRemoving() && success) {
				entity.remove();
			}

			return success;
		} else {
			return false;
		}
	}

	/**
	 * Transform NBT data in the given entity state and return a new instance if
	 * the NBT data needs to be transformed.
	 *
	 * @param state
	 *            the existing state
	 * @return a new state or the existing one
	 */
	private BaseEntity transformNbtData(BaseEntity state) {
		CompoundTag tag = state.getNbtData();

		if (tag != null) {
			// Handle hanging entities (paintings, item frames, etc.)
			boolean hasTilePosition = tag.containsKey("TileX") && tag.containsKey("TileY") && tag.containsKey("TileZ");
			boolean hasDirection = tag.containsKey("Direction");
			boolean hasLegacyDirection = tag.containsKey("Dir");

			if (hasTilePosition) {
				Vector tilePosition = new Vector(tag.asInt("TileX"), tag.asInt("TileY"), tag.asInt("TileZ"));
				Vector newTilePosition = transform.apply(tilePosition.subtract(from)).add(to);

				CompoundTagBuilder builder = tag.createBuilder().putInt("TileX", newTilePosition.getBlockX()).putInt("TileY", newTilePosition.getBlockY())
				        .putInt("TileZ", newTilePosition.getBlockZ());

				if (hasDirection || hasLegacyDirection) {
					int d = hasDirection ? tag.asInt("Direction") : MCDirections.fromLegacyHanging((byte) tag.asInt("Dir"));
					Direction direction = MCDirections.fromHanging(d);

					if (direction != null) {
						Vector vector = transform.apply(direction.toVector()).subtract(transform.apply(Vector.ZERO)).normalize();
						Direction newDirection = Direction.findClosest(vector, Flag.CARDINAL);

						builder.putByte("Direction", (byte) MCDirections.toHanging(newDirection));
						builder.putByte("Dir", MCDirections.toLegacyHanging(MCDirections.toHanging(newDirection)));
					}
				}

				return new BaseEntity(state.getTypeId(), builder.build());
			}
		}

		return state;
	}

}
