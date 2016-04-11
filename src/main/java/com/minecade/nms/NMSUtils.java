package com.minecade.nms;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_9_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftCaveSpider;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftSpider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import net.minecraft.server.v1_9_R1.IChatBaseComponent;
import net.minecraft.server.v1_9_R1.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_9_R1.EntityCaveSpider;
import net.minecraft.server.v1_9_R1.EntityHuman;
import net.minecraft.server.v1_9_R1.EntitySpider;
import net.minecraft.server.v1_9_R1.PacketPlayOutChat;
import net.minecraft.server.v1_9_R1.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_9_R1.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_9_R1.WorldServer;

// let's try to move most of the NMS code to this class
public class NMSUtils {

	// default Bukkit World.createExplosion method won't allow setting a source
	public static void createExplosion(Entity source, Location location, float power) {
		WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
		world.explode(((CraftEntity) source).getHandle(), location.getX(), location.getY(), location.getZ(), power, false);
	}

	public static void sendActionMessage(Player p, String message) {
		IChatBaseComponent icbc = ChatSerializer.a("{\"text\": \"" + ChatColor.translateAlternateColorCodes('&', message) + "\"}");
		PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte) 2);
		((CraftPlayer)p).getHandle().playerConnection.sendPacket(bar);
	}

	// TODO: explore this code to possibly make mobs attack each other
	public static void makeSpiderAgressiveOnDaylight(Entity entity) {
		if (entity instanceof CraftCaveSpider) {
			EntityCaveSpider spider = ((CraftCaveSpider)entity).getHandle();
			spider.goalSelector.a(4, new PathfinderGoalMeleeAttack(spider, 1.0D, true));
			spider.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget<EntityHuman>(spider, EntityHuman.class, true));
		} else if (entity instanceof CraftSpider) {
			EntitySpider spider = ((CraftSpider)entity).getHandle();
			spider.goalSelector.a(4, new PathfinderGoalMeleeAttack(spider, 1.0D, true));
			spider.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget<EntityHuman>(spider, EntityHuman.class, true));
		}
	}

	private NMSUtils() {
		super();
	}

}
