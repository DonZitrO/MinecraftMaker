package com.minecade.core.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spigotmc.SpigotConfig;

public class ServerPropertyFilesConfigurator {

	public static void configureServerProperties() {
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream("server.properties")) {
			props.load(in);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureServerProperties - unable to load server.properties file: %s", e.getMessage()));
			e.printStackTrace();
			Bukkit.shutdown();
		}
		boolean changed = forcePropertyValue(props, "allow-nether", "false");
		changed |= forcePropertyValue(props, "enable-query", "false");
		changed |= forcePropertyValue(props, "enable-rcon", "false");
		changed |= forcePropertyValue(props, "hardcore", "false");
		changed |= forcePropertyValue(props, "announce-player-achievements", "false");
		changed |= forcePropertyValue(props, "hardcore", "false");
		changed |= forcePropertyValue(props, "snooper-enabled", "false");
		changed |= forcePropertyValue(props, "spawn-protection", "0");
		changed |= forcePropertyValue(props, "view-distance", "8");
		changed |= forcePropertyValue(props, "gamemode", "2");
		changed |= forcePropertyValue(props, "difficulty", "3");
		changed |= forcePropertyValue(props, "online-mode", "false");
		changed |= forcePropertyValue(props, "pvp", "false");

		if (changed) {
			Bukkit.getLogger().info(String.format("ServerPropertyFilesConfigurator.configureServerProperties - server.properties file changed - restarting server..."));
			try (FileOutputStream out = new FileOutputStream("server.properties")) {
				props.store(out, null);
				out.flush();
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureServerProperties - unable to update server.properties file: %s", e.getMessage()));
				e.printStackTrace();
			}
			Bukkit.shutdown();
		}
	}

	public static void configureBukkitYML() {
		YamlConfiguration bukkitConfig = new YamlConfiguration();
		File file = new File("bukkit.yml");
		try {
			bukkitConfig.load(file);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureBukkitYML - unable to load bukkit.yml file: %s", e.getMessage()));
			e.printStackTrace();
			Bukkit.shutdown();
		}
		boolean changed = forcePropertyValue(bukkitConfig, "settings.connection-throttle", -1);
		changed |= forcePropertyValue(bukkitConfig, "settings.allow-end", false);
		changed |= forcePropertyValue(bukkitConfig, "worlds.world.generator", "VoidGenerator");
		changed |= forcePropertyValue(bukkitConfig, "worlds.mcmaker.generator", "VoidGenerator");
		changed |= forcePropertyValue(bukkitConfig, "ticks-per.autosave", 0);
		if (changed) {
			Bukkit.getLogger().info(String.format("ServerPropertyFilesConfigurator.configureBukkitYML - bukkit.yml file changed - restarting server..."));
			try {
				bukkitConfig.save(file);
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureBukkitYML - unable to update bukkit.yml file: %s", e.getMessage()));
				e.printStackTrace();
			}
			Bukkit.shutdown();
		}
	}

//	public static void configurePermissionsYML() {
//		YamlConfiguration permissionConfig = new YamlConfiguration();
//		File file = new File("permissions.yml");
//		char originalSeparator = permissionConfig.options().pathSeparator();
//		permissionConfig.options().pathSeparator('_');
//		try {
//			try {
//				file.createNewFile();
//				permissionConfig.load(file);
//			} catch (Exception e) {
//				Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configurePermissionsYML - unable to load permissions.yml file: %s", e.getMessage()));
//				e.printStackTrace();
//				Bukkit.shutdown();
//			}
//			boolean changed = forcePropertyValue(permissionConfig, "minecraft_description", "Gives the user the ability to use all vanilla utilities and commands");
//			changed |= forcePropertyValue(permissionConfig, "minecraft_default", "op");
//			changed |= forcePropertyValue(permissionConfig, "minecraft_children_minecraft.command", true);
//			changed |= forcePropertyValue(permissionConfig, "minecraft.command_description", "Gives the user the ability to use all vanilla minecraft commands");
//			changed |= forcePropertyValue(permissionConfig, "minecraft.command_default", "op");
//			changed |= forcePropertyValue(permissionConfig, "minecraft.command_children_minecraft.command.me", true);
//			changed |= forcePropertyValue(permissionConfig, "minecraft.command.me_description", "Allows the user to perform a chat action");
//			changed |= forcePropertyValue(permissionConfig, "minecraft.command.me_default", "op");
//			changed |= forcePropertyValue(permissionConfig, "bukkit_description", "Gives the user the ability to use all vanilla utilities and commands");
//			changed |= forcePropertyValue(permissionConfig, "bukkit_default", "op");
//			changed |= forcePropertyValue(permissionConfig, "bukkit_children_bukkit.command", true);
//			changed |= forcePropertyValue(permissionConfig, "bukkit.command_description", "Gives the user the ability to use all vanilla minecraft commands");
//			changed |= forcePropertyValue(permissionConfig, "bukkit.command_default", "op");
//			changed |= forcePropertyValue(permissionConfig, "bukkit.command_children_bukkit.command.me", true);
//			changed |= forcePropertyValue(permissionConfig, "bukkit.command.me_description", "Allows the user to perform a chat action");
//			changed |= forcePropertyValue(permissionConfig, "bukkit.command.me_default", "op");
//			if (changed) {
//				Bukkit.getLogger().info(String.format("ServerPropertyFilesConfigurator.configureBukkitYML - permissions.yml file changed - restarting server..."));
//				try {
//					permissionConfig.save(file);
//				} catch (Exception e) {
//					Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureBukkitYML - unable to update server.properties file: %s", e.getMessage()));
//					e.printStackTrace();
//				}
//				Bukkit.shutdown();
//			}
//		} finally {
//			permissionConfig.options().pathSeparator(originalSeparator);
//		}
//	}

	public static void configureSpigotYML() {
		File file = new File("spigot.yml");
		YamlConfiguration spigotConfig = SpigotConfig.config;
		if (spigotConfig == null) {
			try {
				spigotConfig = YamlConfiguration.loadConfiguration(file);
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureSpigotYML - unable to load spigot.yml file: %s", e.getMessage()));
				e.printStackTrace();
				Bukkit.shutdown();
			}
		}
		boolean changed = forcePropertyValue(spigotConfig, "settings.bungeecord", true);
		changed |= forcePropertyValue(spigotConfig, "settings.restart-on-crash", false);
		changed |= forcePropertyValue(spigotConfig, "stats.disable-saving", true);
		changed |= forcePropertyValue(spigotConfig, "stats.forced-stats.achievement.openInventory", 1);
		if (changed) {
			Bukkit.getLogger().info(String.format("ServerPropertyFilesConfigurator.configureSpigotYML - spigot.yml file changed - restarting server..."));
			try {
				spigotConfig.save(file);
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureSpigotYML - unable to update spigot.yml file: %s", e.getMessage()));
				e.printStackTrace();
			}
			SpigotConfig.config = spigotConfig;
			Bukkit.shutdown();
		}
	}

	private static boolean forcePropertyValue(Properties props, String key, String value) {
		boolean changed = false;
		if (!props.containsKey(key) || !value.equalsIgnoreCase(props.getProperty(key))) {
			props.setProperty(key, value);
			changed = true;
		}
		return changed;
	}

	private static boolean forcePropertyValue(ConfigurationSection config, String key, Object value) {
		boolean changed = false;
		if (!config.contains(key) || !String.valueOf(value).equalsIgnoreCase(String.valueOf(config.get(key)))) {
			config.set(key, value);
			changed = true;
		}
		return changed;
	}

}
