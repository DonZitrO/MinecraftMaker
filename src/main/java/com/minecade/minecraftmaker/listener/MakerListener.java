package com.minecade.minecraftmaker.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.minecade.core.event.AsyncAccountDataLoadEvent;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;

public class MakerListener implements Listener {

	private final MinecraftMakerPlugin plugin;

	public MakerListener(MinecraftMakerPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		// prevents mobs from burning in the daylight
		if (event.getDuration() == 8 && !(event.getEntity() instanceof Player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerInteract - Player: [%s] -  Action: [%s] - Cancelled: [%s]", event.getPlayer().getName(), event.getAction(), event.isCancelled()));
		}
		// delegate to controller for specific behavior
		plugin.getController().onPlayerInteract(event);
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerListener.onPlayerInteract - Exit - Player: [%s] - Action: [%s] - Cancelled: [%s]", event.getPlayer().getName(), event.getAction(), event.isCancelled()));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public final void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onAsyncPlayerPreLogin - Starting... - Player: [%s<%s>] - Initial result: [%s]", event.getName(), event.getUniqueId(), event.getLoginResult()));
		plugin.getController().onAsyncPlayerPreLogin(event);
		Bukkit.getLogger().info(String.format("MakerListener.onAsyncPlayerPreLogin - Finished - Player: [%s<%s>] - Result: [%s]", event.getName(), event.getUniqueId(), event.getLoginResult()));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public final void onPlayerJoin(PlayerJoinEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onPlayerJoin - Player: [%s<%s>]", event.getPlayer().getName(), event.getPlayer().getUniqueId()));
		event.setJoinMessage(null);
		plugin.getController().onPlayerJoin(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public final void onPlayerQuit(PlayerQuitEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onPlayerQuit - Player: [%s<%s>]", event.getPlayer().getName(), event.getPlayer().getUniqueId()));
		event.setQuitMessage(null);
		plugin.getController().onPlayerQuit(event.getPlayer());
	}

	@EventHandler
	public final void onAsyncAccountDataLoad(AsyncAccountDataLoadEvent event) {
		Bukkit.getLogger().info(String.format("MakerListener.onAsyncAccountDataLoad - Player: [%s<%s>]", event.getData().getUsername(), event.getData().getUniqueId()));
		if (event.getData() instanceof MakerPlayerData) {
			plugin.getController().onAsyncAccountDataLoad((MakerPlayerData)event.getData());
		}
	}
}
