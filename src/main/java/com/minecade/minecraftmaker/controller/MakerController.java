package com.minecade.minecraftmaker.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.minecade.core.data.Rank;
import com.minecade.core.event.AsyncAccountDataLoadEvent;
import com.minecade.core.event.EventUtils;
import com.minecade.core.item.ItemUtils;
import com.minecade.core.player.PlayerUtils;
import com.minecade.core.util.BungeeUtils;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.function.mask.ExistingBlockMask;
import com.minecade.minecraftmaker.function.operation.ResumableForwardExtentCopy;
import com.minecade.minecraftmaker.function.operation.ResumableOperationQueue;
import com.minecade.minecraftmaker.function.operation.SchematicWriteOperation;
import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.MakerLobbyItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.bukkit.BukkitUtil;
import com.minecade.minecraftmaker.schematic.exception.FilenameException;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.io.BlockArrayClipboard;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.io.ClipboardFormat;
import com.minecade.minecraftmaker.schematic.io.ClipboardReader;
import com.minecade.minecraftmaker.schematic.world.BlockTransformExtent;
import com.minecade.minecraftmaker.schematic.world.MakerExtent;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.WorldData;
import com.minecade.minecraftmaker.util.FileUtils;
import com.minecade.minecraftmaker.util.LevelUtils;
import com.minecade.minecraftmaker.util.MakerWorldUtils;
import com.minecade.minecraftmaker.util.Tickable;
import com.minecade.minecraftmaker.util.TickableUtils;

public class MakerController implements Runnable, Tickable {

	private static final int FAST_RELOGIN_DELAY_SECONDS = 5;
	private static final int DOUBLE_LOGIN_DELAY_SECONDS = 2;
	private static final int DEFAULT_MAX_PLAYERS = 40;
	private static final int DEFAULT_MAX_LEVELS = 10;
	private static final int MAX_ACCOUNT_DATA_ENTRIES = 20;
	private static final int MAX_ALLOWED_LOGIN_ENTRIES = 200;

	private static final Vector DEFAULT_SPAWN_VECTOR = new Vector(-16.5d, 65.0d, 16.5d);
	private static final float DEFAULT_SPAWN_YAW = -90f;
	private static final float DEFAULT_SPAWN_PITCH = 0f;

	private final MinecraftMakerPlugin plugin;

	private BukkitTask globalTickerTask;
	private String mainWorldName;
	private World mainWorld;
	private MakerExtent makerExtent;

	private Vector spawnVector;
	private float spawnYaw;
	private float spawnPitch;

	private long currentTick;
	private boolean enabled;
	private int maxPlayers;
	private short maxLevels;

	// keeps track of every player on the server
	private Map<UUID, MakerPlayer> playerMap;
	// keeps track of every arena on the server
	protected Map<Short, MakerLevel> levelMap;
	// shallow level data for server browser
	protected Map<Long, MakerLevel> levelsBySerialMap = Collections.synchronizedMap(new TreeMap<>());

	// an async thread loads the data to this map, then the main thread process it
	private final Map<UUID, MakerPlayerData> accountDataMap = Collections.synchronizedMap(new LinkedHashMap<UUID, MakerPlayerData>(MAX_ACCOUNT_DATA_ENTRIES * 2) {
		private static final long serialVersionUID = 1L;

		protected boolean removeEldestEntry(Map.Entry<UUID, MakerPlayerData> eldest) {
			return size() > MAX_ACCOUNT_DATA_ENTRIES;
		}

	});

