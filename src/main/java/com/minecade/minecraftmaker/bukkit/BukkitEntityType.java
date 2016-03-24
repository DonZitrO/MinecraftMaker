package com.minecade.minecraftmaker.bukkit;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Golem;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.minecart.ExplosiveMinecart;

import com.minecade.minecraftmaker.util.Enums;

class BukkitEntityType implements EntityType {

	private static final org.bukkit.entity.EntityType armorStandType = Enums.findByValue(org.bukkit.entity.EntityType.class, "ARMOR_STAND");

	private final Entity entity;

	BukkitEntityType(Entity entity) {
		checkNotNull(entity);
		this.entity = entity;
	}

	@Override
	public boolean isPlayerDerived() {
		return entity instanceof HumanEntity;
	}

	@Override
	public boolean isProjectile() {
		return entity instanceof Projectile;
	}

	@Override
	public boolean isItem() {
		return entity instanceof Item;
	}

	@Override
	public boolean isFallingBlock() {
		return entity instanceof FallingBlock;
	}

	@Override
	public boolean isPainting() {
		return entity instanceof Painting;
	}

	@Override
	public boolean isItemFrame() {
		return entity instanceof ItemFrame;
	}

	@Override
	public boolean isBoat() {
		return entity instanceof Boat;
	}

	@Override
	public boolean isMinecart() {
		return entity instanceof Minecart;
	}

	@Override
	public boolean isTNT() {
		return entity instanceof TNTPrimed || entity instanceof ExplosiveMinecart;
	}

	@Override
	public boolean isExperienceOrb() {
		return entity instanceof ExperienceOrb;
	}

	@Override
	public boolean isLiving() {
		return entity instanceof LivingEntity;
	}

	@Override
	public boolean isAnimal() {
		return entity instanceof Animals;
	}

	@Override
	public boolean isAmbient() {
		return entity instanceof Ambient;
	}

	@Override
	public boolean isNPC() {
		return entity instanceof Villager;
	}

	@Override
	public boolean isGolem() {
		return entity instanceof Golem;
	}

	@Override
	public boolean isTamed() {
		return entity instanceof Tameable && ((Tameable) entity).isTamed();
	}

	@Override
	public boolean isTagged() {
		return entity instanceof LivingEntity && ((LivingEntity) entity).getCustomName() != null;
	}

	@Override
	public boolean isArmorStand() {
		return entity.getType() == armorStandType;
	}

}
