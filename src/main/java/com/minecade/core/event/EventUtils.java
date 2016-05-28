package com.minecade.core.event;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class EventUtils {

	public static boolean isBlockClick(PlayerInteractEvent event, Material blockMaterial) {
		return (Action.LEFT_CLICK_BLOCK.equals(event.getAction()) || Action.RIGHT_CLICK_BLOCK.equals(event.getAction())) && event.getClickedBlock().getType().equals(blockMaterial);
	}

	public static boolean isBlockLeftClick(PlayerInteractEvent event) {
		return Action.LEFT_CLICK_BLOCK.equals(event.getAction());
	}

	public static boolean isBlockLeftClick(PlayerInteractEvent event, Material blockMaterial) {
		return Action.LEFT_CLICK_BLOCK.equals(event.getAction()) && event.getClickedBlock().getType().equals(blockMaterial);
	}

	public static boolean isBlockRightClick(PlayerInteractEvent event) {
		return Action.RIGHT_CLICK_BLOCK.equals(event.getAction());
	}

	public static boolean isBlockRightClick(PlayerInteractEvent event, Material blockMaterial) {
		return Action.RIGHT_CLICK_BLOCK.equals(event.getAction()) && event.getClickedBlock().getType().equals(blockMaterial);
	}

	public static boolean isChestClick(PlayerInteractEvent event) {
		return event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST;
	}

	public static boolean isItemClick(PlayerInteractEvent event) {
		switch (event.getAction()) {
		case LEFT_CLICK_AIR:
		case LEFT_CLICK_BLOCK:
		case RIGHT_CLICK_AIR:
		case RIGHT_CLICK_BLOCK:
			return event.hasItem();
		default:
			return false;
		}
	}

	public static boolean isItemRightClick(PlayerInteractEvent event) {
		return isRightClick(event) && event.hasItem();
	}

	public static boolean isItemRightClick(PlayerInteractEvent event, Material itemMaterial) {
		return isItemRightClick(event) && event.getItem().getType().equals(itemMaterial);
	}

	public static boolean isPressurePlateClick(PlayerInteractEvent event) {
		return event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.WOOD_PLATE || event.getClickedBlock().getType() == Material.STONE_PLATE || event.getClickedBlock().getType() == Material.IRON_PLATE);
	}

	public static boolean isRightClick(PlayerInteractEvent event) {
		return Action.RIGHT_CLICK_AIR.equals(event.getAction()) || Action.RIGHT_CLICK_BLOCK.equals(event.getAction());
	}

	public static boolean isSignRightClick(PlayerInteractEvent event) {
		return isBlockRightClick(event) && event.getClickedBlock().getState() instanceof Sign;
	}

	public static boolean isSkullRightClick(PlayerInteractEvent event) {
		return isBlockRightClick(event) && event.getClickedBlock().getState() instanceof Skull;
	}

	private EventUtils() {
	}

}