	// used to control the fast relogin hack
	private final Map<UUID, Long> nextAllowedLogins = Collections.synchronizedMap(new LinkedHashMap<UUID, Long>() {
		private static final long serialVersionUID = 1L;

		protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) {
			return size() > MAX_ALLOWED_LOGIN_ENTRIES;
		}

	});

	public MakerController(MinecraftMakerPlugin plugin, ConfigurationSection config) {
		this.plugin = plugin;
		this.mainWorldName = config != null ? config.getString("main-world", "world") : "world";
		this.maxPlayers = config != null ? config.getInt("max-players", DEFAULT_MAX_PLAYERS) : DEFAULT_MAX_PLAYERS;
		this.maxLevels = config != null ? (short)config.getInt("max-levels", DEFAULT_MAX_LEVELS) : DEFAULT_MAX_LEVELS;
		this.spawnVector = config != null ? config.getVector("spawn-vector", DEFAULT_SPAWN_VECTOR) : DEFAULT_SPAWN_VECTOR;
		this.spawnYaw = config != null ? (float)config.getDouble("spawn-yaw", DEFAULT_SPAWN_YAW) : DEFAULT_SPAWN_YAW;
		this.spawnPitch = config != null ? (float)config.getDouble("spawn-pitch", DEFAULT_SPAWN_PITCH) : DEFAULT_SPAWN_PITCH;
	}

	private void addMakerPlayer(Player player, MakerPlayerData data) {
		final MakerPlayer mPlayer = new MakerPlayer(player, data);
		// add the player to the player map

		playerMap.put(mPlayer.getUniqueId(), mPlayer);
		// TODO: notify player joined
		//plugin.publishServerInfoAsync();

		// add the player to the lobby
		addPlayerToMainLobby(mPlayer);

		// TODO: welcome stuff if needed
	}

	public void addPlayerToMainLobby(MakerPlayer mPlayer) {
		// reset player
		mPlayer.resetPlayer();
		// teleport to spawn point
		if (mPlayer.getPlayer().getLocation().distanceSquared(getDefaultSpawnLocation()) > 4d) {
			if (!mPlayer.getPlayer().teleport(getDefaultSpawnLocation(), TeleportCause.PLUGIN)) {
				mPlayer.getPlayer().kickPlayer(plugin.getMessage("lobby.join.error.teleport"));
			}
		}
		// current level
		mPlayer.setCurrentLevel(null);
		// set lobby inventory
		mPlayer.resetLobbyInventory();
		// reset display name
		mPlayer.getPlayer().setDisplayName(mPlayer.getDisplayName());
		mPlayer.getPlayer().setPlayerListName(mPlayer.getDisplayName());
		// create player Lobby Scoreboard
		// TODO: mPlayer.addLobbyScoreboard();
		// we need this in order to display the rank tags over player heads
		// TODO: entriesToAddToScoreboardTeamsOnNextTick.add(mPlayer);
		// reset visibility
		PlayerUtils.resetPlayerVisibility(mPlayer.getPlayer());
	}

	// FIXME: this is an experimental proof of concept, don't use it elsewhere until tested/refined
	public void addServerBrowserLevels(final Map<Long, MakerLevel> levels, LevelSortBy sortBy) {
		levelsBySerialMap.putAll(levels);
		Bukkit.getScheduler().runTask(plugin, () -> LevelBrowserMenu.updatePages(plugin, levels.values(), sortBy));
	}

	private void controlDoubleLoginHack(AsyncPlayerPreLoginEvent event) {
		if (playerMap.containsKey(event.getUniqueId())) {
			Bukkit.getLogger().warning(String.format("[POSSIBLE-HACK] | MakerController.controlDoubleLoginHack - possible double login detected for player: [%s<%s>]", event.getName(), event.getUniqueId()));
			event.disallow(Result.KICK_OTHER, plugin.getMessage("server.error.double-login"));
		}
	}

	private void controlFastReloginHack(AsyncPlayerPreLoginEvent event) {
		Long nextAllowedLogin = nextAllowedLogins.get(event.getUniqueId());
		if (nextAllowedLogin != null && System.currentTimeMillis() < nextAllowedLogin.longValue()) {
			Bukkit.getLogger().warning(String.format("[POSSIBLE-HACK] | MakerController.controlFastReloginHack - possible too fast relogin detected for player: [%s<%s>]", event.getName(), event.getUniqueId()));
			event.disallow(Result.KICK_OTHER, plugin.getMessage("server.error.too-fast-relogin"));
		} else {
			nextAllowedLogins.put(event.getUniqueId(), System.currentTimeMillis() + (FAST_RELOGIN_DELAY_SECONDS * 1000));
		}
	}

	public void createEmptyLevel(MakerPlayer author, int floorBlockId) {
		if (!author.isInLobby() || author.hasPendingOperation()) {
			author.sendActionMessage(plugin, "level.create.error.author-busy");
			return;
		}
		MakerLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			author.sendActionMessage(plugin, "level.error.full");
			return;
		}
		level.setLevelId(UUID.randomUUID());
		level.setAuthorId(author.getUniqueId());
		level.setAuthorName(author.getName());
		author.sendActionMessage(plugin, "level.loading");
		try {
			level.setClipboard(LevelUtils.createEmptyLevelClipboard(getMainWorld(), level.getChunkZ(), floorBlockId));
			level.tryStatusTransition(LevelStatus.PREPARING, LevelStatus.CLIPBOARD_LOADED);
		} catch (MinecraftMakerException e) {
			Bukkit.getLogger().severe(String.format("MakerController.createEmptyLevel - error while creating and empty level: %s", e.getMessage()));
			level.disable();
			levelMap.remove(level.getChunkZ());
		}
	}

	public void createEmptyLevel(UUID authorId, int floorBlockId) {
		MakerPlayer author = getPlayer(authorId);
		if (author == null) {
			Bukkit.getLogger().warning(String.format("MakerController.createEmptyLevel - author must be online in order to create a level!"));
			return;
		}
		createEmptyLevel(author, floorBlockId);
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
		levelMap = new ConcurrentHashMap<>();
		globalTickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 0);
		enabled = true;
	}

	@Override
	public long getCurrentTick() {
		return currentTick;
	}

	public Location getDefaultSpawnLocation() {
		return spawnVector.toLocation(getMainWorld(), spawnYaw, spawnPitch);
	}

	private MakerLevel getEmptyLevelIfAvailable() {
		for (short i = 0; i < maxLevels; i++) {
			if (!levelMap.containsKey(i)) {
				MakerLevel level = new MakerLevel(plugin, i);
				levelMap.put(i, level);
				return level;
			}
		}
		return null;
	}

	public MakerLevel getLevel(short slotId) {
		return levelMap.get(slotId);
	}

	public World getMainWorld() {
		if (this.mainWorld == null) {
			this.mainWorld = MakerWorldUtils.createOrLoadWorld(this.plugin, this.mainWorldName, DEFAULT_SPAWN_VECTOR);
		}
		return this.mainWorld;
	}

	// FIXME: find a way to reuse the world data object that doesn't mess with multithreading.
	public WorldData getMainWorldData() {
		return BukkitUtil.toWorld(getMainWorld()).getWorldData();
	}

	public MakerExtent getMakerExtent() {
		if (this.makerExtent == null) {
			this.makerExtent = new MakerExtent(BukkitUtil.toWorld(getMainWorld()));
		}
		return this.makerExtent;
	}

	public MakerPlayer getPlayer(Player player) {
		return getPlayer(player.getUniqueId());
	}

	public MakerPlayer getPlayer(UUID playerId) {
		return playerMap.get(playerId);
	}

	public int getPlayerCount() {
		return playerMap.size();
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void loadLevel(UUID authorId, String levelName, short chunkZ) {
		File schematicsFolder = new File(plugin.getDataFolder(), "test");
		// if the directory does not exist, create it
		if (!schematicsFolder.exists()) {
			try {
				schematicsFolder.mkdir();
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("MakerController.loadLevel - unable to create test folder for schematics: %s", e.getMessage()));
				e.printStackTrace();
				return;
			}
		}

		File f;
		try {
			f = FileUtils.getSafeFile(schematicsFolder, levelName, "schematic", "schematic");
		} catch (FilenameException e) {
			// TODO notify player/sender
			Bukkit.getLogger().severe(String.format("MakerController.loadLevel - schematic not found"));
			e.printStackTrace();
			return;
		}

		if (!f.exists()) {
			// TODO notify player/sender
			Bukkit.getLogger().severe(String.format("MakerController.loadLevel - schematic not found"));
			return;
		}

		ClipboardFormat format = ClipboardFormat.SCHEMATIC;

		try (FileInputStream fis = new FileInputStream(f); BufferedInputStream bis = new BufferedInputStream(fis)) {

			ClipboardReader reader = format.getReader(bis);

			WorldData worldData = BukkitUtil.toWorld(getMainWorld()).getWorldData();
			Clipboard clipboard = reader.read(worldData);
			BlockTransformExtent extent = new BlockTransformExtent(clipboard, LevelUtils.IDENTITY_TRANSFORM, worldData.getBlockRegistry());
			ResumableForwardExtentCopy copy = new ResumableForwardExtentCopy(extent, clipboard.getRegion(), clipboard.getOrigin(), getMakerExtent(), LevelUtils.getLevelOrigin(chunkZ));
			copy.setTransform(LevelUtils.IDENTITY_TRANSFORM);
			boolean ignoreAirBlocks = false;
			if (ignoreAirBlocks) {
				copy.setSourceMask(new ExistingBlockMask(clipboard));
			}
			plugin.getLevelOperatorTask().offer(copy);
		} catch (IOException e) {
			Bukkit.getLogger().severe(String.format("MakerController.loadLevel - unable to load level: %s", e.getMessage()));
			e.printStackTrace();
			return;
		}
	}

	public void loadLevelForPlayingBySerial(MakerPlayer mPlayer, Long levelSerial) {
		MakerLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage(plugin, "level.error.full");
			return;
		}
		level.setLevelSerial(levelSerial);
		level.setCurrentPlayerId(mPlayer.getUniqueId());
		plugin.getDatabaseAdapter().loadLevelBySerialFullAsync(level);
		mPlayer.sendActionMessage(plugin, "level.loading");
	}

	public void onAsyncAccountDataLoad(MakerPlayerData data) {
		if (playerMap.containsKey(data.getUniqueId())) {
			Bukkit.getLogger().warning(String.format("MakerController.onAccountDataLoaded - Player is already registered on the server with valid data: [%s]", data.getUsername()));
			return;
		}
		// switch to main thread
		Bukkit.getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(data.getUniqueId());
				if (player == null) {
					Bukkit.getLogger().warning(String.format("MakerController.onAccountDataLoaded - Player is not online: [%s]", data.getUsername()));
					// TODO: discard data?
					return;
				}
				Bukkit.getLogger().warning(String.format("MakerController.onAccountDataLoaded - adding player to the server: [%s]", data.getUsername()));
				addMakerPlayer(player, data);
			}
		});
	}

	public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		// allow vanilla white-list logic to work
		if (Result.KICK_WHITELIST.equals(event.getLoginResult())) {
			return;
		}

		// login hacks
		controlDoubleLoginHack(event);
		controlFastReloginHack(event);
		if (Result.KICK_OTHER.equals(event.getLoginResult())) {
			return;
		}

		// this code is not run by the main thread
		final MakerPlayerData data = plugin.getDatabaseAdapter().loadAccountData(event.getUniqueId(), event.getName());
		if (null == data) {
			event.disallow(Result.KICK_OTHER, plugin.getMessage("server.error.missing-data"));
			return;
		}

		// allow vips to join full servers
		if (Result.KICK_FULL.equals(event.getLoginResult())) {
			if (data.hasRank(Rank.VIP) && getPlayerCount() < Bukkit.getMaxPlayers() + 20) {
				event.allow();
			} else {
				event.setKickMessage(plugin.getMessage("server.error.max-player-capacity"));
				return;
			}
		}
		accountDataMap.put(event.getUniqueId(), data);
		Bukkit.getPluginManager().callEvent(new AsyncAccountDataLoadEvent(data));
	}

	public void onBlockBreak(BlockBreakEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerController.onBlockBreak - player: [%s] - block type: [%s] ", event.getPlayer().getName(), event.getBlock().getType()));
		}
		final MakerPlayer mPlayer = getPlayer(event.getPlayer());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onBlockBreak - untracked player:[%s]", event.getPlayer().getName()));
			event.setCancelled(true);
			return;
		}
		if (!mPlayer.isEditingLevel()) {
			event.setCancelled(true);
			return;
		}
		if (LevelUtils.isBeaconPowerBlock(event.getBlock())) {
			mPlayer.sendActionMessage(plugin, "level.edit.error.bacon-power-block");
			mPlayer.sendMessage(plugin, "level.edit.clear-bacon");
			event.setCancelled(true);
			return;
		}
	}

	public void onBlockPlace(BlockPlaceEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerController.onBlockPlace - player: [%s] - block type: [%s] ", event.getPlayer().getName(), event.getBlockPlaced().getType()));
		}
		final MakerPlayer mPlayer = getPlayer(event.getPlayer());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onBlockPlace - untracked player:[%s]", event.getPlayer().getName()));
			event.setCancelled(true);
			return;
		}
		if (!mPlayer.isEditingLevel()) {
			event.setCancelled(true);
			return;
		}
		// end level beacon placement
		if (Material.BEACON.equals(event.getBlockPlaced().getType())) {
			if(!mPlayer.getCurrentLevel().setupEndLocation(event.getBlockPlaced().getLocation())) {
				event.setCancelled(true);
			}
			return;
		}
	}

	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player damagedPlayer = (Player) event.getEntity();

		final MakerPlayer mPlayer = getPlayer(damagedPlayer);
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onEntityDamage - Untracked player getting damage: [%s] - cause: [%s]", damagedPlayer.getName(), event.getCause()));
			damagedPlayer.teleport(getDefaultSpawnLocation(), TeleportCause.PLUGIN);
			event.setCancelled(true);
			return;
		}

		// level creator back to spawn from void
		if (mPlayer.isEditingLevel()) {
			if (event.getCause() == DamageCause.VOID) {
				mPlayer.teleportOnNextTick(mPlayer.getCurrentLevel().getStartLocation());
			}
			event.setCancelled(true);
			return;
		}
		if (mPlayer.isPlayingLevel()) {
			if (event.getCause() == DamageCause.VOID) {
				Bukkit.getScheduler().runTask(plugin, () -> mPlayer.getCurrentLevel().restartPlaying());
				event.setCancelled(true);
				return;
			}
			return;
		}
		if (mPlayer.isInLobby()) {
			event.setCancelled(true);
		}
	}

	public void onInventoryClick(InventoryClickEvent event) {
		Player bukkitPlayer = (Player) event.getWhoClicked();
		final MakerPlayer mPlayer = getPlayer(bukkitPlayer);
		if (mPlayer == null) {
			event.setCancelled(true);
			return;
		}
		// FIXME: or is not in lobby?
//		if (mPlayer.isEditingLevel()) {
//			return;
//		}
		// cancel inventory right click entirely
		if (event.isRightClick()) {
			event.setCancelled(true);
			mPlayer.updateInventoryOnNextTick();
			return;
		}
		// we are only interested on clicks on container type slots for custom menus and inventories
		if (event.getSlotType() == SlotType.CONTAINER) {
			final ItemStack clicked = event.getCurrentItem();
			if (clicked != null && clicked.getType() != Material.AIR) {
				if (mPlayer.onInventoryClick(event.getInventory(), event.getRawSlot())) {
					event.setCancelled(true);
					mPlayer.closeInventory();
				}
			}
			return;
		} else if (event.getSlotType() == SlotType.QUICKBAR) {
			if (onMenuItemClick(mPlayer, event.getCurrentItem())) {
				event.setCancelled(true);
				return;
			}
		}
	}

	private boolean onMenuItemClick(MakerPlayer mPlayer, ItemStack item) {
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return false;
		}
		if (ItemUtils.itemNameEquals(item, MakerLobbyItem.SERVER_BROWSER.getDisplayName())) {
			mPlayer.openServerBrowserMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.STEVE_CHALLENGE.getDisplayName())) {
			mPlayer.sendActionMessage(plugin, "general.coming-soon");
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.CREATE_LEVEL.getDisplayName())) {
			mPlayer.updateInventoryOnNextTick();
			mPlayer.openLevelTemplateMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.LEVEL_BROWSER.getDisplayName())) {
			mPlayer.updateInventoryOnNextTick();
			mPlayer.openLevelBrowserMenu(plugin);
			return true;
		} else if (ItemUtils.itemNameEquals(item, GeneralMenuItem.EDIT_LEVEL_OPTIONS.getDisplayName())) {
			mPlayer.updateInventoryOnNextTick();
			mPlayer.openEditLevelOptionsMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getDisplayName())) {
			mPlayer.updateInventoryOnNextTick();
			mPlayer.openPlayLevelOptionsMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.QUIT.getDisplayName())) {
			BungeeUtils.switchServer(plugin, mPlayer.getPlayer(), "l1", plugin.getMessage("server.quit.connecting", "Lobby1"));
			return true;
		}
		return false;
	}

	public void onPlayerDeath(PlayerDeathEvent event) {
		event.setDeathMessage("");
		event.getDrops().clear();
		event.setDroppedExp(0);
	}

	public void onPlayerInteract(PlayerInteractEvent event) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | MakerController.onPlayerInteract - Player:[%s]", event.getPlayer().getName()));
		}
		final MakerPlayer mPlayer = getPlayer(event.getPlayer());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerInteract - untracked Player:[%s]", event.getPlayer().getName()));
			event.setCancelled(true);
			return;
		}
		// TODO: check if we should also allow left clicks
		if (EventUtils.isItemRightClick(event)) {
			if (onMenuItemClick(mPlayer, event.getItem())) {
				event.setCancelled(true);
				return;
			}
		}
	}

	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		final MakerPlayer mPlayer = getPlayer(event.getPlayer());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerInteractEntity - untracked Player: [%s]", event.getPlayer().getName()));
			event.setCancelled(true);
			return;
		}
		if (mPlayer.isInLobby()) {
			event.setCancelled(true);
			return;
		}
	}

	public void onPlayerJoin(Player player) {
		Bukkit.getLogger().info(String.format("MakerController.onPlayerJoin - Player: [%s<%s>]", player.getName(), player.getUniqueId()));
		MakerPlayerData data = accountDataMap.remove(player.getUniqueId());
		if (null == data && !playerMap.containsKey(player.getUniqueId())) {
			Bukkit.getLogger().info(String.format("MakerController.onPlayerJoin - No data available yet for Player: [%s<%s>]", player.getName(), player.getUniqueId()));
			// avoid double login hack
			new BukkitRunnable() {
				@Override
				public void run() {
					if (player.isOnline() && !playerMap.containsKey(player.getUniqueId())) {
						Bukkit.getLogger().warning(String.format("[POSSIBLE-HACK] | MakerController.onPlayerJoin - possible double login for player: [%s<%s>]", player.getName(), player.getUniqueId()));
						player.kickPlayer(plugin.getMessage("server.error.double-login"));
					}
				}
			}.runTaskLater(plugin, DOUBLE_LOGIN_DELAY_SECONDS * 20);
			return;
		}
		if (!playerMap.containsKey(data.getUniqueId())) {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerJoin - adding player to the server: [%s]", data.getUsername()));
			addMakerPlayer(player, data);
		}
	}

	public void onPlayerMove(PlayerMoveEvent event) {
		final MakerPlayer mPlayer = getPlayer(event.getPlayer());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerMove - untracked Player: [%s]", event.getPlayer().getName()));
			return;
		}
		if (!mPlayer.isPlayingLevel() || LevelStatus.CLEARED.equals(mPlayer.getCurrentLevel().getStatus())) {
			return;
		}
		mPlayer.getCurrentLevel().checkLevelEnd(event.getTo());
	}

	public void onPlayerQuit(Player player) {
		// wait a bit before coming back
		nextAllowedLogins.put(player.getUniqueId(), System.currentTimeMillis() + (FAST_RELOGIN_DELAY_SECONDS * 1000));
		// remove from map
		MakerPlayer mPlayer = playerMap.remove(player.getUniqueId());
		if (mPlayer != null) {
			// TODO: destroy player custom stuff
			// TODO: cancel pending level loading tasks
			mPlayer.cancelPendingOperation();
		} else {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerQuit - Player: [%s] was already removed", player.getName()));
		}
		// TODO: notify rabbit that player left
		// plugin.publishServerInfoAsync();
		

		
//		// FIXME: experimental
//		entriesToRemoveFromScoreboardTeamsOnNextTick.add(quitter.getName());
//
//		final SCBPlayer gPlayer = getPlayer(quitter);
//
//		if (gPlayer != null) {
//			// destroy stuff
//			if (gPlayer.getCurrentGame() != null) {
//				gPlayer.getCurrentGame().onPlayerQuit(gPlayer);
//			}
//			if (gPlayer.getSpectatingGame() != null) {
//				gPlayer.getSpectatingGame().onSpectatorQuit(gPlayer);
//			}
//			if (quitter.getMaxHealth() != 20) {
//				quitter.setMaxHealth(20);
//			}
//
//			// remove customized scoreboard
//			gPlayer.removeLobbyScoreboard();
//
//			// remove customized inventories
//			gPlayer.removePersonalInventories();
//
//			// remove from the player map
//			playerMap.remove(gPlayer.getPlayer().getUniqueId());	
//			// notify a player left
//			plugin.publishServerInfoAsync();
//		} else {
//			Bukkit.getLogger().warning(String.format("SCBController.onPlayerQuit - Player: [%s] was already removed", quitter.getName()));
//		}
	}

	public void renameLevel(Player player, String newName) {
		final MakerPlayer mPlayer = getPlayer(player);
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.renameLevel - untracked Player: [%s]", player.getName()));
			player.sendMessage(plugin.getMessage("level.rename.error"));
			return;
		}
		// needs to be editing that level
		if (!mPlayer.isEditingLevel()) {
			player.sendMessage(plugin.getMessage("level.rename.error.no-editing"));
			return;
		}
		// rename
		mPlayer.getCurrentLevel().rename(newName);
	}

	@Override
	public void run() {
		tick(getCurrentTick() + 1);
	}

	public void saveLevel(UUID authorId, String levelName, short chunkZ) {
		File schematicsFolder = new File(plugin.getDataFolder(), "test");
		// if the directory does not exist, create it
		if (!schematicsFolder.exists()) {
			try {
				schematicsFolder.mkdir();
			} catch (Exception e) {
				Bukkit.getLogger().severe(String.format("MakerController.loadLevel - unable to create test folder for schematics: %s", e.getMessage()));
				e.printStackTrace();
				return;
			}
		}

		File f;
		try {
			f = FileUtils.getSafeFile(schematicsFolder, levelName, "schematic", "schematic");
		} catch (FilenameException e) {
			// TODO notify player/sender
			Bukkit.getLogger().severe(String.format("MakerController.loadLevel - schematic not found"));
			e.printStackTrace();
			return;
		}

		Region levelRegion = LevelUtils.getLevelRegion(getMainWorld(), chunkZ);
		BlockArrayClipboard clipboard = new BlockArrayClipboard(levelRegion);
		clipboard.setOrigin(levelRegion.getMinimumPoint());
		ResumableForwardExtentCopy copy = new ResumableForwardExtentCopy(getMakerExtent(), levelRegion, clipboard, clipboard.getOrigin());
		plugin.getLevelOperatorTask().offer(new ResumableOperationQueue(copy, new SchematicWriteOperation(clipboard, getMainWorldData(), f)));
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
		if (this.currentTick == 1) {
			MakerWorldUtils.removeAllLivingEntitiesExceptPlayers(this.getMainWorld());
			try {
				plugin.getLevelOperatorTask().offer(LevelUtils.createPasteOperation(LevelUtils.createLobbyClipboard(this.getMainWorld()), getMakerExtent(), getMainWorldData()));
			} catch (MinecraftMakerException e) {
				e.printStackTrace();
			}
			return;
		}
		// tick levels
		for (MakerLevel level : new ArrayList<MakerLevel>(levelMap.values())) {
			if (level != null) {
				TickableUtils.tickSafely(level, currentTick);
			}
		}
		// tick players
		for (MakerPlayer mPlayer : new ArrayList<MakerPlayer>(playerMap.values())) {
			if (mPlayer != null) {
				TickableUtils.tickSafely(mPlayer, currentTick);
			}
		}
	}

	public void unloadLevel(MakerLevel makerLevel) {
		// FIXME: review this
		makerLevel.disable();
		levelMap.remove(makerLevel.getChunkZ());
	}

}
