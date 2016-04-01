package com.minecade.core.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BungeeUtils {

	public static final String BUNGEECORD_CHANNEL = "BungeeCord";

	public static void switchServer(final JavaPlugin plugin, final Player player, final String server, final String message) {
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();DataOutputStream out = new DataOutputStream(bytes);) {
			out.writeUTF("Connect");
			out.writeUTF(server);
			player.sendPluginMessage(plugin, BUNGEECORD_CHANNEL, bytes.toByteArray());
			player.sendMessage(message);
		} catch (Exception ex) {
			Bukkit.getLogger().severe(String.format("Unable to connect player: [%s] to server: [%s] with message: [%s] - %s", player.getName(), server, message, ex.getMessage()));
			//player.kickPlayer(message);
		}
	}

	private BungeeUtils() {
		super();
	}

}
