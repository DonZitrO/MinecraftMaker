package com.minecade.minecraftmaker.controller;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.minecade.mcore.util.BukkitUtils.verifyPrimaryThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.minecade.mcore.controller.AbstractController;
import com.minecade.mcore.data.CoinTransaction;
import com.minecade.mcore.data.CoinTransaction.Reason;
import com.minecade.mcore.data.CoinTransaction.SourceType;
import com.minecade.mcore.data.CoinTransactionResult;
import com.minecade.mcore.data.LevelOperationResult;
import com.minecade.mcore.data.Rank;
import com.minecade.mcore.event.AsyncAccountDataLoadEvent;
import com.minecade.mcore.event.EventUtils;
import com.minecade.mcore.inventory.MenuClickResult;
import com.minecade.mcore.item.ItemUtils;
import com.minecade.mcore.schematic.bukkit.BukkitUtil;
import com.minecade.mcore.schematic.world.WorldData;
import com.minecade.mcore.util.BungeeUtils;
import com.minecade.mcore.util.Tickable;
import com.minecade.mcore.util.TickableUtils;
import com.minecade.mcore.world.WorldTimeAndWeather;
import com.minecade.minecraftmaker.data.MakerPlayerData;
import com.minecade.minecraftmaker.data.MakerSteveData;
import com.minecade.minecraftmaker.data.MakerUnlockable;
import com.minecade.minecraftmaker.data.UnlockOperationResult;
import com.minecade.minecraftmaker.inventory.LevelBrowserMenu;
import com.minecade.minecraftmaker.inventory.LevelPageResult;
import com.minecade.minecraftmaker.inventory.LevelSearchMenu;
import com.minecade.minecraftmaker.inventory.LevelTemplatesMenu;
import com.minecade.minecraftmaker.items.GeneralMenuItem;
import com.minecade.minecraftmaker.items.MakerLobbyItem;
import com.minecade.minecraftmaker.level.LevelSortBy;
import com.minecade.minecraftmaker.level.MakerDisplayableLevel;
import com.minecade.minecraftmaker.level.MakerLevelTemplate;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;
import com.minecade.minecraftmaker.player.MakerPlayer;
import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.util.LevelUtils;
import com.minecade.minecraftmaker.util.MakerWorldUtils;
import com.minecade.nms.NMSUtils;

public class MakerController extends AbstractController<MakerPlayer> implements Runnable, Tickable {

	private static final Vector DEFAULT_SPAWN_VECTOR = new Vector(-25.5d, 35.0d, 63.5d);
	private static final float DEFAULT_SPAWN_YAW = 90.0f;
	private static final float DEFAULT_SPAWN_PITCH = -15.0f;
	private static final int DEFAULT_PLAYERS_EXTRA_SLOTS = 10;
	private static final String DEFAULT_WORLD_NAME = "mcmaker";

	private final MinecraftMakerPlugin plugin;
	private final int maxPlayers;

	private BukkitTask globalTickerTask;
	//private World mainWorld;
	// configurable fields (config.yml)
	private String mainWorldName;
	private Vector spawnVector;
	private float spawnYaw;
	private float spawnPitch;
	private int premiumPlayersExtraSlots;
	// tick related fields
	private long currentTick;
	private boolean disabled;
	private boolean initialized;

	//private short maxLevels;

	private final Set<UUID> mutedPlayers = Collections.synchronizedSet(new HashSet<UUID>());
	private final Set<UUID> mutedOtherPlayers = Collections.synchronizedSet(new HashSet<UUID>());

	private final Set<String> entriesToRemoveFromScoreboardTeams = new HashSet<>();
	private final Set<UUID> entriesToAddToScoreboardTeams = new HashSet<>();

