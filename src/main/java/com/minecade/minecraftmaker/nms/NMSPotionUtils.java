package com.minecade.minecraftmaker.nms;

import net.minecraft.server.v1_9_R1.NBTTagCompound;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import com.minecade.core.item.PotionBuilder;

public class NMSPotionUtils {
	
	public static ItemStack toItemStack(PotionBuilder potion, int amount) {
		ItemStack item = null;
		if (potion.isSplash()) {
			item = new ItemStack(Material.SPLASH_POTION, amount);
		} else if (potion.isLinger()) {
			item = new ItemStack(Material.LINGERING_POTION, amount);
		} else {
			item = new ItemStack(Material.POTION, amount);
		}
		net.minecraft.server.v1_9_R1.ItemStack stack = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tagCompound = stack.getTag();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
		}
		PotionType type = potion.getType();
		boolean _long = potion.hasExtendedDuration();
		boolean strong = false;
		String tag = "";
		if (type.equals(PotionType.FIRE_RESISTANCE)) {
			if (_long) {
				tag = "long_fire_resistance";
			} else {
				tag = "fire_restistance";
			}
		} else if (type.equals(PotionType.INSTANT_DAMAGE)) {
			if (strong) {
				tag = "strong_harming";
			} else {
				tag = "harming";
			}
		} else if (type.equals(PotionType.INSTANT_HEAL)) {
			if (strong) {
				tag = "strong_healing";
			} else {
				tag = "healing";
			}
		} else if (type.equals(PotionType.INVISIBILITY)) {
			if (_long) {
				tag = "long_invisibility";
			} else {
				tag = "invisibility";
			}
		} else if (type.equals(PotionType.JUMP)) {
			if (_long) {
				tag = "long_leaping";
			} else if (strong) {
				tag = "strong_leaping";
			} else {
				tag = "leaping";
			}
//		} else if (type.equals(PotionType.LUCK)) {
//			tag = "luck";
		} else if (type.equals(PotionType.NIGHT_VISION)) {
			if (_long) {
				tag = "long_night_vision";
			} else {
				tag = "night_vision";
			}
		} else if (type.equals(PotionType.POISON)) {
			if (_long) {
				tag = "long_poison";
			} else if (strong) {
				tag = "strong_poison";
			} else {
				tag = "poison";
			}
		} else if (type.equals(PotionType.REGEN)) {
			if (_long) {
				tag = "long_regeneration";
			} else if (strong) {
				tag = "strong_regeneration";
			} else {
				tag = "regeneration";
			}
		} else if (type.equals(PotionType.SLOWNESS)) {
			if (_long) {
				tag = "long_slowness";
			} else {
				tag = "slowness";
			}
		} else if (type.equals(PotionType.SPEED)) {
			if (_long) {
				tag = "long_swiftness";
			} else if (strong) {
				tag = "strong_swiftness";
			} else {
				tag = "swiftness";
			}
		} else if (type.equals(PotionType.STRENGTH)) {
			if (_long) {
				tag = "long_strength";
			} else if (strong) {
				tag = "strong_strength";
			} else {
				tag = "strength";
			}
		} else if (type.equals(PotionType.WATER_BREATHING)) {
			if (_long) {
				tag = "long_water_breathing";
			} else {
				tag = "water_breathing";
			}
		} else if (type.equals(PotionType.WATER)) {
			tag = "water";
		} else if (type.equals(PotionType.WEAKNESS)) {
			if (_long) {
				tag = "long_weakness";
			} else {
				tag = "weakness";
			}
//		} else if (type.equals(PotionType.EMPTY)) {
//			tag = "empty";
//		} else if (type.equals(PotionType.MUNDANE)) {
//			tag = "mundane";
//		} else if (type.equals(PotionType.THICK)) {
//			tag = "thick";
//		} else if (type.equals(PotionType.AWKWARD)) {
//			tag = "awkward";
		} else {
			return null;
		}

		tagCompound.setString("Potion", "minecraft:" + tag);
		stack.setTag(tagCompound);
		return CraftItemStack.asBukkitCopy(stack);
	}

	private NMSPotionUtils() {
		super();
	}
}
