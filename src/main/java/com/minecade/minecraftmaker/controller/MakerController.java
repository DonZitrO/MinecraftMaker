package com.minecade.minecraftmaker.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleMoveEvent;
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
import com.minecade.minecraftmaker.data.MakerSteveData;
import com.minecade.minecraftmaker.function.mask.ExistingBlockMask;
import com.minecade.minecraftmaker.function.operation.ResumableForwardExtentCopy;
import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.inventory.LevelPageUpdateCallback;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.MakerLobbyItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.LevelStatus;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.bukkit.BukkitUtil;
import com.minecade.minecraftmaker.schematic.exception.FilenameException;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.io.ClipboardFormat;
import com.minecade.minecraftmaker.schematic.io.ClipboardReader;
import com.minecade.minecraftmaker.schematic.world.BlockTransformExtent;
import com.minecade.minecraftmaker.schematic.world.MakerExtent;
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
	private static final int MIN_STEVE_LEVELS = 16;

	private static final Vector DEFAULT_SPAWN_VECTOR = new Vector(-15.0d, 45.0d, 80.0d);
	private static final float DEFAULT_SPAWN_YAW = 90.0f;
	private static final float DEFAULT_SPAWN_PITCH = -15.0f;

	private final MinecraftMakerPlugin plugin;

	private BukkitTask globalTickerTask;
	private String mainWorldName;
	private World mainWorld;
	private MakerExtent makerExtent;

	private Vector spawnVector;
	private float spawnYaw;
	private float spawnPitch;

	private long currentTick;
	private boolean disabled;
	private boolean initialized;
	private int maxPlayers;
	private short maxLevels;

	// keeps track of every player on the server
	private Map<UUID, MakerPlayer> playerMap;
	// keeps track of every arena on the server
	private Map<Short, MakerPlayableLevel> levelMap;
	// steve level serials
	private final Set<Long> steveLevelSerials = new HashSet<>();
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
		mPlayer.getPlayer().sendMessage(new String[]{
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.new-line"),
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.new-line"),
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.new-line"),
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.new-line"),
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.new-line"),
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.welcome1"),
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.welcome2"),
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.welcome3"),
				this.plugin.getMessage("player.welcome4"), this.plugin.getMessage("player.welcome5"),
				this.plugin.getMessage("player.welcome6"), this.plugin.getMessage("player.welcome7"),
				this.plugin.getMessage("player.new-line"), this.plugin.getMessage("player.welcome8")});
	}

	public void addPlayerToMainLobby(MakerPlayer mPlayer) {
		// reset player
		mPlayer.resetPlayer();
		// teleport to spawn point
		if (!mPlayer.getPlayer().getLocation().getWorld().equals(getMainWorld()) || mPlayer.getPlayer().getLocation().distanceSquared(getDefaultSpawnLocation()) > 4d) {
			if (!mPlayer.getPlayer().teleport(getDefaultSpawnLocation(), TeleportCause.PLUGIN)) {
				mPlayer.getPlayer().kickPlayer(plugin.getMessage("lobby.join.error.teleport"));
			}
		}
		// steve challenge data
		mPlayer.setSteveData(null);
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
		if (!author.isInLobby()) {
			author.sendActionMessage(plugin, "level.create.error.author-busy");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			author.sendActionMessage(plugin, "level.error.full");
			return;
		}
		level.setLevelId(UUID.randomUUID());
		level.setAuthorId(author.getUniqueId());
		level.setAuthorName(author.getName());
		level.setAuthorRank(author.getData().getHighestRank());
		level.setupStartLocation();
		level.waitForBusyLevel(author, true);
		try {
			level.setClipboard(LevelUtils.createEmptyLevelClipboard(level.getChunkZ(), floorBlockId));
			level.tryStatusTransition(LevelStatus.BLANK, LevelStatus.CLIPBOARD_LOADED);
		} catch (Exception e) {
			Bukkit.getLogger().severe(String.format("MakerController.createEmptyLevel - error while creating and empty level: %s", e.getMessage()));
			e.printStackTrace();
			level.disable(e.getMessage(), e);
		}
	}

	public void createEmptyLevel(UUID authorId, short widthChunks, int floorBlockId) {
		MakerPlayer author = getPlayer(authorId);
		if (author == null) {
			Bukkit.getLogger().warning(String.format("MakerController.createEmptyLevel - author must be online in order to create a level!"));
			return;
		}
		createEmptyLevel(author, floorBlockId);
	}

	@Override
	public void disable(String reason, Exception exception) {
		if (isDisabled()) {
			return;
		}
		if (globalTickerTask != null) {
			globalTickerTask.cancel();
		}
		disabled = true;
	}

	@Override
	public long getCurrentTick() {
		return currentTick;
	}

	public Location getDefaultSpawnLocation() {
		return spawnVector.toLocation(getMainWorld(), spawnYaw, spawnPitch);
	}

	private MakerPlayableLevel getEmptyLevelIfAvailable() {
		for (short i = 0; i < maxLevels; i++) {
			if (!levelMap.containsKey(i)) {
				MakerPlayableLevel level = new MakerPlayableLevel(plugin, i);
				levelMap.put(i, level);
				return level;
			}
		}
		return null;
	}

	public MakerPlayableLevel getLevel(short slotId) {
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

	// intentionally copy this to avoid tampering with the original set
	private Set<Long> getSteveLevels() {
		return new HashSet<Long>(steveLevelSerials);
	}

	public void init() {
		if (initialized) {
			throw new IllegalStateException("This controller is already initialized");
		}
		playerMap =  Collections.synchronizedMap(new LinkedHashMap<UUID, MakerPlayer>(maxPlayers * 4, .75f, true) {
			private static final long serialVersionUID = 1L;
			protected boolean removeEldestEntry(Map.Entry<UUID, MakerPlayer> eldest) {
				return size() > (maxPlayers * 2);
			}
		});
		levelMap = new ConcurrentHashMap<>();
		Bukkit.getScheduler().runTask(plugin, () -> MakerWorldUtils.removeAllLivingEntitiesExceptPlayers(this.getMainWorld()));
		globalTickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 0);
		initialized = true;
	}

	@Override
	public boolean isDisabled() {
		return disabled;
	}

	public void levelLikeCallback(UUID levelId, UUID playerId, boolean dislike, long totalLikes, long totalDislikes) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		// update level browser
		LevelBrowserMenu.updateLevelLikes(plugin, levelId, totalLikes, totalDislikes);
		// update current user level
		MakerPlayer mPlayer = getPlayer(playerId);
		if (mPlayer == null) {
			return;
		}
		mPlayer.sendActionMessage(plugin, dislike ? "level.dislike.success" : "level.like.success");
		MakerPlayableLevel level = mPlayer.getCurrentLevel();
		if (level == null) {
			return;
		}
		level.setLikes(totalDislikes);
		level.setDislikes(totalDislikes);
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

	public void loadLevelForEditingBySerial(MakerPlayer mPlayer, Long levelSerial) {
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage(plugin, "level.play.error.author-busy");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage(plugin, "level.error.full");
			return;
		}
		level.setLevelSerial(levelSerial);
		level.setupStartLocation();
		level.waitForBusyLevel(mPlayer, true);
		plugin.getDatabaseAdapter().loadPlayableLevelBySerialAsync(level);
	}

	public void loadLevelForPlayingBySerial(MakerPlayer mPlayer, Long levelSerial) {
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage(plugin, "level.play.error.player-busy");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage(plugin, "level.error.full");
			return;
		}
		level.setLevelSerial(levelSerial);
		level.setupStartLocation();
		level.waitForBusyLevel(mPlayer, true);
		level.setCurrentPlayerId(mPlayer.getUniqueId());
		plugin.getDatabaseAdapter().loadPlayableLevelBySerialAsync(level);
	}

	public void loadPublishedLevelCallback(MakerDisplayableLevel level, int levelCount) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		LevelBrowserMenu.updateLevelCount(levelCount);
		steveLevelSerials.add(level.getLevelSerial());
		LevelBrowserMenu.addOrUpdateLevel(plugin, level);
	}

	public void loadPublishedLevelsCallback(List<MakerDisplayableLevel> levels, int levelCount, UUID playerId) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		LevelBrowserMenu.updateLevelCount(levelCount);
		for (MakerDisplayableLevel level : levels) {
			steveLevelSerials.add(level.getLevelSerial());
			LevelBrowserMenu.addOrUpdateLevel(plugin, level);
		}
		if (playerId!= null) {
			LevelBrowserMenu.updatePlayerMenu(playerId);
		}
	}

	public void loadPublishedLevelsCountCallback(int levelCount) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		LevelBrowserMenu.updateLevelCount(levelCount);
		plugin.getAsyncLevelBrowserUpdater().resetCompleted();
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

	public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
		MakerPlayer makerPlayer = getPlayer(event.getPlayer().getUniqueId());
		if (makerPlayer != null) {
			event.setFormat(String.format("%%s%s: %%s", StringUtils.EMPTY, ChatColor.GRAY, ChatColor.DARK_GRAY));
		} else {
			event.setCancelled(true);
		}
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

		// TODO: Beta - remove after beta
		if (!data.hasRank(Rank.PRO)) {
			event.disallow(Result.KICK_OTHER, plugin.getMessage("server.login.error.pro-only"));
			return;
		}
		
		// allow vips to join full servers TODO: Beta - uncomment after beta
