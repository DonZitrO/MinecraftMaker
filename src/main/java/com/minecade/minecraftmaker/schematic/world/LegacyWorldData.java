package com.minecade.minecraftmaker.schematic.world;

/**
 * An implementation of {@link WorldData} that uses legacy numeric IDs and a
 * built-in block database.
 */
public class LegacyWorldData implements WorldData {

	private static final LegacyWorldData INSTANCE = new LegacyWorldData();
	private final LegacyBlockRegistry blockRegistry = new LegacyBlockRegistry();
	private final NullItemRegistry itemRegistry = new NullItemRegistry();
	private final NullEntityRegistry entityRegistry = new NullEntityRegistry();
	private final NullBiomeRegistry biomeRegistry = new NullBiomeRegistry();

	/**
	 * Create a new instance.
	 */
	protected LegacyWorldData() {
	}

	@Override
	public BlockRegistry getBlockRegistry() {
		return blockRegistry;
	}

	@Override
	public ItemRegistry getItemRegistry() {
		return itemRegistry;
	}

	@Override
	public EntityRegistry getEntityRegistry() {
		return entityRegistry;
	}

	@Override
	public BiomeRegistry getBiomeRegistry() {
		return biomeRegistry;
	}

	/**
	 * Get a singleton instance.
	 *
	 * @return an instance
	 */
	public static LegacyWorldData getInstance() {
		return INSTANCE;
	}

}
