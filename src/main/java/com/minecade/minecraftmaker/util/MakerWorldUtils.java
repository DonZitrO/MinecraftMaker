package com.minecade.minecraftmaker.util;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class MakerWorldUtils {

	public static World createOrLoadWorld(JavaPlugin plugin, String worldName) {
		return createOrLoadWorld(plugin, worldName, null);
	}

	public static World createOrLoadWorld(JavaPlugin plugin, String worldName, Vector defaultSpawnVector) {
		WorldCreator worldCreator = new WorldCreator(worldName);
		plugin.getServer().setDefaultGameMode(GameMode.ADVENTURE);
		worldCreator.generator(plugin.getDefaultWorldGenerator(worldName, null));
		World world = worldCreator.createWorld();
		setupWorld(world);
		if (null != defaultSpawnVector) {
			world.setSpawnLocation(defaultSpawnVector.getBlockX(), defaultSpawnVector.getBlockY(), defaultSpawnVector.getBlockZ());
		}
		return world;
	}

	public static void setupWorld(World world) {
		// world auto save disable
		world.setAutoSave(false);
		// difficulty
		world.setDifficulty(Difficulty.HARD);
		// don't Spawn monster and animals
		world.setSpawnFlags(false, false);
		// No storms
		world.setStorm(false);
		// no thunders
		world.setThundering(false);
		// set day light
		world.setTime(0);
		// forever sunny
		world.setWeatherDuration(Integer.MAX_VALUE);
		// Enables/disables text output of command block commands to console
		world.setGameRuleValue("commandBlockOutput", "false");
		// Enables/disables day/night cycle
		world.setGameRuleValue("doDaylightCycle", "false");
		// Enables/disables fire updates (no fire spread or dissipation)
		world.setGameRuleValue("doFireTick", "false");
		// Enables/disables mob drops
		world.setGameRuleValue("doMobLoot", "false");
		// Enables/disables the spawning of mobs unless you want them to
		// (excl: eggs and mob spawners will still spawn mobs)
		world.setGameRuleValue("doMobSpawning", "false");
		// Enables/disables blocks dropping items when broken (includes TNT
		// destroying blocks)
		world.setGameRuleValue("doTileDrops", "false");
		// Enables/disables keeping inventory on death
		world.setGameRuleValue("keepInventory", "false");
		// Enables/disables creepers, ghasts, and Wither blowing up blocks,
		// endermen picking up blocks and zombies breaking doors
		world.setGameRuleValue("mobGriefing", "false");
		// Allows/Disallows player to naturally regenerate health,
		// regardless of food level
		world.setGameRuleValue("naturalRegeneration", "false");
	}

	public static void removeAllLivingEntitiesExceptPlayers(World world) {
		for (Entity e : world.getEntities()) {
			if (e instanceof LivingEntity && !(e instanceof Player)) {
				e.remove();
			}
		}
	}

	private MakerWorldUtils() {
	}

}