//		if (Result.KICK_FULL.equals(event.getLoginResult())) {
//			if (data.hasRank(Rank.VIP) && getPlayerCount() < Bukkit.getMaxPlayers() + 20) {
//				event.allow();
//			} else {
//				event.setKickMessage(plugin.getMessage("server.error.max-player-capacity"));
//				return;
//			}
//		}
		if (!Result.ALLOWED.equals(event.getLoginResult())) {
			return;
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
		if (event.getBlock().getType().equals(Material.BEACON)) {
			if (event.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.IRON_BLOCK)) {
				mPlayer.sendMessage(plugin, "level.edit.clear-bacon");
			} else {
				mPlayer.sendActionMessage(plugin, "level.edit.error.start-bacon");
			}
			event.setCancelled(true);
			return;
		}
		if (LevelUtils.isBeaconPowerBlock(event.getBlock())) {
			mPlayer.sendActionMessage(plugin, "level.edit.error.bacon-power-block");
			event.setCancelled(true);
			return;
		}
	}

	public void onBlockFromTo(BlockFromToEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getBlock().getLocation());
		if (slot < 0) {
			// this should allow water to flow in lobby
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onBlockFromTo - cancelled liquid block flowing on unregistered level slot - from: [%s] - to: [%s] - location: [%s]", event.getBlock().getType(), event.getToBlock().getType(), event.getToBlock().getLocation().toVector()));
			return;
		}
		level.onBlockFromTo(event);
	}

	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getBlock().getLocation());
		if (slot < 0) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onBlockPistonExtend - cancelled piston push outside level - location: [%s]", event.getBlock().getLocation().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onBlockPistonExtend - cancelled piston push on unregistered level slot - location: [%s]", event.getBlock().getLocation().toVector()));
			return;
		}
		level.onBlockPistonExtend(event);
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
		switch (event.getBlockPlaced().getType()) {
		case BEACON:
			if (!LevelUtils.isValidEndBeaconLocation(event.getBlockPlaced().getLocation().toVector(), mPlayer.getCurrentLevel().getLevelRegion())) {
				event.setCancelled(true);
				mPlayer.sendActionMessage(plugin, "level.create.error.end-beacon-too-close-to-border");
				return;
			}
			Bukkit.getScheduler().runTask(plugin, () -> mPlayer.getCurrentLevel().setupEndLocation(event.getBlockPlaced().getLocation()));
			return;
		// place for disabled building blocks
		case BARRIER:
		case ENDER_CHEST:
			event.setCancelled(true);
			mPlayer.sendActionMessage(plugin, "level.create.error.disabled-block");
			return;
		default:
			if (mPlayer.getCurrentLevel().getRelativeEndLocation() != null && LevelUtils.isAboveLocation(event.getBlockPlaced().getLocation().toVector(), mPlayer.getCurrentLevel().getEndLocation().toVector())) {
				event.setCancelled(true);
				mPlayer.sendActionMessage(plugin, "level.create.error.block-place-above-end-beacon");
				return;
			}
			break;
		}
	}

	public void onBlockRedstone(BlockRedstoneEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getBlock().getLocation());
		if (slot < 0) {
			event.setNewCurrent(event.getOldCurrent());
			Bukkit.getLogger().warning(String.format("MakerController.onBlockRedstone - cancelled redstone change outside level - block type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setNewCurrent(event.getOldCurrent());
			Bukkit.getLogger().warning(String.format("MakerController.onBlockRedstone - cancelled redstone change from unregistered level slot - block type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			return;
		}
		level.onBlockRedstone(event);
	}

	public void onCreatureDamageByEntity(EntityDamageByEntityEvent event) {
		if (!event.getCause().equals(DamageCause.ENTITY_ATTACK) || !(event.getDamager() instanceof Player)) {
			return;
		}
		MakerPlayer mPlayer = getPlayer((Player) event.getDamager());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onCreatureDamageByEntity - Untracked player damaging entities damager: [%s] - cause: [%s]", event.getDamager().getName(), event.getCause()));
			event.setCancelled(true);
			return;
		}
		if (mPlayer.isEditingLevel() && event.getEntity() instanceof LivingEntity) {
			event.getEntity().remove();
			event.setCancelled(true);
			return;
		}
	}

	public void onCreatureSpawn(CreatureSpawnEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getLocation());
		if (slot < 0) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onCreatureSpawn - cancelled creature spawning outside level - creature type: [%s] - location: [%s]", event.getEntityType(), event.getLocation().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onCreatureSpawn - cancelled creature spawning on unregistered level slot - creature type: [%s] - location: [%s]", event.getEntityType(), event.getLocation().toVector()));
			return;
		}
		level.onCreatureSpawn(event);
	}

	public void onEntityExplode(EntityExplodeEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getLocation());
		if (slot < 0) {
			event.setCancelled(true);
			event.blockList().clear();
			Bukkit.getLogger().warning(String.format("MakerController.onEntityExplode - cancelled entity explosion outside level - type: [%s] - location: [%s]", event.getEntityType(), event.getLocation().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			event.blockList().clear();
			Bukkit.getLogger().warning(String.format("MakerController.onEntityExplode - cancelled entity explosion on unregistered slot - creature type: [%s] - location: [%s]", event.getEntityType(), event.getLocation().toVector()));
			return;
		}
		level.onEntityExplode(event);
	}

	public void onEntityTeleport(EntityTeleportEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getFrom());
		if (slot < 0) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onEntityTeleport - cancelled creature teleporting from outside level - creature type: [%s] - from: [%s]", event.getEntityType(), event.getFrom().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onEntityTeleport - cancelled creature teleporting from unregistered level slot - creature type: [%s] - location: [%s]", event.getEntityType(), event.getFrom().toVector()));
			return;
		}
		level.onEntityTeleport(event);
	}

	public void onInventoryClick(InventoryClickEvent event) {
		Player bukkitPlayer = (Player) event.getWhoClicked();
		final MakerPlayer mPlayer = getPlayer(bukkitPlayer);
		if (mPlayer == null) {
			event.setCancelled(true);
			return;
		}
		// priority for quickbar menu items
		if (event.getSlotType() == SlotType.QUICKBAR && onMenuItemClick(mPlayer, event.getCurrentItem())) {
			event.setCancelled(true);
			return;
		}
		// specific options for editors
		if (mPlayer.isEditingLevel()) {
			// allow editors to interact with creative inventory
			if(event.getInventory().getName().equals("container.inventory")) {
				if (ItemUtils.itemNameEquals(event.getCurrentItem(), GeneralMenuItem.EDIT_LEVEL_OPTIONS.getDisplayName())) {
					event.setCancelled(true);
					mPlayer.updateInventory();
				}
				return;
			}
		}
		// cancel inventory right click entirely on the rest of scenarios
		if (event.isRightClick()) {
			event.setCancelled(true);
			mPlayer.updateInventory();
			return;
		}
		// we are only interested on clicks on container type slots for custom menus and inventories
		if (event.getSlotType() == SlotType.CONTAINER) {
			final ItemStack clicked = event.getCurrentItem();
			if (clicked != null && clicked.getType() != Material.AIR) {
				switch (mPlayer.onInventoryClick(event.getInventory(), event.getRawSlot())) {
				case CANCEL_CLOSE:
					event.setCancelled(true);
					mPlayer.closeInventory();
					break;
				case CANCEL_UPDATE:
					event.setCancelled(true);
					mPlayer.updateInventory();
					break;
				default:
					break;
				}
			}
			return;
		}
	}

	private boolean onMenuItemClick(MakerPlayer mPlayer, ItemStack item) {
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return false;
		}
		if (ItemUtils.itemNameEquals(item, MakerLobbyItem.SERVER_BROWSER.getDisplayName())) {
			//mPlayer.openServerBrowserMenu();
			mPlayer.sendActionMessage(plugin, "general.coming-soon");
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.STEVE_CHALLENGE.getDisplayName())) {
			startSteveChallenge(mPlayer);
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.CREATE_LEVEL.getDisplayName())) {
			mPlayer.updateInventory();
			mPlayer.openLevelTemplateMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.PLAYER_LEVELS.getDisplayName())) {
			mPlayer.updateInventory();
			mPlayer.openPlayerLevelsMenu(plugin, LevelSortBy.LIKES, true);
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.LEVEL_BROWSER.getDisplayName())) {
			mPlayer.updateInventory();
			mPlayer.openLevelBrowserMenu(plugin, LevelSortBy.LIKES, true);
			return true;
		} else if (ItemUtils.itemNameEquals(item, GeneralMenuItem.EDIT_LEVEL_OPTIONS.getDisplayName())) {
			mPlayer.updateInventory();
			mPlayer.openEditLevelOptionsMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getDisplayName())) {
			mPlayer.updateInventory();
			mPlayer.openPlayLevelOptionsMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, GeneralMenuItem.EDITOR_PLAY_LEVEL_OPTIONS.getDisplayName())) {
			mPlayer.updateInventory();
			mPlayer.openEditorPlayLevelOptionsMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, GeneralMenuItem.STEVE_LEVEL_OPTIONS.getDisplayName())) {
			mPlayer.updateInventory();
			mPlayer.openSteveLevelOptionsMenu();
			return true;
		} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.QUIT.getDisplayName())) {
			BungeeUtils.switchServer(plugin, mPlayer.getPlayer(), "l1", plugin.getMessage("server.quit.connecting", "Lobby1"));
			return true;
		}
		return false;
	}

	public void onPlayerDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}

		Player damagedPlayer = (Player) event.getEntity();

		MakerPlayer mPlayer = getPlayer(damagedPlayer);
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onEntityDamage - Untracked player getting damaged: [%s] - cause: [%s]", damagedPlayer.getName(), event.getCause()));
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
				mPlayer.getCurrentLevel().restartPlaying();
				event.setCancelled(true);
				return;
			}
			return;
		}
		if (mPlayer.hasClearedLevel()) {
			if (event.getCause() == DamageCause.VOID) {
				mPlayer.teleportOnNextTick(mPlayer.getCurrentLevel().getEndLocation().add(new Vector(0, 1, 0)));
			}
			event.setCancelled(true);
			return;
		}
		if (mPlayer.isInLobby()) {
			if (event.getCause() == DamageCause.VOID) {
				mPlayer.teleportOnNextTick(getDefaultSpawnLocation());
			}
			event.setCancelled(true);
			return;
		}
	}

	public void onPlayerDeath(PlayerDeathEvent event) {
		event.setDeathMessage("");
		event.getDrops().clear();
		event.setDroppedExp(0);
		event.getEntity().spigot().respawn();
	}

	public void onPlayerDropItem(PlayerDropItemEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getItemDrop().getLocation());
		if (slot < 0) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerDropItem - cancelled item drop outside level - location: [%s]", event.getItemDrop().getLocation().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerDropItem - cancelled item drop on unregistered level slot - location: [%s]", event.getItemDrop().getLocation().toVector()));
			return;
		}
		level.onPlayerDropItem(event);
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
		if (mPlayer.isInBusyLevel()) {
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
		// TODO: enhance this code and move it to level when needed
		if (mPlayer.isEditingLevel() && event.getRightClicked().getType().equals(EntityType.HORSE)) {
			Horse horse = (Horse) event.getRightClicked();
			if (!horse.isAdult()) {
				return;
			}
			if (horse.getPassenger() != null) {
				return;
			}
			if (horse.getInventory().getSaddle() != null) {
				return;
			}
			horse.setTamed(!horse.isTamed());
			if (horse.isTamed()) {
				mPlayer.sendMessage(plugin, "level.create.horse.tamed");
				return;
			} else {
				mPlayer.sendMessage(plugin, "level.create.horse.untamed");
				event.setCancelled(true);
			}
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
			event.setCancelled(true);
			return;
		}
		if (mPlayer.isInBusyLevel()) {
			// look but don't move
			if (event.getTo().getX() != event.getFrom().getX() || event.getTo().getY() != event.getFrom().getY() || event.getTo().getZ() != event.getFrom().getZ()) {
				mPlayer.sendActionMessage(plugin, "level.busy.look-dont-move");
				event.setCancelled(true);
			}
			return;
		}
		if (mPlayer.isPlayingLevel()) {
			mPlayer.getCurrentLevel().checkLevelBorder(event.getTo());
			mPlayer.getCurrentLevel().checkLevelEnd(event.getTo());
		}
	}

	public void onPlayerQuit(Player player) {
		// wait a bit before coming back
		nextAllowedLogins.put(player.getUniqueId(), System.currentTimeMillis() + (FAST_RELOGIN_DELAY_SECONDS * 1000));
		// remove from map
		MakerPlayer mPlayer = playerMap.remove(player.getUniqueId());
		if (mPlayer != null) {
			MakerPlayableLevel level = mPlayer.getCurrentLevel();
			if (level != null) {
				level.onPlayerQuit();
			}
			mPlayer.onQuit();
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

	public void onPlayerRespawn(PlayerRespawnEvent event) {
		final MakerPlayer mPlayer = getPlayer(event.getPlayer());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerRespawn - untracked player: [%s]", event.getPlayer().getName()));
			return;
		}
		if (mPlayer.isPlayingLevel()) {
			event.setRespawnLocation(mPlayer.getCurrentLevel().getStartLocation());
			mPlayer.getCurrentLevel().restartPlaying();
			return;
		}
		if (mPlayer.hasClearedLevel()) {
			event.setRespawnLocation(mPlayer.getCurrentLevel().getEndLocation());
			return;
		}
		if (mPlayer.isEditingLevel()) {
			event.setRespawnLocation(mPlayer.getCurrentLevel().getStartLocation());
			return;
		}
		event.setRespawnLocation(getDefaultSpawnLocation());
		Bukkit.getScheduler().runTask(plugin, () -> addPlayerToMainLobby(mPlayer));
	}

	public void onPlayerTeleport(PlayerTeleportEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getFrom());
		if (slot < 0) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerTeleport - cancelled player teleporting from outside level - player: [%s] - cause: [%s] - from: [%s]", event.getPlayer().getName(), event.getCause(), event.getFrom().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerTeleport - cancelled  player teleporting from unregistered level slot - player: [%s] - cause: [%s] - location: [%s]", event.getPlayer().getName(), event.getCause(), event.getFrom().toVector()));
			return;
		}
		level.onPlayerTeleport(event);
	}

	public void onVehicleMove(VehicleMoveEvent event) {
		Entity passenger = event.getVehicle().getPassenger();
		if (passenger == null || !(passenger instanceof Player)) {
			return;
		}
		final MakerPlayer mPlayer = getPlayer((Player)passenger);
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onVehicleMove - untracked player riding vehicle: [%s]", passenger.getName()));
			return;
		}
		if (mPlayer.isPlayingLevel()) {
			mPlayer.getCurrentLevel().checkLevelBorder(event.getTo());
			mPlayer.getCurrentLevel().checkLevelEnd(event.getTo());
		}
	}