	// worlds
	private Map<WorldTimeAndWeather, World> worlds = new ConcurrentHashMap<>();
	// keeps track of every player on the server
	private Map<UUID, MakerPlayer> playerMap = new ConcurrentHashMap<>();
	// keeps track of every arena on the server
	private Map<Short, MakerPlayableLevel> levelMap= new ConcurrentHashMap<>();
	// steve level serials
	// private final Set<Long> steveLevelSerials = new HashSet<>();
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
		this.maxPlayers = Bukkit.getMaxPlayers();
		this.mainWorldName = config != null ? config.getString("main-world", DEFAULT_WORLD_NAME) : DEFAULT_WORLD_NAME;
		//this.maxPlayers = config != null ? config.getInt("max-players", DEFAULT_MAX_PLAYERS) : DEFAULT_MAX_PLAYERS;
		//this.maxLevels = config != null ? (short)config.getInt("max-levels", DEFAULT_MAX_LEVELS) : DEFAULT_MAX_LEVELS;
		this.spawnVector = config != null ? config.getVector("spawn-vector", DEFAULT_SPAWN_VECTOR) : DEFAULT_SPAWN_VECTOR;
		this.spawnYaw = config != null ? (float)config.getDouble("spawn-yaw", DEFAULT_SPAWN_YAW) : DEFAULT_SPAWN_YAW;
		this.spawnPitch = config != null ? (float)config.getDouble("spawn-pitch", DEFAULT_SPAWN_PITCH) : DEFAULT_SPAWN_PITCH;
		this.premiumPlayersExtraSlots = config != null ? config.getInt("premium-extra-slots", DEFAULT_PLAYERS_EXTRA_SLOTS) : DEFAULT_PLAYERS_EXTRA_SLOTS;
	}

	private void addMakerPlayer(Player player, MakerPlayerData data) {
		final MakerPlayer mPlayer = new MakerPlayer(plugin, player, data);
		// add the player to the player map
		playerMap.put(mPlayer.getUniqueId(), mPlayer);
		// TODO: notify player joined
		//plugin.publishServerInfoAsync();
		// setup scoreboard
		mPlayer.initScoreboard(plugin);
		entriesToAddToScoreboardTeams.add(mPlayer.getUniqueId());
		// add the player to the lobby
		addPlayerToGameLobby(mPlayer);
		// welcome message
		Bukkit.getScheduler().runTask(plugin, () -> sendBruteForceWelcomeMessage(mPlayer));
	}

	@Override
	public void addPlayerToGameLobby(MakerPlayer mPlayer) {
		// reset player
		mPlayer.resetPlayer(GameMode.ADVENTURE);
		// teleport to spawn point
		if (!mPlayer.getPlayer().getLocation().getWorld().equals(getLobbyWorld()) || mPlayer.getPlayer().getLocation().distanceSquared(getDefaultSpawnLocation()) > 4d) {
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
		// reset player list name
		mPlayer.getPlayer().setPlayerListName(mPlayer.getDisplayName());
	}

	public void broadcastToDefaultPlayers(String message) {
		for (MakerPlayer mPlayer : playerMap.values()) {
			if (!mPlayer.hasRank(Rank.VIP) && mPlayer.getPlayer().isOnline()) {
				mPlayer.getPlayer().sendMessage(message);
			}
		}
	}

	public void checkTemplate(MakerPlayer mPlayer, MakerLevelTemplate template) {
		checkNotNull(mPlayer);
		checkNotNull(template);
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage("template.check.error.player-busy");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage("level.error.full");
			return;
		}
		level.setLevelId(UUID.randomUUID());
		level.setAuthorId(UUID.randomUUID());
		level.setAuthorName(template.getAuthorName());
		level.setAuthorRank(Rank.GUEST);
		level.setLevelTemplate(template);
		level.setTemplateCheckerId(mPlayer.getUniqueId());
		level.waitForBusyLevel(mPlayer, true, false, true);
	}

	public void clearSteveChallengeCallback(UUID playerId, String playerName) {
		verifyPrimaryThread();
		checkNotNull(playerId);
		checkNotNull(playerName);
		String description = plugin.getMessage("coin.transaction.first-time-steve-challenge-clear.description", playerName);
		CoinTransaction transaction = new CoinTransaction(UUID.randomUUID(), playerId, 1000, plugin.getServerUniqueId(), SourceType.SERVER, Reason.FIRST_TIME_STEVE_CHALLENGE_CLEAR, description);
		plugin.getDatabaseAdapter().executeCoinTransactionAsync(transaction);
		MakerPlayer mPlayer = plugin.getController().getPlayer(playerId);
		if (mPlayer != null) {
			mPlayer.getData().setSteveClear(true);
		}
	}

	public void coinTransactionCallback(CoinTransaction transaction, CoinTransactionResult result, long balance) {
		checkNotNull(transaction);
		verifyPrimaryThread();
		switch (result) {
		case COMMITTED:
			notifyCoinTransactionCommit(transaction, balance);
			break;
		case INSUFFICIENT_COINS:
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "coin.transaction.error.insufficient-coins");
			break;
		default:
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "server.error.internal");
			break;
		}
		if (balance >= 0) {
			updatePlayerCoinBalanceIfPresent(transaction.getPlayerId(), balance);
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "coin.transaction.new-balance", balance); 
		}
	}

	private void controlDoubleLoginHack(AsyncPlayerPreLoginEvent event) {
		if (playerMap.containsKey(event.getUniqueId())) {
			Bukkit.getLogger().warning(String.format("[POSSIBLE-HACK] | MakerController.controlDoubleLoginHack - possible double login detected for player: [%s<%s>]", event.getName(), event.getUniqueId()));
			event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_OTHER, plugin.getMessage("server.error.too-fast-relogin"));
		}
	}

	private void controlFastReloginHack(AsyncPlayerPreLoginEvent event) {
		Long nextAllowedLogin = nextAllowedLogins.get(event.getUniqueId());
		if (nextAllowedLogin != null && System.currentTimeMillis() < nextAllowedLogin.longValue()) {
			Bukkit.getLogger().warning(String.format("[POSSIBLE-HACK] | MakerController.controlFastReloginHack - possible too fast relogin detected for player: [%s<%s>]", event.getName(), event.getUniqueId()));
			event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_OTHER, plugin.getMessage("server.error.too-fast-relogin"));
		} else {
			nextAllowedLogins.put(event.getUniqueId(), System.currentTimeMillis() + (FAST_RELOGIN_DELAY_SECONDS * 1000));
		}
	}

	public void copyAndLoadLevelForEditingBySerial(MakerPlayer mPlayer, long copyFromSerial) {
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage("level.create.error.author-busy");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage("level.error.full");
			return;
		}
		level.setLevelId(UUID.randomUUID());
		level.setAuthorId(mPlayer.getUniqueId());
		level.setAuthorName(mPlayer.getName());
		level.setAuthorRank(mPlayer.getData().getHighestRank());
		level.setCopyFromSerial(copyFromSerial);
		level.waitForBusyLevel(mPlayer, true, false, true);
	}

	public void createEmptyLevel(MakerPlayer author, MakerLevelTemplate template) {
		checkNotNull(author);
		checkNotNull(template);
		if (!author.isInLobby()) {
			author.sendActionMessage("level.create.error.author-busy");
			return;
		}
		if (!author.canCreateLevel()) {
			author.sendMessage("level.create.error.unpublished-limit", author.getUnpublishedLevelsCount());
			author.sendMessage("level.create.error.unpublished-limit.publish-delete");
			if (!author.hasRank(Rank.TITAN)) {
				author.sendMessage("upgrade.rank.increase.limits.or");
				author.sendMessage("upgrade.rank.unpublished.limits");
			}
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			author.sendActionMessage("level.error.full");
			return;
		}
		level.setLevelId(UUID.randomUUID());
		level.setAuthorId(author.getUniqueId());
		level.setAuthorName(author.getName());
		level.setAuthorRank(author.getData().getHighestRank());
		level.setLevelTemplate(template);
		level.waitForBusyLevel(author, true, false, true);
	}

