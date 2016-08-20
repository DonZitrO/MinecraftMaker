package com.minecade.minecraftmaker.data;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

public enum MakerUnlockable {

	RAINY_LEVEL(3000),
	MIDNIGHT_LEVEL(3000),
	MINESHAFT_OG_TEMPLATE(1000, UUID.fromString("2b55f573-26b8-41f2-8c55-5e15614b566f")),
	SKYLANDS_TEMPLATE(1000, UUID.fromString("62c57f5d-7319-490a-bc42-8744df21ec05")),
	HAUNTED_FOREST_TEMPLATE(1000, UUID.fromString("51fe8587-e418-4324-8cc7-9f7aebdb1586")),
	LANDSCAPE_TEMPLATE(1000, UUID.fromString("51a2ca79-4860-4f69-a46a-0e2209926a3a")),
	TIMER_TEMPLATE(1000, UUID.fromString("410b4ea4-9216-4fa0-98df-56336972f4ca")),
	REDSTONE_READY_TEMPLATE(1000, UUID.fromString("ad2440a5-aa75-4c78-b202-927caf9761dd")),
	PORTAL_RESTORE_TEMPLATE(1000, UUID.fromString("6d3f5364-250e-4864-8f29-a77eac0ed3a9")),
	ROCKY_MOUNTAINS_TEMPLATE(1000, UUID.fromString("586f4fe8-b652-4d9e-8fb0-d72c4a0670ce")),
	JUNGLE_OG_TEMPLATE(1000, UUID.fromString("1f5a9227-424d-46f6-86f6-70b22a974dc9")),
	POLLUTED_WASTELAND_TEMPLATE(1000, UUID.fromString("93e0a929-b7c6-46b8-a29d-9c6597d0d4b0"));

	private final static Map<UUID, MakerUnlockable> BY_UUID = Maps.newHashMap();

	static {
		for (MakerUnlockable unlockable : values()) {
			if (unlockable.uuid != null) {
				BY_UUID.put(unlockable.uuid, unlockable);
			}
		}
	}

	public static MakerUnlockable getUnlockableByUUID(UUID uuid) {
		return BY_UUID.get(uuid);
	}

	private final int cost;
	private final UUID uuid;

	private MakerUnlockable(int cost, UUID uuid) {
		this.cost = cost;
		this.uuid = uuid;
	}

	private MakerUnlockable(int cost) {
		this(cost, null);
	}

	public int getCost() {
		return cost;
	}

}
