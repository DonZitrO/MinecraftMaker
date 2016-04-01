package com.minecade.core.config;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import com.minecade.core.exception.ConfigException;

public class ConfigUtils {

	public static double getRequiredConfigDouble(ConfigurationSection config, String key) {
		double value = config.getDouble(key);
		validateNotNull(config, key, value);
		return value;
	}

	public static float getRequiredConfigFloat(ConfigurationSection config, String key) {
		return (float) getRequiredConfigDouble(config, key);
	}

	public static int getRequiredConfigInt(ConfigurationSection config, String key) {
		int value = config.getInt(key);
		validateNotNull(config, key, value);
		return value;
	}

	public static <T> List<T> getRequiredConfigList(ConfigurationSection config, String key, List<T> type) throws ConfigException {
		@SuppressWarnings("unchecked")
		List<T> value = (List<T>) config.getList(key, type);
		validateNotNull(config, key, value);
		return value;
	}

	public static String getRequiredConfigString(ConfigurationSection config, String key) throws ConfigException {
		String value = config.getString(key);
		validateNotNull(config, key, value);
		return value;
	}

	public static Vector getRequiredConfigVector(ConfigurationSection config, String key) throws ConfigException {
		Vector value = config.getVector(key);
		validateNotNull(config, key, value);
		return value;
	}

	private static void validateNotNull(ConfigurationSection config, String key, Object value) throws ConfigException {
		if (null == value || !config.contains(key)) {
			throw new ConfigException(String.format("Required value for config key: [%s] was not found in config section: [%s]", key, (null == config.getParent()) ? "<root>" : config.getName()));
		}
		Bukkit.getLogger().info(String.format("Found value: [%s] for key: [%s] on config: [%s]", value, key, (null == config.getParent()) ? "<root>" : config.getName()));
	}

	private ConfigUtils() {
		super();
	}

}