//	public void saveLevel(UUID authorId, String levelName, short chunkZ) {
//		File schematicsFolder = new File(plugin.getDataFolder(), "test");
//		// if the directory does not exist, create it
//		if (!schematicsFolder.exists()) {
//			try {
//				schematicsFolder.mkdir();
//			} catch (Exception e) {
//				Bukkit.getLogger().severe(String.format("MakerController.loadLevel - unable to create test folder for schematics: %s", e.getMessage()));
//				e.printStackTrace();
//				return;
//			}
//		}
//
//		File f;
//		try {
//			f = FileUtils.getSafeFile(schematicsFolder, levelName, "schematic", "schematic");
//		} catch (FilenameException e) {
//			// TODO notify player/sender
//			Bukkit.getLogger().severe(String.format("MakerController.loadLevel - schematic not found"));
//			e.printStackTrace();
//			return;
//		}
//
//		Region levelRegion = LevelUtils.getDefaultLevelRegion(chunkZ);
//		BlockArrayClipboard clipboard = new BlockArrayClipboard(levelRegion);
//		clipboard.setOrigin(levelRegion.getMinimumPoint());
//		ResumableForwardExtentCopy copy = new ResumableForwardExtentCopy(getMakerExtent(), levelRegion, clipboard, clipboard.getOrigin());
//		plugin.getLevelOperatorTask().offer(new ResumableOperationQueue(copy, new SchematicWriteOperation(clipboard, getMainWorldData(), f)));
//	}

	private void refreshPublishedLevelsCount() {
		plugin.getDatabaseAdapter().loadPublishedLevelsCountAsync();
	}

	public void removeLevelFromSlot(MakerPlayableLevel makerLevel) {
		Bukkit.getLogger().warning(String.format("MakerController.removeLevelFromSlot - removing level: [%s<%s>] from slot: [%s]", makerLevel.getLevelName(), makerLevel.getLevelId(), makerLevel.getChunkZ()));
		levelMap.remove(makerLevel.getChunkZ());
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
			mPlayer.sendActionMessage(plugin, "level.rename.error.no-editing");
			return;
		}
		if (newName.equals(mPlayer.getCurrentLevel().getLevelName())) {
			mPlayer.sendActionMessage(plugin, "level.rename.error.different-name");
			return;
		}
		// rename
		mPlayer.getCurrentLevel().rename(newName);
	}

	@Override
	public void run() {
		tick(getCurrentTick() + 1);
	}

	public void sendActionMessageToPlayerIfPresent(UUID playerId, String key, Object... args) {
		MakerPlayer mPlayer = getPlayer(playerId);
		if (mPlayer != null) {
			mPlayer.sendActionMessage(plugin, key, args);
		}
	}

	public void startSteveChallenge(MakerPlayer mPlayer) {
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage(plugin, "level.play.error.player-busy");
			return;
		}
		Set<Long> levels = getSteveLevels();
		if (levels.isEmpty() || levels.size() < MIN_STEVE_LEVELS) {
			mPlayer.sendActionMessage(plugin, "steve.error.few-levels");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage(plugin, "level.error.full");
			return;
		}
		level.setupStartLocation();
		level.waitForBusyLevel(mPlayer, false);
		level.setCurrentPlayerId(mPlayer.getUniqueId());
		MakerSteveData steveData = new MakerSteveData(levels);
		level.setSteveData(steveData);
		mPlayer.setSteveData(steveData);
		level.setLevelSerial(steveData.getRandomLevel());
		plugin.getDatabaseAdapter().loadPlayableLevelBySerialAsync(level);
		mPlayer.sendTitleAndSubtitle(plugin.getMessage("steve.start.title"), plugin.getMessage("steve.start.subtitle"));
	}

	@Override
	public void tick(long currentTick) {
		this.currentTick = currentTick;
		// tick levels
		for (MakerPlayableLevel level : new ArrayList<MakerPlayableLevel>(levelMap.values())) {
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
		if (this.currentTick % 1200 == 600) {
			refreshPublishedLevelsCount();
		}
	}

	public void levelPageUpdateCallback(LevelPageUpdateCallback callback) {
		if (!Bukkit.isPrimaryThread()) {
			throw new RuntimeException("This method is meant to be called from the main thread ONLY");
		}
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().warning(String.format("[DEBUG] | MakerController.levelPageUpdateCallback - callback: [%s]", callback));
		}
		if (callback.getLevels() != null) {
			for (MakerDisplayableLevel level: callback.getLevels()) {
				steveLevelSerials.add(level.getLevelSerial());
				LevelBrowserMenu.addOrUpdateLevel(plugin, level);
			}
		}
		for (UUID playerId :callback.getPlayers()) {
			LevelBrowserMenu.updatePlayerMenu(playerId);
		}
	}

	public void requestLevelPageUpdate(LevelSortBy sortBy, boolean reverseSortBy, int currentPage, UUID playerId) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().warning(String.format("[DEBUG] | MakerController.requestLevelPageUpdate - sortBy: [%s] - reverse: [%s] - page: [%s] - playerId: [%s]", sortBy, reverseSortBy, currentPage, playerId));
		}
		plugin.getAsyncLevelBrowserUpdater().requestLevelPageUpdate(sortBy, reverseSortBy, currentPage, playerId);
	}

}
