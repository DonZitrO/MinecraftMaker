package com.minecade.minecraftmaker.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class FireworksUtils {

	private static final List<Color> COLORS = Arrays.asList(
			Color.AQUA, Color.BLACK, Color.BLUE, Color.FUCHSIA, Color.GRAY, Color.GREEN,
			Color.LIME, Color.MAROON, Color.NAVY, Color.OLIVE, Color.ORANGE, Color.PURPLE,
			Color.RED, Color.SILVER, Color.TEAL, Color.WHITE, Color.YELLOW, Color.WHITE);
	private static final Random RANDOM = new Random();

	public static void launch(Location location){
		FireworksUtils firework = new FireworksUtils();
		firework.launchTask(location);
	}

	private int iterations = 10;
	private BukkitTask task;

	private void launchTask(final Location location){
		task = Bukkit.getScheduler().runTaskTimer(MinecraftMakerPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				// Spawn the Firework, get the FireworkMeta.
				Firework firework = (Firework) location.getWorld().spawnEntity(location.add(0, 1, 0), EntityType.FIREWORK);
				Type type = Type.values()[RANDOM.nextInt(Type.values().length)];
				Color fadeColor = COLORS.get(RANDOM.nextInt(COLORS.size()));
				Color color = COLORS.get(RANDOM.nextInt(COLORS.size()));

				// Create our effect with this
				FireworkEffect effect = FireworkEffect.builder().flicker(RANDOM.nextBoolean())
						.withColor(color)
						.withFade(fadeColor)
						.with(type)
						.trail(RANDOM.nextBoolean())
						.build();

				// Set Meta
				FireworkMeta fireworkMeta = firework.getFireworkMeta();
				fireworkMeta.setPower(RANDOM.nextInt(3));
				fireworkMeta.addEffect(effect);
				firework.setFireworkMeta(fireworkMeta);

				if(--iterations == 0){
					task.cancel();
				}
			}
		},  1 * 20, 1 * 20);
	}
}
