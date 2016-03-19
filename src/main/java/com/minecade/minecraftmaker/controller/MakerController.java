package com.minecade.minecraftmaker.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.minecade.minecraftmaker.MakerArena;
import com.minecade.minecraftmaker.MakerPlayer;
import com.minecade.minecraftmaker.MinecraftMaker;
import com.minecade.minecraftmaker.util.LevelUtils;
import com.minecade.minecraftmaker.util.Tickable;
import com.minecade.minecraftmaker.util.TickableUtils;

public class MakerController implements Runnable, Tickable {

	private static final int DEFAULT_MAX_PLAYERS = 40;

	private final MinecraftMaker plugin;

	private BukkitTask globalTickerTask;
	private String mainWorldName;
	private World mainWorld;

	private long currentTick;
	private boolean enabled;
	private int maxPlayers;

	// keeps track of every player on the server
	private Map<UUID, MakerPlayer> playerMap;
	// keeps track of every arena on the server
	protected Map<UUID, MakerArena> arenaMap;

	public MakerController(MinecraftMaker plugin, ConfigurationSection config) {
		this.plugin = plugin;
		this.mainWorldName = config != null ? config.getString("main-world", "world") : "world";
		this.maxPlayers = config != null ? config.getInt("max-players", DEFAULT_MAX_PLAYERS) : DEFAULT_MAX_PLAYERS;
	}

	@Override
	public void disable() {
		if (!enabled) {
			return;
		}
		if (globalTickerTask != null) {
			globalTickerTask.cancel();
		}
		enabled = false;
	}

	@Override
	public void enable() {
		if (enabled) {
			throw new IllegalStateException("This controller is already enabled");
		}
		playerMap =  Collections.synchronizedMap(new LinkedHashMap<UUID, MakerPlayer>(maxPlayers * 4, .75f, true) {
			private static final long serialVersionUID = 1L;
			protected boolean removeEldestEntry(Map.Entry<UUID, MakerPlayer> eldest) {
				return size() > (maxPlayers * 2);
			}
		});
		arenaMap = new ConcurrentHashMap<>();
		globalTickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 0);
		enabled = true;
	}

	@Override
	public long getCurrentTick() {
		return currentTick;
	}

	public World getMainWorld() {
		if (this.mainWorld == null) {
			this.mainWorld = Bukkit.getWorld(this.mainWorldName);
		}
		return this.mainWorld;
	}

	public MakerArena getArena(UUID arenaId) {
		return arenaMap.get(arenaId);
	}

	public MakerPlayer getPlayer(Player player) {
		return getPlayer(player.getUniqueId());
	}

	public MakerPlayer getPlayer(UUID playerId) {
		return playerMap.get(playerId);
	}

	@Override
	public void run() {
		tick(getCurrentTick() + 1);
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
		// tick arenas
		for (MakerArena arena : new ArrayList<MakerArena>(arenaMap.values())) {
			if (arena != null) {
				TickableUtils.tickSafely(arena, currentTick);
			}
		}
		// tick players
		for (MakerPlayer mPlayer : new ArrayList<MakerPlayer>(playerMap.values())) {
			if (mPlayer != null) {
				TickableUtils.tickSafely(mPlayer, currentTick);
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void createEmptyLevel(String string, int chunkZ) {
		// TODO register level on slot
		LevelUtils.placeEmptyLevel(getMainWorld(), chunkZ);
	}

}
