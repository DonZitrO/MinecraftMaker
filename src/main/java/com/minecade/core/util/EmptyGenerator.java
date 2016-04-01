package com.minecade.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

public class EmptyGenerator extends ChunkGenerator {

	private Map<Integer, byte[]> emptyChunks = new HashMap<Integer, byte[]>();

	@Override
	public byte[] generate(World world, Random random, int x, int z) {
		if (!emptyChunks.containsKey(world.getMaxHeight())) {
			emptyChunks.put(world.getMaxHeight(), new byte[world.getMaxHeight() * 16 * 16]);
		}
		return emptyChunks.get(world.getMaxHeight());
	}

}
