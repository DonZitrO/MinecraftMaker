package com.minecade.minecraftmaker.schematic.bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.block.Biome;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.world.BaseBiome;
import com.minecade.minecraftmaker.schematic.world.BiomeData;
import com.minecade.minecraftmaker.schematic.world.BiomeRegistry;

/**
 * A biome registry for Bukkit.
 */
class BukkitBiomeRegistry implements BiomeRegistry {

	BukkitBiomeRegistry() {
	}

	@Nullable
	@Override
	public BaseBiome createFromId(int id) {
		return new BaseBiome(id);
	}

	@Override
	public List<BaseBiome> getBiomes() {
		BukkitImplAdapter adapter = MinecraftMakerPlugin.getInstance().getBukkitImplAdapter();
		if (adapter != null) {
			List<BaseBiome> biomes = new ArrayList<BaseBiome>();
			for (Biome biome : Biome.values()) {
				int biomeId = adapter.getBiomeId(biome);
				biomes.add(new BaseBiome(biomeId));
			}
			return biomes;
		} else {
			return Collections.emptyList();
		}
	}

	@Nullable
	@Override
	public BiomeData getData(BaseBiome biome) {
		BukkitImplAdapter adapter = MinecraftMakerPlugin.getInstance().getBukkitImplAdapter();
		if (adapter != null) {
			final Biome bukkitBiome = adapter.getBiome(biome.getId());
			return new BiomeData() {
				@Override
				public String getName() {
					return bukkitBiome.name();
				}
			};
		} else {
			return null;
		}
	}

}
