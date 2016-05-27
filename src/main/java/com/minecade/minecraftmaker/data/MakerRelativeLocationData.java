package com.minecade.minecraftmaker.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class MakerRelativeLocationData {

	private final double x;
	private final double y;
	private final double z;
	private final float yaw;
	private final float pitch;

	private UUID locationId;

	public MakerRelativeLocationData(double x, double y, double z, float yaw, float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public MakerRelativeLocationData(Location location, UUID loactionId) {
		checkNotNull(location);
		this.x = location.getX();
		this.y = location.getY();
		this.z = location.getZ() % 16L;
		this.yaw = location.getYaw();
		this.pitch = location.getPitch();
		this.locationId = loactionId;
	}

	public UUID getLocationId() {
		return this.locationId;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public float getYaw() {
		return yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public Vector toVector() {
		return new Vector(x, y, z);
	}

	public Location toLocation(Short chunkZ, World world) {
		checkNotNull(chunkZ);
		checkNotNull(world);
		return new Vector(x, y, z + (chunkZ * 16)).toLocation(world, yaw, pitch);
	}

	public void setLocationId(UUID locationId) {;
		this.locationId = locationId;
	}

	@Override
	public String toString() {
		return "MakerRelativeLocationData [x=" + x + ", y=" + y + ", z=" + z + ", yaw=" + yaw + ", pitch=" + pitch + ", locationId=" + locationId + "]";
	}

}