//	public void createEmptyLevel(UUID authorId, short widthChunks, int floorBlockId) {
//		MakerPlayer author = getPlayer(authorId);
//		if (author == null) {
//			Bukkit.getLogger().warning(String.format("MakerController.createEmptyLevel - author must be online in order to create a level!"));
//			return;
//		}
//		createEmptyLevel(author, floorBlockId);
//	}

	public void deleteLevel(MakerPlayer mPlayer, long serial) {
		long confirmSerial = mPlayer.getLevelToDeleteSerial();
		if (confirmSerial != serial) {
			mPlayer.setLevelToDeleteSerial(serial);
			mPlayer.sendMessage("command.level.delete.confirm1", serial);
			if (!mPlayer.hasRank(Rank.ADMIN)) {
				mPlayer.sendMessage("command.level.delete.confirm2", serial);
			}
			mPlayer.sendMessage("command.level.delete.confirm3", serial);
		} else {
			plugin.getDatabaseAdapter().deleteLevelBySerialAsync(serial, mPlayer);
		}
	}

	public void deleteLevelBySerialCallback(long levelSerial, UUID playerId, LevelOperationResult result, Long playerCoinBalance, Integer totalPublishedLevelsCount) {
		verifyPrimaryThread();
		switch (result) {
		case SUCCESS:
			MakerPlayer mPlayer = getPlayer(playerId);
			if (mPlayer == null) {
				break;
			}
			mPlayer.sendMessage("command.level.delete.success", levelSerial);
			if (!mPlayer.hasRank(Rank.ADMIN)) {
				mPlayer.sendMessage("coin.transaction.level-delete.player", 500);
			}
			if (playerCoinBalance != null) {
				mPlayer.setCoins(playerCoinBalance);
				mPlayer.sendMessage("coin.transaction.new-balance", playerCoinBalance); 
			}
			plugin.getDatabaseAdapter().loadPlayerLevelsCountAsync(playerId);
			break;
		case NOT_FOUND:
			sendMessageToPlayerIfPresent(playerId, "command.level.error.not-found", levelSerial);
			break;
		case PERMISSION_DENIED:
			sendMessageToPlayerIfPresent(playerId, "command.level.delete.denied", levelSerial);
			break;
		case INSUFFICIENT_COINS:
			sendMessageToPlayerIfPresent(playerId, "coin.transaction.error.insufficient-coins");
			break;
		case ERROR:
			sendMessageToPlayerIfPresent(playerId, "server.error.internal");
			break;
		default:
			break;
		}
		if (totalPublishedLevelsCount != null) {
			LevelBrowserMenu.updateLevelCount(totalPublishedLevelsCount);
		}
	}

	@Override
	public void disable() {
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
		return spawnVector.toLocation(getLobbyWorld(), spawnYaw, spawnPitch);
	}

	@Override
	public String getDescription() {
		return String.format("MakerController - currentTick: [%s]", getCurrentTick());
	}

	private MakerPlayableLevel getEmptyLevelIfAvailable() {
		for (short i = 0; i < 10; i += 3) {
			if (!levelMap.containsKey(i)) {
				MakerPlayableLevel level = new MakerPlayableLevel(plugin, i);
				levelMap.put(i, level);
				return level;
			}
		}
		for (short i = 100; i <= Short.MAX_VALUE; i += 20) {
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

	public World getLobbyWorld() {
		return getWorld(DEFAULT_TIME_AND_WEATHER);
	}

	public String getMainWorldName() {
		return mainWorldName;
	}

	public MakerPlayer getPlayer(Player player) {
		checkNotNull(player);
		return getPlayer(player.getUniqueId());
	}

	public MakerPlayer getPlayer(UUID playerId) {
		checkNotNull(playerId);
		return playerMap.get(playerId);
	}

	public int getPlayerCount() {
		return playerMap.size();
	}

	public World getWorld(WorldTimeAndWeather timeAndWeather) {
		checkNotNull(timeAndWeather);
		World world = worlds.get(timeAndWeather);
		if (world == null) {
			world = MakerWorldUtils.createOrLoadWorld(this.plugin, String.format("%s%s", getMainWorldName(), timeAndWeather.getWorldSufix(), this.spawnVector));
			world.setStorm(timeAndWeather.isStorm());
			world.setTime(timeAndWeather.getTime());
			world.setThundering(false);
			world.setWeatherDuration(Integer.MAX_VALUE);
			worlds.put(timeAndWeather, world);
		}
		return world;
	}

	// FIXME: find a way to reuse the world data object that doesn't mess with multithreading.
	public WorldData getWorldData(WorldTimeAndWeather timeAndWeather) {
		checkNotNull(timeAndWeather);
		return BukkitUtil.toWorld(getWorld(timeAndWeather)).getWorldData();
	}

	public void init() {
		if (initialized) {
			throw new IllegalStateException("This controller is already initialized");
		}
		Bukkit.getScheduler().runTask(plugin, () -> MakerWorldUtils.removeAllLivingEntitiesExceptPlayers(this.getLobbyWorld()));
		globalTickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 0);
		initialized = true;
	}

	@Override
	public boolean isDisabled() {
		return disabled;
	}

	private boolean isVoidLocation(Location location) {
		return location.getBlockY() < -1;
	}

	public void levelLikeCallback(UUID levelId, UUID playerId, boolean dislike, long totalLikes, long totalDislikes, long trendingScore) {
		verifyPrimaryThread();
		// update level browser
		// LevelBrowserMenu.updateLevelLikes(plugin, levelId, totalLikes, totalDislikes, trendingScore);
		// update current user level
		MakerPlayer mPlayer = getPlayer(playerId);
		if (mPlayer == null) {
			return;
		}
		mPlayer.sendActionMessage(dislike ? "level.dislike.success" : "level.like.success");
		MakerPlayableLevel level = mPlayer.getCurrentLevel();
		if (level == null || !levelId.equals(level.getLevelId())) {
			return;
		}
		level.setLikes(totalDislikes);
		level.setDislikes(totalDislikes);
	}

	public void levelPageResultCallback(LevelPageResult result) {
		verifyPrimaryThread();
		LevelBrowserMenu.updateLevelCount(result.getLevelCount());
		for (UUID playerId :result.getPlayers()) {
			LevelBrowserMenu.updatePlayerMenu(playerId, result.getLevels());
		}
	}

	public void loadLevelForEditingBySerial(MakerPlayer mPlayer, Long levelSerial) {
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage("level.play.error.author-busy");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage("level.error.full");
			return;
		}
		level.setLevelSerial(levelSerial);
		level.setAuthorId(mPlayer.getUniqueId());
		level.waitForBusyLevel(mPlayer, true, false, true);
	}

	public void loadLevelForPlayingBySerial(MakerPlayer mPlayer, Long levelSerial) {
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage("level.play.error.player-busy");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage("level.error.full");
			return;
		}
		level.setLevelSerial(levelSerial);
		level.setCurrentPlayerId(mPlayer.getUniqueId());
		level.waitForBusyLevel(mPlayer, true, false, true);
	}

	public void loadLevelTemplatesCallback(Collection<MakerLevelTemplate> templates) {
		verifyPrimaryThread();
		checkNotNull(templates);
		LevelTemplatesMenu.updateTemplates(templates);
	}

	public void loadPublishedLevelCallback(MakerDisplayableLevel level, int levelCount) {
		verifyPrimaryThread();
		// FIXME: candidate for removal?
		LevelBrowserMenu.updateLevelCount(levelCount);
		//PlayerLevelsMenu.removeLevelFromViewer(level);
		//steveLevelSerials.add(level.getLevelSerial());
		//LevelBrowserMenu.addOrUpdateLevel(plugin, level);
	}

	public void muteOthers(UUID playerId) {
		mutedOtherPlayers.add(playerId);
	}

	public void mutePlayer(UUID playerId) {
		mutedPlayers.add(playerId);
	}

	private void notifyCoinTransactionCommit(CoinTransaction transaction, long balance) {
		checkNotNull(transaction);
		verifyPrimaryThread();
		switch (transaction.getReason()) {
		case FIRST_TIME_LEVEL_CLEAR:
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "coin.transaction.first-time-level-clear.player", transaction.getAmount());
			break;
		case POPULAR_LEVEL_RECORD_BEAT:
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "coin.transaction.popular-level-record-beat.player", transaction.getAmount());
			break;
		case STEVE_CHALLENGE_CLEAR:
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "coin.transaction.steve-challenge-clear.player", transaction.getAmount());
			break;
		case LEVEL_UNPUBLISH:
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "coin.transaction.level-unpublish.player", transaction.getAmount());
			break;
		case LEVEL_DELETE:
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "coin.transaction.level-delete.player", transaction.getAmount());
			break;
		case FIRST_TIME_STEVE_CHALLENGE_CLEAR:
			sendMessageToPlayerIfPresent(transaction.getPlayerId(), "coin.transaction.first-time-steve-challenge-clear.player", transaction.getAmount());
			break;
		default:
			break;
		}
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
		MakerPlayer mPlayer = getPlayer(event.getPlayer().getUniqueId());
		if (mPlayer == null) {
			event.setCancelled(true);
			return;
		}
		if (mutedPlayers.contains(mPlayer.getUniqueId())) {
			Bukkit.getScheduler().runTask(plugin, () -> mPlayer.sendMessage("player.muted"));
			event.setCancelled(true);
			return;
		}
		event.setFormat(String.format("%%s%s: %%s", StringUtils.EMPTY, ChatColor.GRAY, ChatColor.DARK_GRAY));
		Iterator<Player> iter = event.getRecipients().iterator();
		while (iter.hasNext()) {
			Player recipient = iter.next();
			if (mutedOtherPlayers.contains(recipient.getUniqueId())) {
				iter.remove();
				continue;
			}
		}
	}

	public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		// allow vanilla white-list logic to work
		if (org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST.equals(event.getLoginResult())) {
			return;
		}
		// login hack prevention
		controlDoubleLoginHack(event);
		controlFastReloginHack(event);
		if (org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_OTHER.equals(event.getLoginResult())) {
			return;
		}

		// this code is not run by the main thread
		final MakerPlayerData data = plugin.getDatabaseAdapter().loadAccountData(event.getUniqueId(), event.getName());
		if (null == data) {
			event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_OTHER, plugin.getMessage("server.error.missing-data"));
			return;
		}
		// as this is async code, we get this from our own concurrent map, not from the internal bukkit entity collections!
		int currentPlayerCount = getPlayerCount();
		if (currentPlayerCount >= maxPlayers || org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_FULL.equals(event.getLoginResult())) {
			if (data.hasRank(Rank.GM) || data.hasRank(Rank.YT)) {
				// allows YTs and staff to join full servers
				event.allow();
			} else if (data.hasRank(Rank.VIP)) {
				// allows ranked players to use some extra slots
				if (getPlayerCount() < maxPlayers + premiumPlayersExtraSlots) {
					data.setAllowedInFullServer(true);
					event.allow();
				} else {
					event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_OTHER, plugin.getMessage("server.error.no-free-premium-slots"));
				}
			} else {
				event.disallow(org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.KICK_OTHER, plugin.getMessage("server.error.full.upgrade"));
			}
		}
		if (!org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.ALLOWED.equals(event.getLoginResult())) {
			return;
		}
		accountDataMap.put(event.getUniqueId(), data);
		// this event call is not very useful until this code gets integrated to the core
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
		if (!mPlayer.isEditing()) {
			event.setCancelled(true);
			return;
		}
		if (event.getBlock().getType().equals(Material.BEACON)) {
			if (event.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.IRON_BLOCK)) {
				mPlayer.sendMessage("level.edit.clear-bacon");
			} else {
				mPlayer.sendActionMessage("level.edit.error.start-bacon");
			}
			event.setCancelled(true);
			return;
		}
		if (LevelUtils.isBeaconPowerBlock(event.getBlock())) {
			mPlayer.sendActionMessage("level.edit.error.bacon-power-block");
			event.setCancelled(true);
			return;
		}
	}

	public void onBlockDispense(BlockDispenseEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getBlock().getLocation());
		if (slot < 0) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onBlockDispense - cancelled block dispense - type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onBlockDispense - cancelled block dispense on unregistered level slot: [%s] - type: [%s] - location: [%s]", slot, event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			return;
		}
		level.onBlockDispense(event);
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

	public void onBlockIgnite(BlockIgniteEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getBlock().getLocation());
		if (slot < 0) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onBlockIgnite - cancelled block igniting on outside levels - type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			Bukkit.getLogger().warning(String.format("MakerController.onBlockIgnite - cancelled block igniting on unregistered level slot - type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			return;
		}
		level.onBlockIgnite(event);
	}

	public void onBlockPhysics(BlockPhysicsEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getBlock().getLocation());
		if (slot < 0) {
			event.setCancelled(true);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("MakerController.onBlockPhysics - cancelled block physics outside level - block type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			}
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setCancelled(true);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("MakerController.onBlockPhysics - cancelled block physics from unregistered level slot - block type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			}
			return;
		}
		level.onBlockPhysics(event);
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
		if (!mPlayer.isEditing()) {
			event.setCancelled(true);
			return;
		}
		// end level beacon placement
		switch (event.getBlockPlaced().getType()) {
		case BEACON:
			if (!mPlayer.isAuthorEditingLevel()) {
				mPlayer.sendMessage("level.edit.error.author-only");
				event.setCancelled(true);
				return;
			}
			if (!LevelUtils.isValidEndBeaconLocation(event.getBlockPlaced().getLocation().toVector(), mPlayer.getCurrentLevel().getLevelRegion())) {
				event.setCancelled(true);
				mPlayer.sendActionMessage("level.create.error.end-beacon-too-close-to-border");
				return;
			}
			Bukkit.getScheduler().runTask(plugin, () -> mPlayer.getCurrentLevel().setupEndLocation(event.getBlockPlaced().getLocation()));
			return;
		// place for disabled building blocks
		case BARRIER:
		case ENDER_CHEST:
			event.setCancelled(true);
			mPlayer.sendActionMessage("level.create.error.disabled-block");
			return;
		default:
			if (mPlayer.getCurrentLevel().getRelativeEndLocation() != null && LevelUtils.isAboveLocation(event.getBlockPlaced().getLocation().toVector(), mPlayer.getCurrentLevel().getEndLocation().toVector())) {
				event.setCancelled(true);
				mPlayer.sendActionMessage("level.create.error.block-place-above-end-beacon");
				return;
			}
			break;
		}
	}

	public void onBlockRedstone(BlockRedstoneEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getBlock().getLocation());
		if (slot < 0) {
			Block block = event.getBlock();
			block.setType(Material.AIR);
			block.getState().update(true);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("MakerController.onBlockRedstone - cancelled redstone change outside level - block type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			}
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			Block block = event.getBlock();
			block.setType(Material.AIR);
			block.getState().update(true);
			if (plugin.isDebugMode()) {
				Bukkit.getLogger().warning(String.format("MakerController.onBlockRedstone - cancelled redstone change from unregistered level slot - block type: [%s] - location: [%s]", event.getBlock().getType(), event.getBlock().getLocation().toVector()));
			}
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
		if (mPlayer.isEditing() && event.getEntity() instanceof LivingEntity) {
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
			event.setTo(event.getFrom());
			Bukkit.getLogger().warning(String.format("MakerController.onEntityTeleport - cancelled creature teleporting from outside level - creature type: [%s] - from: [%s]", event.getEntityType(), event.getFrom().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setTo(event.getFrom());
			Bukkit.getLogger().warning(String.format("MakerController.onEntityTeleport - cancelled creature teleporting from unregistered level slot - creature type: [%s] - location: [%s]", event.getEntityType(), event.getFrom().toVector()));
			return;
		}
		level.onEntityTeleport(event);
	}

	protected MenuClickResult onMenuItemClick(MakerPlayer mPlayer, ItemStack item) {
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return MenuClickResult.ALLOW;
		}
		if (mPlayer.isInLobby()) {
			if (ItemUtils.itemNameEquals(item, MakerLobbyItem.SERVER_BROWSER.getDisplayName())) {
				//mPlayer.openServerBrowserMenu();
				mPlayer.sendActionMessage("general.coming-soon");
				return MenuClickResult.CANCEL_UPDATE;
			} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.STEVE_CHALLENGE.getDisplayName())) {
				startSteveChallenge(mPlayer);
				return MenuClickResult.CANCEL_UPDATE;
			} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.CREATE_LEVEL.getDisplayName())) {
				if (!mPlayer.canCreateLevel()) {
					mPlayer.sendMessage("level.create.error.unpublished-limit", mPlayer.getUnpublishedLevelsCount());
					mPlayer.sendMessage("level.create.error.unpublished-limit.publish-delete");
					if (!mPlayer.hasRank(Rank.TITAN)) {
						mPlayer.sendMessage("upgrade.rank.increase.limits.or");
						mPlayer.sendMessage("upgrade.rank.unpublished.limits");
					}
					return MenuClickResult.CANCEL_CLOSE;
				}
				mPlayer.openLevelTemplateMenu();
				return MenuClickResult.CANCEL_UPDATE;
			} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.PLAYER_LEVELS.getDisplayName())) {
				mPlayer.openPlayerLevelsMenu(true);
				return MenuClickResult.CANCEL_UPDATE;
			} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.LEVEL_BROWSER.getDisplayName())) {
				mPlayer.openLevelBrowserMenu();
				return MenuClickResult.CANCEL_UPDATE;
			} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.SPECTATE.getDisplayName())) {
				if (!mPlayer.hasRank(Rank.VIP)) {
					mPlayer.sendMessage("upgrade.rank.spectate");
				} else {
					plugin.getController().startSpectating(mPlayer);
				}
				return MenuClickResult.CANCEL_CLOSE;
			} else if (ItemUtils.itemNameEquals(item, MakerLobbyItem.QUIT.getDisplayName())) {
				BungeeUtils.switchServer(plugin, mPlayer.getPlayer(), "l1", plugin.getMessage("server.quit.connecting", "Lobby1"));
				return MenuClickResult.CANCEL_UPDATE;
			}
			return MenuClickResult.ALLOW;
		}
		if (mPlayer.isCheckingTemplate()) {
			if (ItemUtils.itemNameEquals(item, GeneralMenuItem.CHECK_TEMPLATE_OPTIONS.getDisplayName())) {
				mPlayer.openCheckTemplateOptionsMenu();
				return MenuClickResult.CANCEL_UPDATE;
			}
			return MenuClickResult.ALLOW;
		}
		if (mPlayer.isInSteve()) {
			if (ItemUtils.itemNameEquals(item, GeneralMenuItem.STEVE_LEVEL_OPTIONS.getDisplayName())) {
				if (mPlayer.isPlayingLevel()) {
					mPlayer.openSteveLevelOptionsMenu();
				} else if (mPlayer.hasClearedLevel()) {
					mPlayer.openSteveClearLevelOptionsMenu();
				}
				return MenuClickResult.CANCEL_UPDATE;
			}
			return MenuClickResult.ALLOW;
		}
		if (mPlayer.isAuthorEditingLevel()) {
			if (ItemUtils.itemNameEquals(item, GeneralMenuItem.EDIT_LEVEL_OPTIONS.getDisplayName())) {
				mPlayer.openEditLevelOptionsMenu();
				return MenuClickResult.CANCEL_UPDATE;
			}
			return MenuClickResult.ALLOW;
		}
		if (mPlayer.isGuestEditingLevel()) {
			if (ItemUtils.itemNameEquals(item, GeneralMenuItem.GUEST_EDIT_LEVEL_OPTIONS.getDisplayName())) {
				mPlayer.openGuestEditLevelOptionsMenu();
				return MenuClickResult.CANCEL_UPDATE;
			}
			return MenuClickResult.ALLOW;
		}
		if (mPlayer.isPlayingLevel() || mPlayer.hasClearedLevel()) {
			if (ItemUtils.itemNameEquals(item, GeneralMenuItem.PLAY_LEVEL_OPTIONS.getDisplayName())) {
				mPlayer.openPlayLevelOptionsMenu();
				return MenuClickResult.CANCEL_UPDATE;
			} else if (ItemUtils.itemNameEquals(item, GeneralMenuItem.EDITOR_PLAY_LEVEL_OPTIONS.getDisplayName())) {
				mPlayer.openEditorPlayLevelOptionsMenu();
				return MenuClickResult.CANCEL_UPDATE;
			}
			return MenuClickResult.ALLOW;
		}
		return MenuClickResult.ALLOW;
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
		if (mPlayer.isInBusyLevel()) {
			event.setCancelled(true);
			return;
		}
		// level creator back to spawn from void
		if (mPlayer.isEditing()) {
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
		event.setKeepInventory(false);
		event.setKeepLevel(false);
		event.getDrops().clear();
		event.setDroppedExp(0);
		NMSUtils.respawnOnNextTick(plugin, event.getEntity());
		// event.getEntity().spigot().respawn();
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
		if (EventUtils.isItemClick(event)) {
			switch (onMenuItemClick(mPlayer, event.getItem())) {
			case CANCEL_CLOSE:
				event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
				event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
				event.setCancelled(true);
				mPlayer.closeInventory();
				return;
			case CANCEL_UPDATE:
				event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
				event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
				event.setCancelled(true);
				mPlayer.updateInventory();
				return;
			default:
				break;
			}
		}
		if (mPlayer.isEditing()) {
			if (EventUtils.isItemRightClick(event, Material.LAVA_BUCKET)) {
				mPlayer.sendActionMessage("level.create.error.disabled-block");
				event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
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
		if (mPlayer.isEditing() && event.getRightClicked().getType().equals(EntityType.HORSE)) {
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
				mPlayer.sendMessage("level.create.horse.tamed");
				return;
			} else {
				mPlayer.sendMessage("level.create.horse.untamed");
				event.setCancelled(true);
			}
			return;
		}
	}

	public void onPlayerJoin(Player player) {
		Bukkit.getLogger().info(String.format("MakerController.onPlayerJoin - Player: [%s<%s>]", player.getName(), player.getUniqueId()));
		player.setInvulnerable(true);
		MakerPlayerData data = accountDataMap.remove(player.getUniqueId());
		if (null == data && !playerMap.containsKey(player.getUniqueId())) {
			Bukkit.getLogger().info(String.format("MakerController.onPlayerJoin - No data available yet for Player: [%s<%s>]", player.getName(), player.getUniqueId()));
			// avoid double login hack
			new BukkitRunnable() {
				@Override
				public void run() {
					if (player.isOnline() && !playerMap.containsKey(player.getUniqueId())) {
						Bukkit.getLogger().warning(String.format("[POSSIBLE-HACK] | MakerController.onPlayerJoin - possible double login for player: [%s<%s>]", player.getName(), player.getUniqueId()));
						player.kickPlayer(plugin.getMessage("server.error.too-fast-relogin"));
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

	public void onPlayerLogin(PlayerLoginEvent event) {
		// allow vanilla white-list logic to work
		if (org.bukkit.event.player.PlayerLoginEvent.Result.KICK_WHITELIST.equals(event.getResult())) {
			event.setKickMessage(plugin.getMessage("server.login.error.whitelist"));
			accountDataMap.remove(event.getPlayer().getUniqueId());
			return;
		}
		if (!accountDataMap.containsKey(event.getPlayer().getUniqueId())) {
			event.disallow(org.bukkit.event.player.PlayerLoginEvent.Result.KICK_OTHER, plugin.getMessage("server.error.missing-data"));
			return;
		}
		event.allow();
	}

	public void onPlayerMove(PlayerMoveEvent event) {
		final MakerPlayer mPlayer = getPlayer(event.getPlayer());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerMove - untracked Player: [%s]", event.getPlayer().getName()));
			event.setCancelled(true);
			return;
		}
		if (mPlayer.isInLobby() || mPlayer.isCheckingTemplate()) {
			return;
		}
		if (mPlayer.isSpectating()) {
			if (event.getTo().getBlockX() < -48 || event.getTo().getBlockX() > 186) {
				event.setTo(event.getFrom());
				mPlayer.sendActionMessage("spectator.off-limits");
				return;
			}
			if (event.getTo().getBlockY() < -16 || event.getTo().getBlockY() > 80) {
				event.setTo(event.getFrom());
				mPlayer.sendActionMessage("spectator.off-limits");
				return;
			}
			if (event.getTo().getBlockZ() < -16 || event.getTo().getBlockZ() > 16000) {
				event.setTo(event.getFrom());
				mPlayer.sendActionMessage("spectator.off-limits");
				return;
			}
			return;
		}
		if (mPlayer.isInBusyLevel()) {
			// look but don't move
			if (event.getTo().getX() != event.getFrom().getX() || event.getTo().getY() != event.getFrom().getY() || event.getTo().getZ() != event.getFrom().getZ()) {
				mPlayer.sendActionMessage("level.busy.look-dont-move");
				event.setCancelled(true);
			}
			return;
		}
		if (mPlayer.isEditing()) {
			if (!mPlayer.getCurrentLevel().contains(event.getTo().toVector())) {
				event.setTo(event.getFrom());
				event.setCancelled(true);
			}
			return;
		}
		if (mPlayer.isPlayingLevel()) {
			if (isVoidLocation(event.getTo())) {
				mPlayer.getCurrentLevel().restartPlaying();
				return;
			}
			mPlayer.getCurrentLevel().checkLevelEnd(event.getTo());
			return;
		}
	}

	public void onPlayerQuit(Player player) {
		// wait a bit before coming back
		nextAllowedLogins.put(player.getUniqueId(), System.currentTimeMillis() + (FAST_RELOGIN_DELAY_SECONDS * 1000));
		// we need to remove this player from every other scoreboard team
		entriesToRemoveFromScoreboardTeams.add(player.getName());
		// remove from map
		MakerPlayer mPlayer = playerMap.remove(player.getUniqueId());
		if (mPlayer != null) {
			MakerPlayableLevel level = mPlayer.getCurrentLevel();
			if (level != null && !mPlayer.isGuestEditingLevel()) {
				level.onPlayerQuit();
			}
			mPlayer.onQuit();
		} else {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerQuit - Player: [%s] was already removed", player.getName()));
		}
		// TODO: notify rabbit that player left
		// plugin.publishServerInfoAsync();
	}

	public void onPlayerRespawn(PlayerRespawnEvent event) {
		event.getPlayer().setInvulnerable(true);
		final MakerPlayer mPlayer = getPlayer(event.getPlayer());
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerRespawn - untracked player: [%s]", event.getPlayer().getName()));
			return;
		}
		if (mPlayer.hasClearedLevel()) {
			event.setRespawnLocation(mPlayer.getCurrentLevel().getEndLocation());
			return;
		}
		if (mPlayer.isOnLevel()) {
			event.setRespawnLocation(mPlayer.getCurrentLevel().getStartLocation());
			if (mPlayer.getCurrentLevel().isPlayable() && !mPlayer.getCurrentLevel().isBusy()) {
				mPlayer.getCurrentLevel().restartPlaying();
			}
			return;
		}
		event.setRespawnLocation(getDefaultSpawnLocation());
		if (mPlayer.isInLobby()) {
			Bukkit.getScheduler().runTask(plugin, () -> addPlayerToGameLobby(mPlayer));
		}
	}

	public void onPlayerTeleport(PlayerTeleportEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getFrom());
		if (slot < 0) {
			event.setTo(event.getFrom());
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerTeleport - cancelled player teleporting from outside level - player: [%s] - cause: [%s] - from: [%s]", event.getPlayer().getName(), event.getCause(), event.getFrom().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.setTo(event.getFrom());
			Bukkit.getLogger().warning(String.format("MakerController.onPlayerTeleport - cancelled  player teleporting from unregistered level slot - player: [%s] - cause: [%s] - location: [%s]", event.getPlayer().getName(), event.getCause(), event.getFrom().toVector()));
			return;
		}
		level.onPlayerTeleport(event);
	}

	public void onVehicleCreate(VehicleCreateEvent event) {
		short slot = LevelUtils.getLocationSlot(event.getVehicle().getLocation());
		if (slot < 0) {
			event.getVehicle().remove();
			Bukkit.getLogger().warning(String.format("MakerController.onVehicleCreate - cancelled vehicle creation outside level - type: [%s] - location: [%s]", event.getVehicle().getType(), event.getVehicle().getLocation().toVector()));
			return;
		}
		MakerPlayableLevel level = levelMap.get(slot);
		if (level == null) {
			event.getVehicle().remove();
			Bukkit.getLogger().warning(String.format("MakerController.onVehicleCreate - cancelled vehicle creation on unregistered level slot - type: [%s] - location: [%s]", event.getVehicle().getType(), event.getVehicle().getLocation().toVector()));
			return;
		}
		level.onVehicleCreate(event);
	}

//	public void onVehicleDestroy(VehicleDestroyEvent event) {
//		short slot = LevelUtils.getLocationSlot(event.getVehicle().getLocation());
//		if (slot < 0) {
//			return;
//		}
//		MakerPlayableLevel level = levelMap.get(slot);
//		if (level == null) {
//			return;
//		}
//		level.onVehicleDestroy(event);
//	}

	public void onVehicleMove(VehicleMoveEvent event) {
		Entity passenger = event.getVehicle().getPassenger();
		if (passenger == null || !(passenger instanceof Player) || ((Player) passenger).isDead()) {
			return;
		}
		final MakerPlayer mPlayer = getPlayer((Player)passenger);
		if (mPlayer == null) {
			Bukkit.getLogger().warning(String.format("MakerController.onVehicleMove - untracked player riding vehicle: [%s]", passenger.getName()));
			return;
		}
		if (mPlayer.isPlayingLevel()) {
			if (isVoidLocation(event.getTo())) {
				mPlayer.getCurrentLevel().restartPlaying();
				return;
			}
			mPlayer.getCurrentLevel().checkLevelEnd(event.getTo());
			return;
		}
	}

	public void playerLevelClearsCountCallback(UUID playerId, long result) {
		verifyPrimaryThread();
		MakerPlayer mPlayer = getPlayer(playerId);
		if (mPlayer == null) {
			return;
		}
		mPlayer.setUniqueLevelClearsCount(result);
	}

	public void playerLevelsCountCallback(UUID authorId, int publishedCount, int unpublishedCount) {
		verifyPrimaryThread();
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().warning(String.format("[DEBUG] | MakerController.playerLevelsCountCallback - maker: [%s] - published count: [%s] - unpublished count: [%s]", authorId, publishedCount, unpublishedCount));
		}
		MakerPlayer mPlayer = getPlayer(authorId);
		if (mPlayer == null) {
			return;
		}
		mPlayer.setPublishedLevelsCount(publishedCount);
		mPlayer.setUnblishedLevelsCount(unpublishedCount);
	}

	public void removeLevelFromSlot(MakerPlayableLevel makerLevel) {
		Bukkit.getLogger().warning(String.format("MakerController.removeLevelFromSlot - removing level: [%s<%s>] from slot: [%s]", makerLevel.getLevelName(), makerLevel.getLevelId(), makerLevel.getChunkZ()));
		levelMap.remove(makerLevel.getChunkZ());
	}

	public void renameLevel(MakerPlayer mPlayer, String newName) {
		// needs to be editing that level
		if (!mPlayer.isAuthorEditingLevel()) {
			mPlayer.sendActionMessage("level.rename.error.no-editing");
			return;
		}
		if (newName.equals(mPlayer.getCurrentLevel().getLevelName())) {
			mPlayer.sendActionMessage("level.rename.error.different-name");
			return;
		}
		// rename
		mPlayer.getCurrentLevel().rename(newName);
	}

	public void requestLevelPageUpdate(LevelSortBy sortBy, boolean reverseSortBy, int currentPage, UUID playerId) {
		if (plugin.isDebugMode()) {
			Bukkit.getLogger().warning(String.format("[DEBUG] | MakerController.requestLevelPageUpdate - sortBy: [%s] - reverse: [%s] - page: [%s] - playerId: [%s]", sortBy, reverseSortBy, currentPage, playerId));
		}
		plugin.getAsyncLevelBrowserUpdater().requestLevelPageUpdate(sortBy, reverseSortBy, currentPage, playerId);
	}

	@Override
	public void run() {
		tick(getCurrentTick() + 1);
	}

	public void searchLevels(MakerPlayer mPlayer, String searchString) {
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage("level.search.error.not-in-lobby");
			return;
		}
		if (!mPlayer.canSearchAgain()){
			mPlayer.sendActionMessage("level.search.error.too-soon");
			return;
		}
		mPlayer.resetLevelSearchMenu();
		plugin.getDatabaseAdapter().searchPublishedLevelsPageByNameAsync(mPlayer.getUniqueId(), searchString, 0, LevelSearchMenu.ITEMS_PER_PAGE);
	}

	public void searchLevelsCallback(UUID playerId, String searchString, int levelCount, Collection<MakerDisplayableLevel> levels) {
		verifyPrimaryThread();
		MakerPlayer mPlayer = getPlayer(playerId);
		if (mPlayer == null || !mPlayer.isInLobby()) {
			return;
		}
		if (levelCount == 0) {
			mPlayer.sendActionMessage("level.search.error.no-results", searchString);
			return;
//		} else if (levels == null) {
//			mPlayer.sendActionMessage("level.search.error.too-many-results", levelCount, searchString);
//			return;
		}
		mPlayer.openLevelSearchMenu(searchString, levelCount, levels);
	}

	public void sendActionMessageToPlayerIfPresent(UUID playerId, String key, Object... args) {
		MakerPlayer mPlayer = getPlayer(playerId);
		if (mPlayer != null) {
			mPlayer.sendActionMessage(key, args);
		}
	}

	@Deprecated // seriously?
	private void sendBruteForceWelcomeMessage(final MakerPlayer mPlayer) {
		if (mPlayer.getPlayer().isOnline()) {
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
			if (mPlayer.getData().isAllowedInFullServer()) {
				mPlayer.sendMessage("server.full.player.allowed");
			}
		}
	}

	public void sendMessageToPlayerIfPresent(UUID playerId, String key, Object... args) {
		MakerPlayer mPlayer = getPlayer(playerId);
		if (mPlayer != null) {
			mPlayer.sendMessage(key, args);
		}
	}

	public void startSpectating(MakerPlayer mPlayer) {
		mPlayer.sendMessage("player.spectate.menu");
		mPlayer.sendMessage("player.spectate.exit.command");
		mPlayer.spectate();
	}

	public void startSteveChallenge(MakerPlayer mPlayer) {
		if (!mPlayer.isInLobby()) {
			mPlayer.sendActionMessage("level.play.error.player-busy");
			return;
		}
		MakerPlayableLevel level = getEmptyLevelIfAvailable();
		if (level == null) {
			mPlayer.sendActionMessage("level.error.full");
			return;
		}
		level.setCurrentPlayerId(mPlayer.getUniqueId());
		MakerSteveData steveData = new MakerSteveData();
		level.setSteveData(steveData);
		mPlayer.setSteveData(steveData);
		//level.setLevelSerial(steveData.getRandomLevel());
		level.waitForBusyLevel(mPlayer, false, true, false);
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
				if (!mPlayer.isDisabled()) {
					// outside tick while we found a way to integrate it in player tick
					updatePlayerScoreboardTeamEntries(mPlayer);
				}
			}
		}
		entriesToAddToScoreboardTeams.clear();
		entriesToRemoveFromScoreboardTeams.clear();
	}

	public void unlock(MakerPlayer mPlayer, MakerUnlockable unlockable) {
		checkNotNull(mPlayer);
		checkNotNull(unlockable);
		if (mPlayer.hasUnlockable(unlockable)) {
			mPlayer.sendMessage("command.unlock.already-unlocked");
			return;
		}
		plugin.getDatabaseAdapter().unlockAsync(mPlayer, unlockable);
	}

	public void unlockCallback(UUID playerId, MakerUnlockable unlockable, UnlockOperationResult finalResult, Long playerCoinBalance) {
		verifyPrimaryThread();
		checkNotNull(playerId);
		checkNotNull(unlockable);
		switch (finalResult) {
		case SUCCESS:
			MakerPlayer mPlayer = getPlayer(playerId);
			if (mPlayer == null) {
				break;
			}
			mPlayer.getData().addUnlockable(unlockable);
			mPlayer.sendMessage("command.unlock.success", unlockable.name().toLowerCase());
			//if (!mPlayer.hasRank(Rank.ADMIN)) {
			mPlayer.sendMessage("coin.transaction.unlock.player", unlockable.getCost(), unlockable.name().toLowerCase());
			//}
			if (playerCoinBalance != null) {
				mPlayer.setCoins(playerCoinBalance);
				mPlayer.sendMessage("coin.transaction.new-balance", playerCoinBalance); 
			}
			break;
		case ALREADY_UNLOCKED:
			sendMessageToPlayerIfPresent(playerId, "command.unlock.already-unlocked");
			break;
		case INSUFFICIENT_COINS:
			sendMessageToPlayerIfPresent(playerId, "coin.transaction.error.insufficient-coins");
			break;
		case ERROR:
			sendMessageToPlayerIfPresent(playerId, "server.error.internal");
		default:
			break;
		}
	}

	public void unmuteOthers(UUID playerId) {
		mutedOtherPlayers.remove(playerId);
	}

	public void unmutePlayer(UUID playerId) {
		mutedPlayers.remove(playerId);
	}

	public void unpublishLevel(MakerPlayer mPlayer, long serial) {
		long confirmSerial = mPlayer.getLevelToUnpublishSerial();
		if (confirmSerial != serial) {
			mPlayer.setLevelToUnpublishSerial(serial);
			mPlayer.sendMessage("command.level.unpublish.confirm1", serial);
			if (!mPlayer.hasRank(Rank.ADMIN)) {
				mPlayer.sendMessage("command.level.unpublish.confirm2", serial);
			}
			mPlayer.sendMessage("command.level.unpublish.confirm3", serial);
		} else {
			plugin.getDatabaseAdapter().unpublishLevelBySerialAsync(serial, mPlayer);
		}
	}

	public void unpublishLevelBySerialCallback(long levelSerial, UUID playerId, LevelOperationResult result, Long playerCoinBalance, Integer totalPublishedLevelsCount) {
		verifyPrimaryThread();
		switch (result) {
		case SUCCESS:
			MakerPlayer mPlayer = getPlayer(playerId);
			if (mPlayer == null) {
				break;
			}
			mPlayer.sendMessage("command.level.unpublish.success", levelSerial);
			if (!mPlayer.hasRank(Rank.ADMIN)) {
				sendMessageToPlayerIfPresent(playerId, "coin.transaction.level-unpublish.player", 500);
			}
			if (playerCoinBalance != null) {
				mPlayer.setCoins(playerCoinBalance);
				mPlayer.sendMessage("coin.transaction.new-balance", playerCoinBalance); 
			}
			plugin.getDatabaseAdapter().loadPlayerLevelsCountAsync(playerId);
			break;
		case NOT_FOUND:
			sendMessageToPlayerIfPresent(playerId, "command.level.error.not-found", levelSerial);
			break;
		case PERMISSION_DENIED:
			sendMessageToPlayerIfPresent(playerId, "command.level.unpublish.denied", levelSerial);
			break;
		case NOT_PUBLISHED:
			sendMessageToPlayerIfPresent(playerId, "command.level.unpublish.not-published", levelSerial);
			break;
		case ALREADY_UNPUBLISHED:
			sendMessageToPlayerIfPresent(playerId, "command.level.unpublish.already-unpublished", levelSerial);
			break;
		case INSUFFICIENT_COINS:
			sendMessageToPlayerIfPresent(playerId, "coin.transaction.error.insufficient-coins");
			break;
		default:
			sendMessageToPlayerIfPresent(playerId, "server.error.internal");
			break;
		}
		if (totalPublishedLevelsCount != null) {
			LevelBrowserMenu.updateLevelCount(totalPublishedLevelsCount);
		}
	}

	private void updatePlayerCoinBalanceIfPresent(UUID playerId, long balance) {
		MakerPlayer mPlayer = getPlayer(playerId);
		if (mPlayer != null) {
			mPlayer.setCoins(balance);
		}
	}

	private void updatePlayerScoreboardTeamEntries(MakerPlayer mPlayer) {
		for (UUID otherId : entriesToAddToScoreboardTeams) {
			MakerPlayer otherPlayer = getPlayer(otherId);
			if (otherPlayer == null) {
				continue;
			}
			mPlayer.updateScoreboardPlayerEntry(otherPlayer.getDisplayRank(), otherPlayer.getName());
			otherPlayer.updateScoreboardPlayerEntry(mPlayer.getDisplayRank(), mPlayer.getName());
		}
		for (String playerName : entriesToRemoveFromScoreboardTeams) {
			mPlayer.removeTeamEntryFromScoreboard(playerName);
		}
	}

}
