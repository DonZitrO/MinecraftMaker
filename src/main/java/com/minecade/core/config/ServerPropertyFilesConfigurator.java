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
		}
		boolean changed = forcePropertyValue(bukkitConfig, "settings.connection-throttle", -1);
		changed |= forcePropertyValue(bukkitConfig, "settings.allow-end", false);
		changed |= forcePropertyValue(bukkitConfig, "worlds.world.generator", "VoidGenerator");
		if (changed) {
			Bukkit.getLogger().info(String.format("ServerPropertyFilesConfigurator.configureBukkitYML - bukkit.yml file changed - restarting server..."));
			try {
				bukkitConfig.save(file);
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureBukkitYML - unable to update server.properties file: %s", e.getMessage()));
				e.printStackTrace();
			}
			Bukkit.shutdown();
		}
	}

	public static void configureSpigotYML() {
		File file = new File("spigot.yml");
		YamlConfiguration spigotConfig = SpigotConfig.config;
		if (spigotConfig == null) {
			try {
				spigotConfig = YamlConfiguration.loadConfiguration(file);
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureSpigotYML - unable to load spigot.yml file: %s", e.getMessage()));
				e.printStackTrace();
				return;
			}
		}
		boolean changed = forcePropertyValue(spigotConfig, "settings.bungeecord", true);
		changed |= forcePropertyValue(spigotConfig, "settings.restart-on-crash", false);
		if (changed) {
			Bukkit.getLogger().info(String.format("ServerPropertyFilesConfigurator.configureSpigotYML - spigot.yml file changed - restarting server..."));
			try {
				spigotConfig.save(file);
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("ServerPropertyFilesConfigurator.configureSpigotYML - unable to update server.properties file: %s", e.getMessage()));
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
