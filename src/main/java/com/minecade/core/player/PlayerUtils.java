package com.minecade.core.player;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BlockIterator;

public final class PlayerUtils {

	public static Block getPlayerTargetBlock(Player player, int distance) {
		BlockIterator bit = new BlockIterator(player, distance);
		while (bit.hasNext()) {
			Block next = bit.next();
			if (!Material.AIR.equals(next.getType())) {
				return next;
			}
		}
		return null;
	}

	public static boolean isOpInCreativeMode(Player player) {
		if (player.isOp() && GameMode.CREATIVE.equals(player.getGameMode())) {
			return true;
		}
		return false;
	}

	public static void resetPlayer(Player player) {
		resetPlayer(player, GameMode.ADVENTURE);
	}

	public static void resetPlayer(Player player, GameMode gameMode) {
		if (!player.getGameMode().equals(gameMode)) {
			player.setGameMode(gameMode);
		}
		player.setLevel(0);
		player.setExp(0);
		player.setFoodLevel(100);
		player.getInventory().clear();
		player.setFireTicks(0);
		player.setHealth(player.getMaxHealth());
		player.setFlying(false);
		for (final PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
	}

	public static void resetPlayerVisibility(Player player) {
		for (Player other : Bukkit.getOnlinePlayers()) {
			player.showPlayer(other);
			other.showPlayer(player);
		}
	}

	private PlayerUtils() {
		super();
	}

}
