package com.minecade.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_9_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftCaveSpider;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftSpider;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

import net.minecraft.server.v1_9_R1.Entity;
import net.minecraft.server.v1_9_R1.EntityInsentient;
import net.minecraft.server.v1_9_R1.EntityItem;
import net.minecraft.server.v1_9_R1.IChatBaseComponent;
import net.minecraft.server.v1_9_R1.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_9_R1.EntityCaveSpider;
import net.minecraft.server.v1_9_R1.EntityHuman;
import net.minecraft.server.v1_9_R1.EntitySpider;
import net.minecraft.server.v1_9_R1.PacketPlayOutChat;
import net.minecraft.server.v1_9_R1.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_9_R1.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_9_R1.PathfinderGoalSelector;
import net.minecraft.server.v1_9_R1.WorldServer;

// let's try to move most of the NMS code to this class
public class NMSUtils {

	public static void clearTitle(Player player) {
		sendTitle(player, 0, 0, 0, "", "");
	}

	// default Bukkit World.createExplosion method won't allow setting a source
	public static void createExplosion(org.bukkit.entity.Entity source, Location location, float power) {
		WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
		world.explode(((CraftEntity) source).getHandle(), location.getX(), location.getY(), location.getZ(), power, false);
	}

	public static Class<?> getNMSClass(String name) {
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
		try {
			return Class.forName("net.minecraft.server." + version + "." + name);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	// TODO: explore this code to possibly make mobs attack each other
	public static void makeSpiderAgressiveOnDaylight(org.bukkit.entity.Entity entity) {
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

	public static void sendActionMessage(Player p, String message) {
		IChatBaseComponent icbc = ChatSerializer.a("{\"text\": \"" + ChatColor.translateAlternateColorCodes('&', message) + "\"}");
		PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte) 2);
		((CraftPlayer)p).getHandle().playerConnection.sendPacket(bar);
	}


	public static void sendPacket(Player player, Object packet) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendTabTitle(Player player, String header, String footer) {
		if (header == null) header = "";
		header = ChatColor.translateAlternateColorCodes('&', header);

		if (footer == null) footer = "";
		footer = ChatColor.translateAlternateColorCodes('&', footer);

		header = header.replaceAll("%player%", player.getDisplayName());
		footer = footer.replaceAll("%player%", player.getDisplayName());

		try {
			Object tabHeader = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + header + "\"}");
			Object tabFooter = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\"" + footer + "\"}");
			Constructor<?> titleConstructor = getNMSClass("PacketPlayOutPlayerListHeaderFooter").getConstructor(getNMSClass("IChatBaseComponent"));
			Object packet = titleConstructor.newInstance(tabHeader);
			Field field = packet.getClass().getDeclaredField("b");
			field.setAccessible(true);
			field.set(packet, tabFooter);
			sendPacket(player, packet);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {

		try {
			Object e;
			Object chatTitle;
			Object chatSubtitle;
			Constructor<?> subtitleConstructor;
			Object titlePacket;
			Object subtitlePacket;

			if (title != null) {
				title = ChatColor.translateAlternateColorCodes('&', title);
				title = title.replaceAll("%player%", player.getDisplayName());
				// Times packets
				e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get((Object) null);
				chatTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", new Class[]{String.class}).invoke((Object) null, new Object[]{"{\"text\":\"" + title + "\"}"});
				subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(new Class[]{getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), Integer.TYPE, Integer.TYPE, Integer.TYPE});
				titlePacket = subtitleConstructor.newInstance(new Object[]{e, chatTitle, fadeIn, stay, fadeOut});
				sendPacket(player, titlePacket);

				e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get((Object) null);
				chatTitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", new Class[]{String.class}).invoke((Object) null, new Object[]{"{\"text\":\"" + title + "\"}"});
				subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(new Class[]{getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent")});
				titlePacket = subtitleConstructor.newInstance(new Object[]{e, chatTitle});
				sendPacket(player, titlePacket);
			}

			if (subtitle != null) {
				subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
				subtitle = subtitle.replaceAll("%player%", player.getDisplayName());
				// Times packets
				e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TIMES").get((Object) null);
				chatSubtitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", new Class[]{String.class}).invoke((Object) null, new Object[]{"{\"text\":\"" + title + "\"}"});
				subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(new Class[]{getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), Integer.TYPE, Integer.TYPE, Integer.TYPE});
				subtitlePacket = subtitleConstructor.newInstance(new Object[]{e, chatSubtitle, fadeIn, stay, fadeOut});
				sendPacket(player, subtitlePacket);

				e = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get((Object) null);
				chatSubtitle = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", new Class[]{String.class}).invoke((Object) null, new Object[]{"{\"text\":\"" + subtitle + "\"}"});
				subtitleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(new Class[]{getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), Integer.TYPE, Integer.TYPE, Integer.TYPE});
				subtitlePacket = subtitleConstructor.newInstance(new Object[]{e, chatSubtitle, fadeIn, stay, fadeOut});
				sendPacket(player, subtitlePacket);
			}
		} catch (Exception var11) {
			var11.printStackTrace();
		}
	}

	public static void stopItemFromDespawning(Item drop) {
		try {
			if (drop instanceof CraftItem) {
				Entity entity = ((CraftItem) drop).getHandle();
				if (entity instanceof EntityItem) {
					EntityItem item = (EntityItem) entity;
					Field age = item.getClass().getDeclaredField("age");
					if (age != null) {
						age.setAccessible(true);
					}
					age.set(item, -32768);
					if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
						Bukkit.getLogger().info(String.format("[DEBUG] | NMSUtils.stopItemFromDespawning - stopped item drop from despawning: %s", drop.getItemStack().getType()));
					}
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("NMSUtils.stopItemFromDespawning - unable to stop item drop from despawning: %s", e.getMessage()));
			e.printStackTrace();
		}
	}

	public static void stopMobFromMovingAndAttacking(org.bukkit.entity.Entity entity) {
		if (entity instanceof CraftEntity) {
			Entity mob = ((CraftEntity)entity).getHandle();
			if (mob instanceof EntityInsentient) {
				EntityInsentient insentient = (EntityInsentient)mob;
				insentient.goalSelector = new PathfinderGoalSelector(insentient.world != null && insentient.world.methodProfiler != null ? insentient.world.methodProfiler : null);
				insentient.targetSelector = new PathfinderGoalSelector(insentient.world != null && insentient.world.methodProfiler != null ? insentient.world.methodProfiler : null);
			}
		}
	}

	private NMSUtils() {
		super();
	}

}
